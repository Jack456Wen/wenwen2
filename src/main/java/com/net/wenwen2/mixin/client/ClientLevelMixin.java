package com.net.wenwen2.mixin.client;

import com.net.wenwen2.TickPolicy;
import com.net.wenwen2.client.ClientDistancePolicy;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    private void wenwen2$suspendDistantClientEntity(Entity entity, CallbackInfo ci) {
        if (TickPolicy.clientDistanceChecksEnabled() && ClientDistancePolicy.shouldSuspend(entity)
            && TickPolicy.freezeClientEntities()) {
            // Position packets can still move frozen entities; keep interpolation origins current.
            entity.setOldPosAndRot();
            ci.cancel();
        }
    }
}
