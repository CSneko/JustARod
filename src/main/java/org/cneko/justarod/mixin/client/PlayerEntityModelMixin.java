package org.cneko.justarod.mixin.client;

import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityModel.class)
public abstract class PlayerEntityModelMixin {

    @Inject(
            method = "setAngles(Lnet/minecraft/entity/Entity;FFFFF)V",
            at = @At("TAIL")
    )
    private void onSetAngles(Entity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof PlayerEntity player)) return;

        if (player.getBundled() > 0) {
            PlayerEntityModel<?> self = (PlayerEntityModel<?>) (Object) this;

            boolean sitting = player.hasVehicle() || player.getPose() == EntityPose.SITTING;

            self.rightArm.pitch = 0.0F;
            self.leftArm.pitch = 0.0F;
            if (!sitting) {
                // 普通站立：手脚全部锁死
                self.rightLeg.pitch = 0.0F;
                self.leftLeg.pitch = 0.0F;
            } else {
                // 坐姿：手臂依然锁死，腿并拢

                // 腿的 pitch 保留坐姿角度（大约 -1.5708F）但 yaw 归零，避免岔开
                self.rightLeg.yaw = 0.0F;
                self.leftLeg.yaw = 0.0F;
            }

            // 手臂 yaw/roll 固定
            self.rightArm.yaw = 0.0F;
            self.leftArm.yaw = 0.0F;
            self.rightArm.roll = 0.0F;
            self.leftArm.roll = 0.0F;

            if (!sitting) {
                self.rightLeg.yaw = 0.0F;
                self.leftLeg.yaw = 0.0F;
                self.rightLeg.roll = 0.0F;
                self.leftLeg.roll = 0.0F;
            } else {
                // 坐姿时腿的 roll 也归零，防止外翻
                self.rightLeg.roll = 0.0F;
                self.leftLeg.roll = 0.0F;
            }

            // 同步外层
            self.rightSleeve.copyTransform(self.rightArm);
            self.leftSleeve.copyTransform(self.leftArm);
            self.rightPants.copyTransform(self.rightLeg);
            self.leftPants.copyTransform(self.leftLeg);
        }

        if (player.getHandcuffed() > 0) {
            PlayerEntityModel<?> self = (PlayerEntityModel<?>) (Object) this;

            // 双手靠拢在胸前
            self.rightArm.pitch = -0.8F; // 手臂抬起
            self.leftArm.pitch = -0.8F;

            self.rightArm.yaw = -0.3F;   // 向内靠拢
            self.leftArm.yaw = 0.3F;

            self.rightArm.roll = 0.0F;
            self.leftArm.roll = 0.0F;

            // 保证袖子同步
            self.rightSleeve.copyTransform(self.rightArm);
            self.leftSleeve.copyTransform(self.leftArm);
        }

    }
}
