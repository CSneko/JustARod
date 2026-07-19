package org.cneko.justarod.client.feature;

import com.mojang.blaze3d.vertex.PoseStack;
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

public class EyePatchFeatureRenderer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {


    public EyePatchFeatureRenderer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> context) {
        super(context);
    }

    @Override
    public void render(PoseStack matrices, MultiBufferSource vertexConsumers, int light,
                       AbstractClientPlayer player, float limbAngle, float limbDistance,
                       float tickDelta, float animationProgress, float headYaw, float headPitch) {

        if (!(player instanceof BDSMable bm) || bm.getEyePatch() <= 0) return;

        matrices.pushPose();

        // 跟随头部旋转
        getParentModel().head.translateAndRotate(matrices);

        // 平移：x 左右，y 上下，z 前后（单位是方块的 1/16）
        matrices.translate(0.0F, -0.3f, 0f);
        // 缩放：稍微小一点
        matrices.scale(0.7F, 0.7F, 0.7F);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                JRItems.Companion.getEYE_PATCH().getDefaultStack(),
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
