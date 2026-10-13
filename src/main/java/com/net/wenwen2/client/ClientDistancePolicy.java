package com.net.wenwen2.client;

import com.net.wenwen2.TickPolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public final class ClientDistancePolicy {
    public static boolean shouldSuspend(Entity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        // Keep the active camera visible, including camera entities used by other mods.
        return entity != minecraft.getCameraEntity() && TickPolicy.shouldSuspendClient(entity, minecraft.player);
    }
    private ClientDistancePolicy() {}
}
