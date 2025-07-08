package org.cneko.justarod.mixin.client;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public class PlayerRendererMixin {
    @Unique
    private static final int TOTAL_PREGNANCY_TICKS = 10 * 24 * 60 * 60 * 20;

    @Inject(method = "setModelPose", at = @At("TAIL"))
    private void onSetModelPose(AbstractClientPlayerEntity player, CallbackInfo ci) {
        boolean isPregnant = player.isPregnant();
        int pregnancyProgress = player.getPregnant(); // 注意：这里应该是int类型

        if (isPregnant && pregnancyProgress > 0) {
            PlayerEntityModel<AbstractClientPlayerEntity> model = ((PlayerEntityRenderer)(Object)this).getModel();

            // 计算进度比例（0.0~1.0）
            float progress = 1.0f - (pregnancyProgress / (float)TOTAL_PREGNANCY_TICKS);

            // 使用曲线函数使变化更平滑
            float bellySize = (float) Math.pow(progress, 1.5); // 1.5次方曲线

            // 调整身体位置和大小（使用更小的缩放因子）
            model.body.pivotY = -1.0F + bellySize * 0.8F; // 减少Y轴移动
            model.body.pivotZ = -0.2F - bellySize * 0.3F; // 减少Z轴移动

            // 减小缩放系数
            model.body.xScale = 1.0F + bellySize * 0.2F;  // 宽度增加20%
            model.body.yScale = 1.0F + bellySize * 0.15F; // 高度增加15%
            model.body.zScale = 1.0F + bellySize * 0.25F; // 厚度增加25%
        }
    }

    @Inject(method = "scale*", at = @At("TAIL"))
    private void onScale(AbstractClientPlayerEntity player, MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        boolean isPregnant = player.isPregnant();
        int pregnancyProgress = player.getPregnant();

        if (isPregnant && pregnancyProgress > 0) {
            float progress = 1.0f - (pregnancyProgress / (float)TOTAL_PREGNANCY_TICKS);
            float bellySize = (float) Math.pow(progress, 1.5);

            // 减小整体位移量
            matrices.translate(0.0, -bellySize * 0.05, 0.0); // 减少Y轴位移

            if (player.isInSneakingPose()) {
                matrices.translate(0.0, -bellySize * 0.02, 0.0); // 减少蹲下时的额外位移
            }
        }
    }
}