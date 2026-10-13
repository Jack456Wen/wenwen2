package com.net.wenwen2.mixin.client;

import com.net.wenwen2.TickPolicy;
import com.net.wenwen2.client.ClientDistancePolicy;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Redirect(method = {"handleMoveEntity", "handleTeleportEntity"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;lerpTo(DDDFFIZ)V"))
    private void wenwen2$applyFrozenPosition(Entity entity, double x, double y, double z,
                                            float yaw, float pitch, int steps, boolean teleport) {
        boolean frozen = TickPolicy.freezeClientEntities() && ClientDistancePolicy.shouldSuspend(entity);
        entity.lerpTo(x, y, z, yaw, pitch, steps, teleport);
        if (frozen) {
            // Do not leave network positions pending in a lerp that requires the cancelled tick.
            entity.moveTo(x, y, z, yaw, pitch);
            entity.setOldPosAndRot();
        }
    }
}
