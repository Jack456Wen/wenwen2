package com.net.wenwen2.mixin;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(Entity.class)
public interface EntityPortalAccessor {
    @Accessor("isInsidePortal")
    boolean wenwen2$isInsidePortal();
}
