package org.cneko.justarod.client.feature;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import org.cneko.justarod.entity.BDSMable;
import org.cneko.justarod.item.JRItems;

public class EarplugFeatureRenderer extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {


    public EarplugFeatureRenderer(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                       AbstractClientPlayerEntity player, float limbAngle, float limbDistance,
                       float tickDelta, float animationProgress, float headYaw, float headPitch) {

        if (!(player instanceof BDSMable bm) || bm.getEarplug() <= 0) return;

        matrices.push();

        // 跟随头部旋转
        getContextModel().head.rotate(matrices);

        // 平移：x 左右，y 上下，z 前后（单位是方块的 1/16）
        matrices.translate(0.0F, -0.3f, 0f);
        // 缩放：稍微小一点
        matrices.scale(0.7F, 0.7F, 0.7F);

        MinecraftClient.getInstance().getItemRenderer().renderItem(
                JRItems.Companion.getEARPLUG().getDefaultStack(),
                ModelTransformationMode.FIXED,
                light,
                OverlayTexture.DEFAULT_UV,
                matrices,
                vertexConsumers,
                player.getWorld(),
                0
        );

        matrices.pop();
    }

}