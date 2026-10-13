package com.net.wenwen2;

import com.net.wenwen2.mixin.EntityPortalAccessor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

public final class TickPolicy {
    private record Settings(boolean enabled, boolean protectDying, boolean mobsOnly, boolean protectBosses,
                            boolean protectNamed, boolean protectTamed, boolean freezeWithoutPlayers,
                            boolean cylinder, double radiusSquared, double vertical,
                            boolean delayChecks, int interval, boolean hideDistant, boolean freezeClient,
                            boolean disablePush, boolean limitSpawns, boolean maintainDespawn) {}
    private record Whitelist(Set<ResourceLocation> ids, Set<String> namespaces, List<TagKey<EntityType<?>>> tags) {}
    private static final class Snapshot {
        final Settings settings;
        final Whitelist whitelist;
        final Map<EntityType<?>, Boolean> types = new ConcurrentHashMap<>();
        Snapshot(Settings settings, Whitelist whitelist) {
            this.settings = settings;
            this.whitelist = whitelist;
        }
        boolean whitelisted(EntityType<?> type) {
            Boolean cached = types.get(type);
            if (cached != null) return cached;
            boolean result = matches(type);
            types.put(type, result);
            return result;
        }
        private boolean matches(EntityType<?> type) {
            ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
            if (id != null && (whitelist.ids().contains(id) || whitelist.namespaces().contains(id.getNamespace()))) return true;
            for (TagKey<EntityType<?>> tag : whitelist.tags()) if (type.is(tag)) return true;
            return false;
        }
    }
    // Atomically publish settings and a fresh type cache on configuration/tag reload.
    private static volatile Snapshot snapshot;
    private static final class PlayerView {
        final PlayerRangeIndex index = new PlayerRangeIndex();
        Snapshot snapshot;
        long gameTime;
    }
    // Values contain positions only, never a reference back to the weak level key.
    private static final Map<ServerLevel, PlayerView> playerViews = new WeakHashMap<>();

    private static PlayerRangeIndex players(ServerLevel level, Snapshot current) {
        PlayerView view = playerViews.get(level);
        if (view == null) {
            view = new PlayerView();
            playerViews.put(level, view);
        }
        long now = level.getGameTime();
        if (view.snapshot != current || view.gameTime != now) {
            Settings s = current.settings;
            view.index.reset(s.radiusSquared(), s.vertical(), s.cylinder());
            for (ServerPlayer player : level.players()) {
                if (!player.isSpectator() && !player.isRemoved())
                    view.index.add(player.getX(), player.getY(), player.getZ());
            }
            view.index.build();
            view.snapshot = current;
            view.gameTime = now;
        }
        return view.index;
    }

    public static boolean shouldSuspend(ServerLevel level, Entity entity) {
        Snapshot current = snapshot;
        if (current == null) return false;
        Settings s = current.settings;
        EntityDistanceState state = ((EntityDistanceAccess) entity).wenwen2$getDistanceState();
        if (!s.enabled() || protectedEntity(entity, s) || current.whitelisted(entity.getType())) {
            if (state != null) state.suspended = false;
            return false;
        }
        if (s.delayChecks() && state != null
            && state.matches(current, level.dimension(), entity.getX(), entity.getY(), entity.getZ())
            && state.canSkip(level.getGameTime())) {
            maintainDespawn(entity, s);
            return true;
        }
        PlayerRangeIndex players = players(level, current);
        boolean suspend = !players.contains(entity.getX(), entity.getY(), entity.getZ())
            && (players.hasPlayers() || s.freezeWithoutPlayers());
        if (suspend && (s.delayChecks() || s.disablePush())) {
            if (state == null) state = ((EntityDistanceAccess) entity).wenwen2$getOrCreateDistanceState();
            long now = level.getGameTime();
            state.update(current, level.dimension(), now,
                FrozenCheckSchedule.nextCheck(now, entity.getId(), s.delayChecks() ? s.interval() : 1),
                entity.getX(), entity.getY(), entity.getZ(), true);
        } else if (state != null) {
            state.suspended = false;
        }
        if (suspend) maintainDespawn(entity, s);
        return suspend;
    }

    private static void maintainDespawn(Entity entity, Settings s) {
        // ServerLevel has already called checkDespawn, including Forge's allow/deny event.
        // Only the idle timer normally advanced by serverAiStep needs to continue while frozen.
        if (s.maintainDespawn() && entity instanceof Mob mob && !mob.isNoAi()
            && !mob.isPersistenceRequired() && !mob.requiresCustomPersistence()) {
            mob.setNoActionTime(FrozenCheckSchedule.advanceIdleTime(mob.getNoActionTime()));
        }
    }

    public static boolean hideDistantEntities() {
        Snapshot current = snapshot;
        return current != null && current.settings.enabled() && current.settings.hideDistant();
    }

    public static boolean freezeClientEntities() {
        Snapshot current = snapshot;
        return current != null && current.settings.enabled() && current.settings.freezeClient();
    }

    public static boolean clientDistanceChecksEnabled() {
        Snapshot current = snapshot;
        return current != null && current.settings.enabled()
            && (current.settings.hideDistant() || current.settings.freezeClient() || current.settings.disablePush());
    }

