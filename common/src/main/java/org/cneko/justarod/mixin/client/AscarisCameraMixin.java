package org.cneko.justarod.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.cneko.justarod.ascaris.AscarisEntity;
import org.cneko.justarod.client.ascaris.AscarisClientState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 第一人称镜头微律动反馈：
 * - 腹鸣/咳嗽/肠梗阻绞痛/驱虫排出时的轻微律动与震颤；
 * - 尾巴缠绕期间仅叠加柔和呼吸起伏与勒紧微震（由 wrapVisualWeight 平滑淡入淡出，
 *   不再硬性拉扯相机偏转角，彻底杜绝转动鼠标被突兀弹回或挣脱瞬间跳切）。
 */
@Mixin(Camera.class)
public abstract class AscarisCameraMixin {

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Shadow
    protected abstract void move(float forward, float up, float left);

    @Inject(method = "update", at = @At("TAIL"))
    private void justarod$ascarisCameraMotion(DeltaTracker tracker, CallbackInfo ci) {
        Camera self = (Camera) (Object) this;
        Entity camEntity = self.entity();
        if (camEntity == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (camEntity != mc.player) return;

        float pt = self.getCameraEntityPartialTicks(tracker);

        // 缩小玩家坐在粉白蛔虫娘胸口上时：相机实时跟随她的胸部弹簧上下轻颤与左右微晃
        if (camEntity.getVehicle() instanceof AscarisEntity carrier) {
            float bounceY = carrier.getBustBounceY(pt);
            float swayX = carrier.getBustSwayX(pt);
            if (!self.isDetached()) {
                if (Math.abs(bounceY) > 0.001f || Math.abs(swayX) > 0.001f) {
                    move(0f, bounceY * 0.036f, swayX * 0.024f);
                    setRotation(
                            self.yRot() + swayX * 2.6f,
                            Mth.clamp(self.xRot() - bounceY * 3.8f, -89.5f, 89.5f)
                    );
                }
            }
            return;
        }

        if (self.isDetached()) return;

        float t = (camEntity.tickCount + pt) / 20.0f;
        float yawOff = 0f;
        float pitchOff = 0f;

        float rumble = AscarisClientState.rumbleIntensity();
        if (rumble > 0.01f) {
            pitchOff += Mth.sin(t * 7.5f) * rumble * 1.1f;
        }
        float cough = AscarisClientState.coughIntensity();
        if (cough > 0.01f) {
            pitchOff += Mth.sin(cough * Mth.PI * 2.0f) * cough * 2.6f;
        }
        float cramp = AscarisClientState.crampIntensity();
        if (cramp > 0.01f) {
            yawOff += Mth.sin(t * 28.0f) * cramp * 2.1f;
            pitchOff += Mth.cos(t * 23.0f) * cramp * 1.8f;
        }
        if (AscarisClientState.isExpelling()) {
            float p = AscarisClientState.expulsionProgress();
            yawOff += Mth.sin(t * 36.0f) * p * 2.4f;
            pitchOff += Mth.cos(t * 31.0f) * p * 1.6f;
        }
        if (AscarisClientState.hasWrapVisual()) {
            float w = AscarisClientState.wrapVisualWeight();
            boolean yandere = AscarisClientState.visualWrapMode() == AscarisEntity.WRAP_YANDERE;
            float tighten = AscarisClientState.tightenIntensity();
            pitchOff += Mth.sin(t * (yandere ? 3.8f : 2.4f)) * (yandere ? 0.85f : 0.55f) * w;
            if (tighten > 0.01f) {
                yawOff += Mth.sin(t * 24.0f) * tighten * 1.2f * w;
                pitchOff += Mth.cos(t * 20.0f) * tighten * 1.0f * w;
            }
        }

        if (Math.abs(yawOff) > 0.001f || Math.abs(pitchOff) > 0.001f) {
            setRotation(self.yRot() + yawOff, Mth.clamp(self.xRot() + pitchOff, -89.5f, 89.5f));
        }
    }
}
