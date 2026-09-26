package org.cneko.justarod.client.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * 26.x 提交式渲染管线：
 * AvatarRenderer 在 extract 阶段把口球物品解析为 ItemStackRenderState，
 * 这里在 submit 阶段贴到头部模型上。
 */
public class BallMouthFeatureRenderer extends RenderLayer<AvatarRenderState, PlayerModel> {

    public BallMouthFeatureRenderer(RenderLayerParent<AvatarRenderState, PlayerModel> context) {
        super(context);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light,
                       AvatarRenderState state, float partialTick, float f) {
        ItemStackRenderState item = BallMouthRenderStateCache.get(state);
        if (item == null || item.isEmpty()) {
            return;
        }

        poseStack.pushPose();

        getParentModel().head.translateAndRotate(poseStack);
        poseStack.translate(0.0F, 0.1F, 0.0F);
        poseStack.scale(0.7F, 0.7F, 0.7F);

        item.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);

        poseStack.popPose();
    }
}
