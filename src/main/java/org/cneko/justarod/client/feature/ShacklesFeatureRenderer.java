package org.cneko.justarod.client.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.cneko.justarod.item.JRItems;

public class ShacklesFeatureRenderer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    protected ItemInHandRenderer heldItemRenderer;

    public ShacklesFeatureRenderer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> context,
                                   ItemInHandRenderer heldItemRenderer) {
        super(context);
        this.heldItemRenderer = heldItemRenderer;
    }

    @Override
    public void render(PoseStack matrices, MultiBufferSource vertexConsumers, int light,
                       AbstractClientPlayer player, float limbAngle, float limbDistance,
                       float tickDelta, float animationProgress, float headYaw, float headPitch) {

        // 判断玩家是否被脚镣束缚
        if (player.getShackled() <= 0) return;

        ItemStack ring = new ItemStack(JRItems.Companion.getHANDCUFFES_RING());
        ItemStack chain = new ItemStack(JRItems.Companion.getHANDCUFFES_CHAIN());

        // 左脚
        matrices.pushPose();
        getParentModel().leftLeg.translateAndRotate(matrices);

        matrices.translate(0.03, 0.7, 0.1);
        matrices.scale(0.8f, 0.8f, 0.8f);
        matrices.mulPose(Axis.XP.rotationDegrees(90f));
        heldItemRenderer.renderItem(player, ring, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, true, matrices, vertexConsumers, light);
        matrices.popPose();


        matrices.pushPose();
        getParentModel().rightLeg.translateAndRotate(matrices);

        matrices.translate(-0.03, 0.7, 0.1);
        matrices.scale(0.8f, 0.8f, 0.8f);
        matrices.mulPose(Axis.XP.rotationDegrees(90f));
        heldItemRenderer.renderItem(player, ring, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, false, matrices, vertexConsumers, light);
        matrices.popPose();


        matrices.pushPose();
        Vec3 leftLegPos = new Vec3(getParentModel().leftLeg.x, getParentModel().leftLeg.y, getParentModel().leftLeg.z);
        Vec3 rightLegPos = new Vec3(getParentModel().rightLeg.x, getParentModel().rightLeg.y, getParentModel().rightLeg.z);


        double midX = (leftLegPos.x() + rightLegPos.x()) / 2.0;
        double midY = (leftLegPos.y() + rightLegPos.y()) / 2.0 - 0.2;
        double midZ = (leftLegPos.z() + rightLegPos.z()) / 2.0;

        matrices.translate(midX, midY, midZ);
        matrices.scale(0.7f, 0.7f, 0.7f);
        heldItemRenderer.renderItem(player, chain, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, false, matrices, vertexConsumers, light);
        matrices.popPose();


    }
}
