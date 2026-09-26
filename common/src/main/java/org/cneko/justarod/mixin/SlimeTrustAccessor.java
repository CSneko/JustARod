package org.cneko.justarod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 访问 {@link SlimeTrustMixin} 注入到原版史莱姆上的信任值字段。
 */
@Mixin(net.minecraft.world.entity.monster.Slime.class)
public interface SlimeTrustAccessor {

    @Accessor("justarod$trust")
    int justarod$getTrust();

    @Accessor("justarod$trust")
    void justarod$setTrust(int trust);
}
