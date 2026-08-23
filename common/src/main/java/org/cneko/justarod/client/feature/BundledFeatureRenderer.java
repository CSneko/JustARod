package org.cneko.justarod.client.feature;

import static org.cneko.justarod.Justarod.MODID;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class BundledFeatureRenderer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static final ResourceLocation BUNDLED_TEXTURE = ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/player/bundled_overlay.png");

    public BundledFeatureRenderer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> context) {
        super(context);
    }

    @Override
    public void render(
            PoseStack matrices,
            MultiBufferSource vertexConsumers,
            int light,
            AbstractClientPlayer player,
            float limbAngle,
            float limbDistance,
            float tickDelta,
            float customAngle,
            float headYaw,
            float headPitch
    ) {
        if (player.getBundled() <= 0) return;

        VertexConsumer consumer = vertexConsumers.getBuffer(RenderType.entityTranslucent(BUNDLED_TEXTURE));

        matrices.scale(1.01f,1.01f,1.01f);

        // 直接复用上下文模型（姿势和旋转已在主渲染中同步过）
        this.getParentModel().renderToBuffer(matrices, consumer, light, OverlayTexture.NO_OVERLAY);
    }
}
