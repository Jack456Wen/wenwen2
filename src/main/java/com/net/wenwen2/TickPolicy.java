package com.net.wenwen2;

import com.net.wenwen2.mixin.EntityPortalAccessor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

public final class TickPolicy {
    private record Settings(boolean enabled, boolean protectDying, boolean mobsOnly, boolean protectBosses,
                            boolean protectNamed, boolean protectTamed, boolean freezeWithoutPlayers,
                            boolean cylinder, double radiusSquared, double vertical,
                            boolean delayChecks, int interval) {}
    private record Whitelist(Set<ResourceLocation> ids, Set<String> namespaces, List<TagKey<EntityType<?>>> tags) {}
    private static final class Snapshot {
        final Settings settings;
        final Whitelist whitelist;
        final Map<EntityType<?>, Boolean> types = new ConcurrentHashMap<>();
        Snapshot(Settings settings, Whitelist whitelist) {
            this.settings = settings;
            this.whitelist = whitelist;
        }
        boolean whitelisted(EntityType<?> type) { return types.computeIfAbsent(type, this::matches); }
        private boolean matches(EntityType<?> type) {
            ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
            if (id != null && (whitelist.ids().contains(id) || whitelist.namespaces().contains(id.getNamespace()))) return true;
            for (TagKey<EntityType<?>> tag : whitelist.tags()) if (type.is(tag)) return true;
            return false;
        }
    }
    // Atomically publish settings and a fresh type cache on configuration/tag reload.
    private static volatile Snapshot snapshot;
    // Server thread only. Values never reference their entity or world, allowing unloaded entities to be collected.
    private record FrozenState(Snapshot snapshot, ResourceKey<Level> dimension, long checkedAt,
                               double x, double y, double z) {}
    private static final Map<Entity, FrozenState> frozen = new WeakHashMap<>();

    public static boolean shouldSuspend(ServerLevel level, Entity entity) {
        Snapshot current = snapshot;
        if (current == null) return false;
        Settings s = current.settings;
        if (!s.enabled() || protectedEntity(entity, s) || current.whitelisted(entity.getType())) {
            if (!frozen.isEmpty()) frozen.remove(entity);
            return false;
        }
        if (s.delayChecks()) {
            FrozenState state = frozen.get(entity);
            if (state != null && state.snapshot() == current && state.dimension() == level.dimension()
                && state.x() == entity.getX() && state.y() == entity.getY() && state.z() == entity.getZ()
                && FrozenCheckSchedule.canSkip(level.getGameTime(), state.checkedAt(), s.interval())) return true;
        }
        boolean hasPlayer = false;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isRemoved()) continue;
            hasPlayer = true;
            if (ActivationRange.contains(entity.getX() - player.getX(), entity.getY() - player.getY(),
                entity.getZ() - player.getZ(), s.cylinder(), s.radiusSquared(), s.vertical())) {
                if (!frozen.isEmpty()) frozen.remove(entity);
                return false;
            }
        }
        boolean suspend = hasPlayer || s.freezeWithoutPlayers();
        if (suspend && s.delayChecks()) {
            frozen.put(entity, new FrozenState(current, level.dimension(), level.getGameTime(),
                entity.getX(), entity.getY(), entity.getZ()));
        } else if (!frozen.isEmpty()) frozen.remove(entity);
        return suspend;
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
            TickConfig.DELAY_FROZEN_CHECKS.get(), TickConfig.FROZEN_CHECK_INTERVAL.get());
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
    public static void clearFrozenEntities() { frozen.clear(); }
    private TickPolicy() {}
}
