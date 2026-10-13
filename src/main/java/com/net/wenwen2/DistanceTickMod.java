package com.net.wenwen2;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraft.world.entity.MobSpawnType;
@Mod("wenwen2")
public final class DistanceTickMod {
    public DistanceTickMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onConfig);
        MinecraftForge.EVENT_BUS.addListener(this::onTagsUpdated);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopped);
        MinecraftForge.EVENT_BUS.addListener(this::onNaturalSpawn);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, TickConfig.SPEC, "wenwen2-common.toml");
    }
    private void onConfig(ModConfigEvent event) {
        if (!(event instanceof ModConfigEvent.Unloading) && event.getConfig().getSpec() == TickConfig.SPEC)
            TickPolicy.reloadConfig();
    }
    private void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.shouldUpdateStaticData()) TickPolicy.invalidateTypes();
    }
    private void onServerStopped(ServerStoppedEvent event) { TickPolicy.clearFrozenEntities(); }
    private void onNaturalSpawn(MobSpawnEvent.PositionCheck event) {
        if (event.getSpawnType() == MobSpawnType.NATURAL
            && TickPolicy.shouldBlockNaturalSpawn(event.getLevel().getLevel(), event.getEntity()))
            event.setResult(Event.Result.DENY);
    }
}
