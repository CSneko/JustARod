package org.cneko.justarod.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import org.cneko.justarod.entity.Pregnant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 熬夜的手抖：疲劳越高，弹射物（弓、弩、三叉戟、雪球、末影珍珠等）散布越大。
 * 拦截点为 Projectile#shootFromRotation 内部对 shoot(DDDFF) 的调用，
 * 通过 ThreadLocal 在两个注入器之间传递本次的额外散布值。
 */
@Mixin(Projectile.class)
public abstract class ProjectileMixin {

    @Unique
    private static final ThreadLocal<Float> justarod$extraSpread = ThreadLocal.withInitial(() -> 0.0f);

    @Inject(method = "shootFromRotation", at = @At("HEAD"))
    private void justarod$captureFatigueSpread(Entity shooter, float x, float y, float z, float velocity, float inaccuracy, CallbackInfo ci) {
        float extra = 0.0f;
        // 原本就零散布的完美瞄准不受影响
        if (inaccuracy > 0 && shooter instanceof Player player && player instanceof Pregnant pregnant) {
            extra = Pregnant.rangedSpreadBonus(Pregnant.fatigueStage(pregnant.getFatigue()));
        }
        justarod$extraSpread.set(extra);
    }

    @ModifyArg(
            method = "shootFromRotation",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/projectile/Projectile;shoot(DDDFF)V"
            ),
            index = 4
    )
    private float justarod$applyFatigueSpread(float originalInaccuracy) {
        return originalInaccuracy + justarod$extraSpread.get();
    }
}
