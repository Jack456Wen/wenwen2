package com.net.wenwen2.mixin.client;

import com.net.wenwen2.TickPolicy;
import com.net.wenwen2.client.ClientDistancePolicy;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void wenwen2$hideDistant(Entity entity, Frustum frustum, double x, double y, double z,
                                    CallbackInfoReturnable<Boolean> cir) {
        if (TickPolicy.hideDistantEntities() && ClientDistancePolicy.shouldSuspend(entity)) cir.setReturnValue(false);
    }

    // Also covers callers that bypass the normal frustum test.
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void wenwen2$skipDistantRender(Entity entity, double x, double y, double z, float yaw, float partialTick,
                                          PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (TickPolicy.hideDistantEntities() && ClientDistancePolicy.shouldSuspend(entity)) ci.cancel();
    }
}
