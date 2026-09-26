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
// import net.minecraft.client.model.PlayerModel;
// import net.minecraft.client.player.AbstractClientPlayer;
// import net.minecraft.client.renderer.ItemInHandRenderer;
// import net.minecraft.client.renderer.MultiBufferSource;
// import net.minecraft.client.renderer.entity.RenderLayerParent;
// import net.minecraft.client.renderer.entity.layers.RenderLayer;
// import net.minecraft.world.item.ItemDisplayContext;
// import net.minecraft.world.item.ItemStack;
// import net.minecraft.world.phys.Vec3;
// import org.cneko.justarod.item.JRItems;
// import org.joml.Vector3f;
// 
// public class HandcuffFeatureRenderer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
//     protected ItemInHandRenderer heldItemRenderer;
//     public HandcuffFeatureRenderer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> context, ItemInHandRenderer heldItemRenderer) {
//         super(context);
//         this.heldItemRenderer = heldItemRenderer;
//     }
// 
//     @Override
//     public void render(PoseStack matrices, MultiBufferSource vertexConsumers, int light,
//                        AbstractClientPlayer player, float limbAngle, float limbDistance,
//                        float tickDelta, float animationProgress, float headYaw, float headPitch) {
// 
//         // 判断玩家是否需要显示
//         if (player.getHandcuffed() <= 0) return;
// 
//         ItemStack ring = new ItemStack(JRItems.Companion.getHANDCUFFES_RING());
//         ItemStack chain = new ItemStack(JRItems.Companion.getHANDCUFFES_CHAIN());
// 
//         // 左手
//         matrices.pushPose();
//         getParentModel().leftArm.translateAndRotate(matrices);
//         matrices.translate(0.05, 0.6, 0.1);
//         matrices.scale(0.8f, 0.8f, 0.8f);
//         matrices.mulPose(Axis.XP.rotationDegrees(90f));
//         heldItemRenderer.renderItem(player, ring, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, true, matrices, vertexConsumers, light);
//         matrices.popPose();
// 
//         // 右手
//         matrices.pushPose();
//         getParentModel().rightArm.translateAndRotate(matrices);
//         matrices.translate(-0.05, 0.6, 0.1);
//         matrices.scale(0.8f, 0.8f, 0.8f);
//         matrices.mulPose(Axis.XP.rotationDegrees(90f));
//         heldItemRenderer.renderItem(player, ring, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, false, matrices, vertexConsumers, light);
//         matrices.popPose();
// 
//         // 链条（连接左右手）
//         matrices.pushPose();
//         // 使用玩家模型的手臂位置作为参考
//         Vec3 leftHandPos = new Vec3(getParentModel().leftArm.x, getParentModel().leftArm.y, getParentModel().leftArm.z);
//         Vec3 rightHandPos = new Vec3(getParentModel().rightArm.x, getParentModel().rightArm.y, getParentModel().rightArm.z);
// 
//         // 取两手的中点作为链条位置
//         double midX = (leftHandPos.x() + rightHandPos.x()) / 2.0;
//         double midY = (leftHandPos.y() + rightHandPos.y()) / 2.0 - 1.5;
//         double midZ = (leftHandPos.z() + rightHandPos.z()) / 2.0 - 0.33;
// 
//         matrices.translate(midX, midY, midZ);
//         matrices.scale(0.7f, 0.7f, 0.7f);
//         heldItemRenderer.renderItem(player, chain, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, false, matrices, vertexConsumers, light);
//         matrices.popPose();
//     }
// 
// }
 */
@SuppressWarnings("unused")
public class HandcuffFeatureRenderer extends RenderLayer<AvatarRenderState, PlayerModel> {
    public HandcuffFeatureRenderer(RenderLayerParent<AvatarRenderState, PlayerModel> context) {
        super(context);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light,
                       AvatarRenderState state, float partialTick, float f) {
        // 功能停用：渲染状态中无法获取实体 BDSMable 数据（见类注释）。
    }
}
