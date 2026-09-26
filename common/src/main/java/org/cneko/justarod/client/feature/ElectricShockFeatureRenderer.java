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
// import com.mojang.blaze3d.vertex.PoseStack;
// import com.mojang.math.Axis;
// import net.minecraft.client.Minecraft;
// import net.minecraft.client.model.PlayerModel;
// import net.minecraft.client.player.AbstractClientPlayer;
// import net.minecraft.client.renderer.MultiBufferSource;
// import net.minecraft.client.renderer.entity.RenderLayerParent;
// import net.minecraft.client.renderer.entity.layers.RenderLayer;
// import net.minecraft.client.renderer.texture.OverlayTexture;
// import net.minecraft.world.item.ItemDisplayContext;
// import org.cneko.justarod.entity.BDSMable;
// import org.cneko.justarod.item.JRItems;
// import org.joml.Vector3f;
// 
// public class ElectricShockFeatureRenderer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
// 
//     public ElectricShockFeatureRenderer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> context) {
//         super(context);
//     }
// 
//     @Override
//     public void render(PoseStack matrices, MultiBufferSource vertexConsumers, int light,
//                        AbstractClientPlayer player, float limbAngle, float limbDistance,
//                        float tickDelta, float animationProgress, float headYaw, float headPitch) {
// 
//         if (!(player instanceof BDSMable bm) || bm.getElectricShock() <= 0) return;
// 
//         matrices.pushPose();
// 
//         // 绑定到右腿
//         getParentModel().rightLeg.translateAndRotate(matrices);
// 
//         // 平移：x 左右，y 上下，z 前后（相对于腿的原点）
//         matrices.translate(-0.1F, 0.2F, 0.0F);
//         // 旋转：绕 Y 轴旋转 90 度
//         matrices.mulPose(Axis.YP.rotation((float) Math.toRadians(90)));
// 
//         // 缩放
//         matrices.scale(0.5F, 0.5F, 0.5F);
// 
//         Minecraft.getInstance().getItemRenderer().renderStatic(
//                 JRItems.Companion.getELECTRIC_SHOCK_DEVICE().getDefaultInstance(),
//                 ItemDisplayContext.FIXED,
//                 light,
//                 OverlayTexture.NO_OVERLAY,
//                 matrices,
//                 vertexConsumers,
//                 player.level(),
//                 0
//         );
// 
//         matrices.popPose();
//     }
// 
// }
 */
@SuppressWarnings("unused")
public class ElectricShockFeatureRenderer extends RenderLayer<AvatarRenderState, PlayerModel> {
    public ElectricShockFeatureRenderer(RenderLayerParent<AvatarRenderState, PlayerModel> context) {
        super(context);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light,
                       AvatarRenderState state, float partialTick, float f) {
        // 功能停用：渲染状态中无法获取实体 BDSMable 数据（见类注释）。
    }
}
