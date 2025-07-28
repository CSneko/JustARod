package org.cneko.justarod.mixin.client;

import net.minecraft.client.model.*;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerRendererMixin {

    // 1. 定义一个静态的、最终的ModelPart来表示肚子。它只会被初始化一次。
    private static final ModelPart BELLY_MODEL_PART;

    // 2. 使用静态代码块来创建和配置模型。
    static {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        // 玩家身体的尺寸为 8x12x4 (宽x高x深)，UV纹理的起始点在(16, 16)。
        // 我们的肚子模型将使用身体正面的纹理(UV从20,20开始)，并且尺寸稍作调整以更好地贴合。
        // 为了避免在边缘处与盔甲或身体发生Z冲突（闪烁），宽度设为7.0F（而不是8.0F）。
        // Z轴深度设为5.0F，比身体的4.0F略厚，这样即使不缩放也有一点点凸出效果。
        root.addChild("belly",
                ModelPartBuilder.create().uv(20, 20)
                        .cuboid(-3.5F, 2.0F, -2.5F, 7.0F, 8.0F, 5.0F),
                ModelTransform.NONE);

        // 从模型数据中创建最终的ModelPart。玩家皮肤纹理尺寸为64x64。
        BELLY_MODEL_PART = TexturedModelData.of(modelData, 64, 64).createModel().getChild("belly");
    }


    @Inject(method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"))
    private void render(AbstractClientPlayerEntity player, float f, float g, MatrixStack matrices, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        // 如果该值小于等于0，则代表没有怀孕。
        int pregnant = player.getPregnant();
        if (pregnant <= 0) {
            return;
        }

        // 根据你的公式计算怀孕进度 (0.0 -> 1.0)
        float pregnantProgress = (20*60*20*10f - pregnant) / (20*60*20*10f);

        // 如果进度太小，就不渲染，以避免不必要的性能开销
        if (pregnantProgress <= 0.05f) {
            return;
        }

        // 保存当前的矩阵状态，以便在渲染结束后恢复
        matrices.push();

        // 获取当前正在渲染的玩家模型
        PlayerEntityModel<AbstractClientPlayerEntity> playerModel = ((PlayerEntityRenderer)(Object)this).getModel();
        // 获取身体部分
        ModelPart body = playerModel.body;

        // 核心步骤：将身体的变换（旋转、位移）应用到矩阵上。
        // 这样，我们接下来渲染的肚子就会完全跟随身体的动画。
        body.rotate(matrices);

        // 根据怀孕进度计算缩放比例
        // XY轴的缩放较小，让肚子看起来更宽、更饱满
        float scaleXY = 1.0f + pregnantProgress * 0.4f; // 最大增长40%的宽高
        // Z轴的缩放较大，实现向前凸出的主要效果
        float scaleZ = 1.0f + pregnantProgress * 1.5f; // 最大增长150%的深度

        matrices.scale(scaleXY, scaleXY, scaleZ);

        // 获取玩家的皮肤纹理
        Identifier skinTexture = player.getSkinTextures().texture();
        // 获取用于渲染实体模型的VertexConsumer
        VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(RenderLayer.getEntitySolid(skinTexture));

        // 渲染肚子模型。使用与玩家相同的光照(i)和覆盖层(OverlayTexture.DEFAULT_UV)。
        BELLY_MODEL_PART.render(matrices, vertexConsumer, i, OverlayTexture.DEFAULT_UV);

        // 恢复之前保存的矩阵状态，防止影响后续渲染（如手臂、头部等）
        matrices.pop();
    }
}