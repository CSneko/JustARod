package org.cneko.justarod.client.feature;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.cneko.justarod.item.JRItems;

public class ShacklesFeatureRenderer extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
    protected HeldItemRenderer heldItemRenderer;

    public ShacklesFeatureRenderer(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context,
                                   HeldItemRenderer heldItemRenderer) {
        super(context);
        this.heldItemRenderer = heldItemRenderer;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                       AbstractClientPlayerEntity player, float limbAngle, float limbDistance,
                       float tickDelta, float animationProgress, float headYaw, float headPitch) {

        // 判断玩家是否被脚镣束缚
        if (player.getShackled() <= 0) return;

        ItemStack ring = new ItemStack(JRItems.Companion.getHANDCUFFES_RING());
        ItemStack chain = new ItemStack(JRItems.Companion.getHANDCUFFES_CHAIN());

        // 左脚
        matrices.push();
        getContextModel().leftLeg.rotate(matrices);

        matrices.translate(0.03, 0.7, 0.1);
        matrices.scale(0.8f, 0.8f, 0.8f);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f));
        heldItemRenderer.renderItem(player, ring, ModelTransformationMode.THIRD_PERSON_LEFT_HAND, true, matrices, vertexConsumers, light);
        matrices.pop();


        matrices.push();
        getContextModel().rightLeg.rotate(matrices);

        matrices.translate(-0.03, 0.7, 0.1);
        matrices.scale(0.8f, 0.8f, 0.8f);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f));
        heldItemRenderer.renderItem(player, ring, ModelTransformationMode.THIRD_PERSON_RIGHT_HAND, false, matrices, vertexConsumers, light);
        matrices.pop();


        matrices.push();
        Vec3d leftLegPos = new Vec3d(getContextModel().leftLeg.pivotX, getContextModel().leftLeg.pivotY, getContextModel().leftLeg.pivotZ);
        Vec3d rightLegPos = new Vec3d(getContextModel().rightLeg.pivotX, getContextModel().rightLeg.pivotY, getContextModel().rightLeg.pivotZ);


        double midX = (leftLegPos.getX() + rightLegPos.getX()) / 2.0;
        double midY = (leftLegPos.getY() + rightLegPos.getY()) / 2.0 - 0.2;
        double midZ = (leftLegPos.getZ() + rightLegPos.getZ()) / 2.0;

        matrices.translate(midX, midY, midZ);
        matrices.scale(0.7f, 0.7f, 0.7f);
        heldItemRenderer.renderItem(player, chain, ModelTransformationMode.THIRD_PERSON_RIGHT_HAND, false, matrices, vertexConsumers, light);
        matrices.pop();


    }
}
