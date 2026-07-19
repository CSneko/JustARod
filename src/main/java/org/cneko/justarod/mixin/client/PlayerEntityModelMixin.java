package org.cneko.justarod.mixin.client;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class PlayerEntityModelMixin {

    @Inject(
            method = "setAngles(Lnet/minecraft/entity/Entity;FFFFF)V",
            at = @At("TAIL")
    )
    private void onSetAngles(Entity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof Player player)) return;
        PlayerModel<?> self = (PlayerModel<?>) (Object) this;


        boolean amputated = player.isAmputated();
        boolean riding = player.isPassenger();
        boolean sneaking = player.isCrouching();
        boolean sleeping = player.isSleeping();

        if (amputated) {
            // 在正常站立/行走时才下移
            boolean applyOffset = !riding && !sneaking && !sleeping;
            float offset = applyOffset ? 12.0F : 0.0F;

            self.leftLeg.visible = false;
            self.rightLeg.visible = false;
            self.leftPants.visible = false;
            self.rightPants.visible = false;

            // 上半身整体下移 offset
            self.body.y = offset;
            self.jacket.y = offset;

            self.head.y = 0.0F + offset;
            self.hat.y = 0.0F + offset;

            self.leftArm.y = 2.0F + offset;
            self.rightArm.y = 2.0F + offset;
            self.leftSleeve.y = 2.0F + offset;
            self.rightSleeve.y = 2.0F + offset;
        } else {
            // 正常状态恢复
            self.leftLeg.visible = true;
            self.rightLeg.visible = true;
            self.leftPants.visible = true;
            self.rightPants.visible = true;

            self.body.y = 0F;
            self.jacket.y = 0F;

            self.head.y = 0.0F;
            self.hat.y = 0.0F;

            self.leftArm.y = 2.0F;
            self.rightArm.y = 2.0F;
            self.leftSleeve.y = 2.0F;
            self.rightSleeve.y = 2.0F;
        }



        if (player.getBundled() > 0) {
            

            boolean sitting = player.isPassenger() || player.getPose() == Pose.SITTING;

            self.rightArm.xRot = 0.0F;
            self.leftArm.xRot = 0.0F;
            if (!sitting) {
                // 普通站立：手脚全部锁死
                self.rightLeg.xRot = 0.0F;
                self.leftLeg.xRot = 0.0F;
            } else {
                // 坐姿：手臂依然锁死，腿并拢

                // 腿的 pitch 保留坐姿角度（大约 -1.5708F）但 yaw 归零，避免岔开
                self.rightLeg.yRot = 0.0F;
                self.leftLeg.yRot = 0.0F;
            }

            // 手臂 yaw/roll 固定
            self.rightArm.yRot = 0.0F;
            self.leftArm.yRot = 0.0F;
            self.rightArm.zRot = 0.0F;
            self.leftArm.zRot = 0.0F;

            if (!sitting) {
                self.rightLeg.yRot = 0.0F;
                self.leftLeg.yRot = 0.0F;
                self.rightLeg.zRot = 0.0F;
                self.leftLeg.zRot = 0.0F;
            } else {
                // 坐姿时腿的 roll 也归零，防止外翻
                self.rightLeg.zRot = 0.0F;
                self.leftLeg.zRot = 0.0F;
            }

            // 同步外层
            self.rightSleeve.copyFrom(self.rightArm);
            self.leftSleeve.copyFrom(self.leftArm);
            self.rightPants.copyFrom(self.rightLeg);
            self.leftPants.copyFrom(self.leftLeg);
        }

        if (player.getHandcuffed() > 0) {

            // 双手靠拢在胸前
            self.rightArm.xRot = -0.8F; // 手臂抬起
            self.leftArm.xRot = -0.8F;

            self.rightArm.yRot = -0.3F;   // 向内靠拢
            self.leftArm.yRot = 0.3F;

            self.rightArm.zRot = 0.0F;
            self.leftArm.zRot = 0.0F;

            // 保证袖子同步
            self.rightSleeve.copyFrom(self.rightArm);
            self.leftSleeve.copyFrom(self.leftArm);
        }

    }
}
