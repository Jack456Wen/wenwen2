package com.net.wenwen2.mixin;

import com.net.wenwen2.TickPolicy;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractMinecart.class)
public abstract class MinecartPushMixin {
    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void wenwen2$skipFrozenPush(Entity other, CallbackInfo ci) {
        if (TickPolicy.shouldDisablePushing((Entity) (Object) this) || TickPolicy.shouldDisablePushing(other)) ci.cancel();
    }
}
