package org.cneko.justarod.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.cneko.justarod.entity.Pregnant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderDragonRenderer.class)
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
            EnderDragon enderDragonEntity,
            float f,
            float g,
            PoseStack matrixStack,
            MultiBufferSource vertexConsumerProvider,
            int i,
            CallbackInfo ci
    ) {
        // 检查是否为幼年
        if (Pregnant.FOREVER_BABY.contains(enderDragonEntity.getUUID())) {
            float scale = 0.1F; // 目标缩放比例
            matrixStack.scale(scale, scale, scale);

            // 调整位置以保持脚部贴地
            float heightOffset = (float) Mth.lerp(g, enderDragonEntity.yo, enderDragonEntity.getY());
            matrixStack.translate(0, (1.0F - scale) * heightOffset, 0);
        }
    }
}
