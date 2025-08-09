package org.cneko.justarod.client.feature;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import static org.cneko.justarod.Justarod.MODID;

public class RashFeatureRenderer extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

    private static final Identifier RASH_TEXTURE = Identifier.of(MODID, "textures/entity/player/syphilitic_rash_overlay.png");

    public RashFeatureRenderer(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context) {
        super(context);
    }

    @Override
    public void render(
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            AbstractClientPlayerEntity player,
            float limbAngle,
            float limbDistance,
            float tickDelta,
            float customAngle,
            float headYaw,
            float headPitch
    ) {
        int syphilisLevel = player.getSyphilis();
        if (syphilisLevel <= 24000 || syphilisLevel > 48000) return;

        VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(RASH_TEXTURE));

        // 直接复用上下文模型（姿势和旋转已在主渲染中同步过）
        this.getContextModel().render(matrices, consumer, light, OverlayTexture.DEFAULT_UV);
    }
}