    /** Client-only callers supply their local player; this class never loads Minecraft client classes. */
    public static boolean shouldSuspendClient(Entity entity, Player viewer) {
        Snapshot current = snapshot;
        if (current == null || viewer == null || viewer.level() != entity.level()) return false;
        Settings s = current.settings;
        if (!s.enabled() || viewer.isSpectator()) return false;
        EntityDistanceState state = ((EntityDistanceAccess) entity).wenwen2$getOrCreateDistanceState();
        long now = entity.level().getGameTime();
        if (state.matches(current, entity.level().dimension(), entity.getX(), entity.getY(), entity.getZ())
            && state.checkedAt == now && state.viewerId == viewer.getId()
            && state.viewerX == viewer.getX() && state.viewerY == viewer.getY() && state.viewerZ == viewer.getZ())
            return state.suspended;
        boolean suspend = !protectedEntity(entity, s) && !current.whitelisted(entity.getType())
            && !ActivationRange.contains(entity.getX() - viewer.getX(), entity.getY() - viewer.getY(),
                entity.getZ() - viewer.getZ(), s.cylinder(), s.radiusSquared(), s.vertical());
        state.update(current, entity.level().dimension(), now, now + 1,
            entity.getX(), entity.getY(), entity.getZ(), suspend);
        state.viewerId = viewer.getId();
        state.viewerX = viewer.getX();
        state.viewerY = viewer.getY();
        state.viewerZ = viewer.getZ();
        return suspend;
    }

    public static boolean shouldDisablePushing(Entity entity) {
        Snapshot current = snapshot;
        if (current == null || !current.settings.enabled() || !current.settings.disablePush()) return false;
        EntityDistanceState state = ((EntityDistanceAccess) entity).wenwen2$getDistanceState();
        if (state == null || !state.suspended
            || !state.matches(current, entity.level().dimension(), entity.getX(), entity.getY(), entity.getZ())) return false;
        long now = entity.level().getGameTime();
        // Client cache is refreshed each tick; server records may wait up to the configured interval.
        if (now < state.checkedAt || now - state.checkedAt > (entity.level().isClientSide ? 1 : current.settings.interval()))
            return false;
        return !protectedEntity(entity, current.settings) && !current.whitelisted(entity.getType());
    }

    public static boolean shouldBlockNaturalSpawn(ServerLevel level, Entity entity) {
        Snapshot current = snapshot;
        if (current == null || !current.settings.enabled() || !current.settings.limitSpawns()
            || protectedEntity(entity, current.settings) || current.whitelisted(entity.getType())) return false;
        PlayerRangeIndex players = players(level, current);
        return !players.contains(entity.getX(), entity.getY(), entity.getZ());
    }

    private static boolean protectedEntity(Entity entity, Settings s) {
        if (entity instanceof Player || entity.isRemoved()) return true;
        if (s.protectDying() && entity instanceof LivingEntity living && living.isDeadOrDying()) return true;
        if (entity.isPassenger() || entity.isVehicle() || entity.isOnPortalCooldown()
            || ((EntityPortalAccessor) entity).wenwen2$isInsidePortal()) return true;
        if (s.mobsOnly() && !(entity instanceof Mob)) return true;
        if (s.protectBosses() && !entity.canChangeDimensions()) return true;
        if (s.protectNamed() && entity.hasCustomName()) return true;
        if (s.protectTamed() && ((entity instanceof TamableAnimal a && a.isTame())
            || (entity instanceof AbstractHorse h && h.isTamed()))) return true;
        return entity instanceof Mob m && m.isLeashed();
    }

    public static synchronized void reloadConfig() {
        boolean cylinder = TickConfig.DISTANCE_MODE.get() == TickConfig.DistanceMode.CYLINDER;
        double radius = cylinder ? TickConfig.HORIZONTAL_DISTANCE.get() : TickConfig.DISTANCE.get();
        Settings settings = new Settings(TickConfig.ENABLED.get(), TickConfig.PROTECT_DYING.get(),
            TickConfig.SCOPE.get() == TickConfig.Scope.MOBS_ONLY, TickConfig.PROTECT_BOSSES.get(),
            TickConfig.PROTECT_NAMED.get(), TickConfig.PROTECT_TAMED.get(), TickConfig.FREEZE_WITHOUT_PLAYERS.get(),
            cylinder, radius * radius, TickConfig.VERTICAL_DISTANCE.get(),
            TickConfig.DELAY_FROZEN_CHECKS.get(), TickConfig.FROZEN_CHECK_INTERVAL.get(),
            TickConfig.HIDE_DISTANT_ENTITIES.get(), TickConfig.FREEZE_CLIENT_ENTITIES.get(),
            TickConfig.DISABLE_FROZEN_PUSHING.get(), TickConfig.LIMIT_NATURAL_SPAWNS.get(),
            TickConfig.MAINTAIN_NATURAL_DESPAWN.get());
        Set<ResourceLocation> ids = new HashSet<>();
        Set<String> namespaces = new HashSet<>();
        List<TagKey<EntityType<?>>> tags = new ArrayList<>();
        for (String entry : TickConfig.WHITELIST.get()) {
            if (entry.startsWith("#")) tags.add(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(entry.substring(1))));
            else if (entry.endsWith(":*")) namespaces.add(entry.substring(0, entry.length() - 2));
            else ids.add(new ResourceLocation(entry));
        }
        snapshot = new Snapshot(settings, new Whitelist(Set.copyOf(ids), Set.copyOf(namespaces), List.copyOf(tags)));
    }

    public static synchronized void invalidateTypes() {
        Snapshot old = snapshot;
        if (old != null) snapshot = new Snapshot(old.settings, old.whitelist);
    }
    public static void clearFrozenEntities() {
        playerViews.clear();
        invalidateTypes();
    }
    private TickPolicy() {}
}
