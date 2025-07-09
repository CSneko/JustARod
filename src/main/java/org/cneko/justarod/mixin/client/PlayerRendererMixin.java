package org.cneko.justarod.mixin.client;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.network.AbstractClientPlayerEntity;
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerRendererMixin {

    @Inject(method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"))
    private void beforeRender(AbstractClientPlayerEntity player, float f, float g, MatrixStack matrices, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        float scale = player.getScale();
        if (scale < 1f) {
            matrices.push();
            // 根据体型动态调整头部和帽子的缩放
            PlayerEntityModel<?> model = ((PlayerEntityRenderer)(Object)this).getModel();
            float headScale = 1.0f + (1f - scale)*1.5f; // 体型每减小0.1，头部放大0.15
            model.head.xScale = model.head.yScale = model.head.zScale = headScale;
            model.hat.xScale = model.hat.yScale = model.hat.zScale = headScale;
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("TAIL"))
    private void afterRender(AbstractClientPlayerEntity player, float f, float g, MatrixStack matrices, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        float scale = player.getScale();
        if (scale < 1f) {
            // 恢复原始缩放
            PlayerEntityModel<?> model = ((PlayerEntityRenderer)(Object)this).getModel();
            model.head.xScale = model.head.yScale = model.head.zScale = 1.0f;
            model.hat.xScale = model.hat.yScale = model.hat.zScale = 1.0f;
            matrices.pop();
        }
    }
}