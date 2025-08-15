package org.cneko.justarod.mixin.client;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.Entity;
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
        PlayerEntity player = (PlayerEntity) entity;
        if (player.getBundled() > 0) {
            PlayerEntityModel<?> self = (PlayerEntityModel<?>) (Object) this;

            // 锁定手臂
            self.rightArm.pitch = 0.0F;
            self.leftArm.pitch = 0.0F;
            self.rightArm.yaw = 0.0F;
            self.leftArm.yaw = 0.0F;
            self.rightArm.roll = 0.0F;
            self.leftArm.roll = 0.0F;

            // 锁定腿
            self.rightLeg.pitch = 0.0F;
            self.leftLeg.pitch = 0.0F;
            self.rightLeg.yaw = 0.0F;
            self.leftLeg.yaw = 0.0F;
            self.rightLeg.roll = 0.0F;
            self.leftLeg.roll = 0.0F;

            // 同步锁定袖子
            self.rightSleeve.copyTransform(self.rightArm);
            self.leftSleeve.copyTransform(self.leftArm);

            // 同步锁定裤子
            self.rightPants.copyTransform(self.rightLeg);
            self.leftPants.copyTransform(self.leftLeg);
        }
    }
}
