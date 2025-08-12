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
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.cneko.justarod.entity.BDSMable;
import org.cneko.justarod.item.JRItems;
import org.joml.Vector3f;

public class ElectricShockFeatureRenderer extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

    public ElectricShockFeatureRenderer(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                       AbstractClientPlayerEntity player, float limbAngle, float limbDistance,
                       float tickDelta, float animationProgress, float headYaw, float headPitch) {

        if (!(player instanceof BDSMable bm) || bm.getElectricShock() <= 0) return;

        matrices.push();

        // 绑定到右腿
        getContextModel().rightLeg.rotate(matrices);

        // 平移：x 左右，y 上下，z 前后（相对于腿的原点）
        matrices.translate(-0.1F, 0.2F, 0.0F);
        // 旋转：绕 Y 轴旋转 90 度
        matrices.multiply(RotationAxis.POSITIVE_Y.rotation((float) Math.toRadians(90)));

        // 缩放
        matrices.scale(0.5F, 0.5F, 0.5F);

        MinecraftClient.getInstance().getItemRenderer().renderItem(
                JRItems.Companion.getELECTRIC_SHOCK_DEVICE().getDefaultStack(),
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
