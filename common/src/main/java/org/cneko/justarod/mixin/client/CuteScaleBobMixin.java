package org.cneko.justarod.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.cneko.justarod.ascaris.AscarisEntity;
import org.cneko.justarod.scale.CuteScaleController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 萌态体型视角防抖：
 * - 原版 bobView 在体型缩小时会以极高频与未衰减的振幅猛烈晃动镜头；
 * - 此处按体型等比柔化振幅与步频，确保缩小至 0.25x~0.6x 时行走丝滑平稳不晕 3D；
 * - 坐在粉白蛔虫娘胸口上时静音原版脚步晃动，交由 AscarisCameraMixin 提供同步胸口弹性晃动。
 */
@Mixin(GameRenderer.class)
public abstract class CuteScaleBobMixin {

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void justarod$smoothScaledBobView(CameraRenderState state, PoseStack poseStack, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || !state.entityRenderState.isPlayer) return;

        // 坐在蛔虫娘胸口时，由 AscarisCameraMixin 同步胸部弹簧微动，避免叠加生硬步行晃动
        if (player.getVehicle() instanceof AscarisEntity) {
            ci.cancel();
            return;
        }

        float scale = CuteScaleController.getEffectiveClientScale(player);
        if (Math.abs(scale - 1.0f) <= 0.025f) {
            return;
        }

        // 等比衰减振幅并柔和步频：0.25x 时振幅衰减至 ~24%，完全告别晕 3D 抖动
        float ampScale = Mth.clamp((float) Math.pow(scale, 0.92), 0.16f, 1.20f);
        float freqScale = Mth.clamp((float) Math.pow(scale, 0.36), 0.58f, 1.25f);

        float walk = state.entityRenderState.backwardsInterpolatedWalkDistance * freqScale;
        float bob = state.entityRenderState.bob * ampScale;

        poseStack.translate(
                Mth.sin(walk * Mth.PI) * bob * 0.5f,
                -Math.abs(Mth.cos(walk * Mth.PI) * bob),
                0.0f
        );
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(walk * Mth.PI) * bob * 2.4f));
        poseStack.mulPose(Axis.XP.rotationDegrees(Math.abs(Mth.cos(walk * Mth.PI - 0.2f) * bob) * 3.8f));
        ci.cancel();
    }
}
