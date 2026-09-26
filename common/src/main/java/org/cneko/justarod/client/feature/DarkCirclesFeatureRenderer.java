package org.cneko.justarod.client.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

// 26.x 迁移说明：
// 26.1.2 的渲染层改为提交式管线（RenderLayer<S extends EntityRenderState, M> + submit(...)），
// 且 AvatarRenderState 不再携带原实体引用，无法在此读取 BDSMable/状态数据。
// 因此原有的饰品渲染逻辑（口球/眼罩/耳塞/电击/手铐/脚镣/捆绑覆盖层/黑眼圈/红疹）
// 暂时停用，仅保留渲染层类与注册管线，便于后续用「实体→渲染状态」数据捕获方案恢复。

/*
 * 以下为迁移前的原始实现（基于旧版 RenderLayer.render(PoseStack, MultiBufferSource, ...) 直接绘制 API），
 * 已停用，供后续按 26.x 提交式渲染管线重写时参考：
// package org.cneko.justarod.client.feature;
// 
// import static org.cneko.justarod.Justarod.MODID;
// 
// import com.mojang.blaze3d.vertex.PoseStack;
// import com.mojang.blaze3d.vertex.VertexConsumer;
// import net.minecraft.client.model.PlayerModel;
// import net.minecraft.client.player.AbstractClientPlayer;
// import net.minecraft.client.renderer.MultiBufferSource;
// import net.minecraft.client.renderer.RenderType;
// import net.minecraft.client.renderer.entity.RenderLayerParent;
// import net.minecraft.client.renderer.entity.layers.RenderLayer;
// import net.minecraft.client.renderer.texture.OverlayTexture;
// import net.minecraft.resources.Identifier;
// import org.cneko.justarod.entity.Pregnant;
// 
// // 熬夜熬出来的黑眼圈……遮都遮不住喵
// public class DarkCirclesFeatureRenderer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
// 
//     private static final Identifier MILD_TEXTURE = Identifier.fromNamespaceAndPath(MODID, "textures/entity/player/dark_circles_mild.png");
//     private static final Identifier HEAVY_TEXTURE = Identifier.fromNamespaceAndPath(MODID, "textures/entity/player/dark_circles_heavy.png");
// 
//     public DarkCirclesFeatureRenderer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> context) {
//         super(context);
//     }
// 
//     @Override
//     public void render(
//             PoseStack matrices,
//             MultiBufferSource vertexConsumers,
//             int light,
//             AbstractClientPlayer player,
//             float limbAngle,
//             float limbDistance,
//             float tickDelta,
//             float customAngle,
//             float headYaw,
//             float headPitch
//     ) {
//         if (!(player instanceof Pregnant pregnant)) return;
//         int stage = Pregnant.fatigueStage(pregnant.getFatigue());
//         if (stage < 2 || player.isInvisible()) return;
// 
//         Identifier texture = stage >= 3 ? HEAVY_TEXTURE : MILD_TEXTURE;
//         VertexConsumer consumer = vertexConsumers.getBuffer(RenderType.entityTranslucent(texture));
//         this.getParentModel().renderToBuffer(matrices, consumer, light, OverlayTexture.NO_OVERLAY);
//     }
// }
 */
@SuppressWarnings("unused")
public class DarkCirclesFeatureRenderer extends RenderLayer<AvatarRenderState, PlayerModel> {
    public DarkCirclesFeatureRenderer(RenderLayerParent<AvatarRenderState, PlayerModel> context) {
        super(context);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light,
                       AvatarRenderState state, float partialTick, float f) {
        // 功能停用：渲染状态中无法获取实体 BDSMable 数据（见类注释）。
    }
}
