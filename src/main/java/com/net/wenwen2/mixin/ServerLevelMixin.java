package com.net.wenwen2.mixin;
import com.net.wenwen2.TickPolicy;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    private void wenwen2$suspendDistantEntity(Entity entity, CallbackInfo ci) {
        if (TickPolicy.shouldSuspend((ServerLevel)(Object)this, entity)) ci.cancel();
    }
}
