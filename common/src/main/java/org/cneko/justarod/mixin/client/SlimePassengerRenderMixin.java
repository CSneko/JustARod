package org.cneko.justarod.mixin.client;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.cneko.justarod.entity.slime.TamedSlimeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 被吞进史莱姆体内的实体不再走原版渲染。
 *
 * <p>原因不是「位置不对」，而是**画在哪个阶段都不对**：原版实体在实体主渲染阶段提交，
 * 而史莱姆的外壳 + 内核都是半透明、且在这个阶段之后叠加，
 * 于是体内的东西被 0.32 × 0.45 两层色盖住（只剩 ~18% 透出来，肉眼等于看不见，只看得见一层壳）。
 *
 * <p>体内内容统一由 {@code SlimeContentsLayer} 在图层阶段绘制（模型提交之后 → 真的看得见）。
 * 两处都画就会出现一大一小两个分身，所以这里把原版那次渲染关掉。
 *
 * <p>{@code LevelRenderer} 通过 {@link EntityRenderDispatcher#shouldRender} 决定要不要
 * 提取/提交一个实体，因此这是唯一需要拦截的入口。
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class SlimePassengerRenderMixin {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void justarod$hideContainedPassenger(Entity entity, Frustum frustum,
                                                 double camX, double camY, double camZ,
                                                 CallbackInfoReturnable<Boolean> cir) {
        if (entity.getVehicle() instanceof TamedSlimeEntity) {
            cir.setReturnValue(false);
        }
    }
}
