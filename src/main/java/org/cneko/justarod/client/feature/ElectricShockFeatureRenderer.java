package org.cneko.justarod.client.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import org.cneko.justarod.entity.BDSMable;
import org.cneko.justarod.item.JRItems;
import org.joml.Vector3f;

public class ElectricShockFeatureRenderer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public ElectricShockFeatureRenderer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> context) {
        super(context);
    }

    @Override
    public void render(PoseStack matrices, MultiBufferSource vertexConsumers, int light,
                       AbstractClientPlayer player, float limbAngle, float limbDistance,
                       float tickDelta, float animationProgress, float headYaw, float headPitch) {

        if (!(player instanceof BDSMable bm) || bm.getElectricShock() <= 0) return;

        matrices.pushPose();

        // 绑定到右腿
        getParentModel().rightLeg.translateAndRotate(matrices);

        // 平移：x 左右，y 上下，z 前后（相对于腿的原点）
        matrices.translate(-0.1F, 0.2F, 0.0F);
        // 旋转：绕 Y 轴旋转 90 度
        matrices.mulPose(Axis.YP.rotation((float) Math.toRadians(90)));

        // 缩放
        matrices.scale(0.5F, 0.5F, 0.5F);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                JRItems.Companion.getELECTRIC_SHOCK_DEVICE().getDefaultStack(),
                ItemDisplayContext.FIXED,
                light,
                OverlayTexture.NO_OVERLAY,
                matrices,
                vertexConsumers,
                player.level(),
                0
        );

        matrices.popPose();
    }

}
