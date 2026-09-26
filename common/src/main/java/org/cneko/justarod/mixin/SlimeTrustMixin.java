package org.cneko.justarod.mixin;

import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 给野生史莱姆加「驯服信任值」。
 *
 * <p>26.x 移除了 {@code Entity#getPersistentData()}，但粘液球的喂食次数需要持久化，
 * 因此这里用 mixin 给 {@link Slime} 加一个字段，并挂到原版存档里。
 * 只对野生史莱姆有意义；{@code TamedSlimeEntity} 有自己的信任值字段。
 */
@Mixin(Slime.class)
public class SlimeTrustMixin {

    @Unique
    private int justarod$trust = 0;

    @Inject(method = "addAdditionalSaveData", at = @org.spongepowered.asm.mixin.injection.At("TAIL"))
    private void justarod$saveTrust(ValueOutput out, CallbackInfo ci) {
        if (justarod$trust > 0) {
            out.putInt("JustarodSlimeTrust", justarod$trust);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @org.spongepowered.asm.mixin.injection.At("TAIL"))
    private void justarod$loadTrust(ValueInput in, CallbackInfo ci) {
        this.justarod$trust = in.getIntOr("JustarodSlimeTrust", 0);
    }
}
