package org.cneko.justarod.mixin.client;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EnderDragonEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.util.math.MathHelper;
import org.cneko.justarod.entity.Pregnant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderDragonEntityRenderer.class)
public class EnderDragonRendererMixin {
    @Inject(
            method = "render(Lnet/minecraft/entity/boss/dragon/EnderDragonEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/util/math/MatrixStack;push()V",
                    shift = At.Shift.AFTER
            )
    )
    private void scaleBabyDragon(
            EnderDragonEntity enderDragonEntity,
            float f,
            float g,
            MatrixStack matrixStack,
            VertexConsumerProvider vertexConsumerProvider,
            int i,
            CallbackInfo ci
    ) {
        // 检查是否为幼年
        if (Pregnant.FOREVER_BABY.contains(enderDragonEntity.getUuid())) {
            float scale = 0.1F; // 目标缩放比例
            matrixStack.scale(scale, scale, scale);

            // 调整位置以保持脚部贴地
            float heightOffset = (float) MathHelper.lerp(g, enderDragonEntity.prevY, enderDragonEntity.getY());
            matrixStack.translate(0, (1.0F - scale) * heightOffset, 0);
        }
    }
}
