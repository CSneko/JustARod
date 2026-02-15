package org.cneko.justarod.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.cneko.justarod.entity.Pregnant; // 你的接口包路径
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BackgroundRenderer.class)
public class BackgroundRendererMixin {

    // 1. 修改雾的颜色：强制变成乳白色
    @Inject(method = "applyFogColor", at = @At("RETURN"))
    private static void modifyFogColorForCataract(CallbackInfo ci) {
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player instanceof Pregnant pregnant && pregnant.getCataract() > 0) {
            // 获取严重程度
            float maxSeverity = 20 * 60 * 20 * 10f;
            float progress = (float) pregnant.getCataract();

            // 计算白色混合因子 (0.0 ~ 1.0)
            float factor = Math.min(progress / maxSeverity, 1.0f);

            // 只有病情到了一定程度才开始变白
            if (factor > 0.2f) {
                // 获取原本的雾颜色
                float[] color = RenderSystem.getShaderFogColor();

                // 线性插值混合成白色 (1.0, 1.0, 1.0)
                // 这里的 factor * 0.8f 是为了保留一点点环境色，不要全白瞎了眼
                float strength = factor * 0.9f;

                float r = color[0] * (1 - strength) + strength;
                float g = color[1] * (1 - strength) + strength;
                float b = color[2] * (1 - strength) + strength;

                RenderSystem.setShaderFogColor(r, g, b);
            }
        }
    }

    // 2. 修改雾的距离：强制拉近视距（模拟模糊）
    @Inject(method = "applyFog", at = @At("HEAD"), cancellable = true)
    private static void modifyFogDistanceForCataract(Camera camera, BackgroundRenderer.FogType fogType, float viewDistance, boolean thickFog, float tickDelta, CallbackInfo ci) {
        if (camera.getFocusedEntity() instanceof LivingEntity entity && entity instanceof Pregnant pregnant) {
            int cataract = pregnant.getCataract();
            if (cataract <= 0) return;

            // 阈值定义 (需与 Pregnant 接口一致)
            int STAGE_2 = 20 * 60 * 20 * 5;

            // 只有中期以上才开始影响视距
            if (cataract > STAGE_2) {
                float newEnd = getNewEnd(viewDistance, (float) cataract);

                // 设置为 newEnd * 0.6f，意味着 0~60% 的距离是完全清晰的，只有最后 40% 开始渐变模糊
                // 这样就不会感觉眼前总是有一层雾气挡着手
                float newStart = newEnd * 0.6f;

                RenderSystem.setShaderFogStart(newStart);
                RenderSystem.setShaderFogEnd(newEnd);

                ci.cancel();
            }
        }
    }

    private static float getNewEnd(float viewDistance, float cataract) {
        float maxSeverity = 20 * 60 * 20 * 10f;
        float progress = cataract;
        float factor = Math.min(progress / maxSeverity, 1.0f);

        // 原始视距
        float originalEnd = viewDistance;

        // 计算新的视距结束点
        // 即使最严重的时候，也保留 8格 的视距，保证玩家能看清脚下的路和贴脸的怪
        float minVisiblity = 8.0f;
        float newEnd = originalEnd * (1.0f - factor * 0.9f); // 最多减少90%
        newEnd = Math.max(newEnd, minVisiblity);
        return newEnd;
    }
}