package org.cneko.justarod.client.feature;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.item.ItemStack;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3i;
import org.cneko.justarod.item.JRItems;
import org.joml.Vector3f;

public class HandcuffFeatureRenderer extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
    protected HeldItemRenderer heldItemRenderer;
    public HandcuffFeatureRenderer(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context, HeldItemRenderer heldItemRenderer) {
        super(context);
        this.heldItemRenderer = heldItemRenderer;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                       AbstractClientPlayerEntity player, float limbAngle, float limbDistance,
                       float tickDelta, float animationProgress, float headYaw, float headPitch) {

        // 判断玩家是否需要显示
        if (player.getHandcuffed() <= 0) return;

        ItemStack ring = new ItemStack(JRItems.Companion.getHANDCUFFES_RING());

        // 左手
        matrices.push();
        getContextModel().leftArm.rotate(matrices);
        matrices.translate(0.05, 0.6, 0.1);
        matrices.scale(0.8f, 0.8f, 0.8f);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f));
        heldItemRenderer.renderItem(player, ring, ModelTransformationMode.THIRD_PERSON_LEFT_HAND, true, matrices, vertexConsumers, light);
        matrices.pop();


        matrices.push();
        // 右手
        getContextModel().rightArm.rotate(matrices);
        matrices.translate(0.05, 0.6, 0.1);
        matrices.scale(0.8f, 0.8f, 0.8f);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f));
        heldItemRenderer.renderItem(player, ring, ModelTransformationMode.THIRD_PERSON_RIGHT_HAND, false, matrices, vertexConsumers, light);
        matrices.pop();
    }
}
