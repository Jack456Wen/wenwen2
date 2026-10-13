package com.net.wenwen2.mixin;

import com.net.wenwen2.EntityDistanceAccess;
import com.net.wenwen2.EntityDistanceState;
import com.net.wenwen2.TickPolicy;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityDistanceMixin implements EntityDistanceAccess {
    @Unique private EntityDistanceState wenwen2$distanceState;

    @Override public EntityDistanceState wenwen2$getDistanceState() { return wenwen2$distanceState; }

    @Override public EntityDistanceState wenwen2$getOrCreateDistanceState() {
        if (wenwen2$distanceState == null) wenwen2$distanceState = new EntityDistanceState();
        return wenwen2$distanceState;
    }

    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void wenwen2$skipFrozenPush(Entity other, CallbackInfo ci) {
        if (TickPolicy.shouldDisablePushing((Entity) (Object) this) || TickPolicy.shouldDisablePushing(other)) ci.cancel();
    }
}
