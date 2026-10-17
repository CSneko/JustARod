package org.cneko.justarod.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.HumanoidArm;
import org.cneko.justarod.client.renderer.EndRodGunAnimation;
import org.cneko.justarod.item.EndRodGunItem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class EndRodGunModelMixin {
    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart leftArm;

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
    private void justarod$spreadGunArms(HumanoidRenderState state, CallbackInfo ci) {
        if (!(state instanceof AvatarRenderState avatar)) return;
        var level = Minecraft.getInstance().level;
        var entity = level == null ? null : level.getEntity(avatar.id);
        if (!(entity instanceof LivingEntity player)) return;
        if (EndRodGunItem.isSpinning(player)) {
            rightArm.xRot = leftArm.xRot = 0;
            rightArm.yRot = leftArm.yRot = 0;
            rightArm.zRot = (float) (Math.PI / 2);
            leftArm.zRot = (float) (-Math.PI / 2);
            return;
        }
        float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        if (EndRodGunAnimation.holdsGun(player, HumanoidArm.RIGHT)) {
            EndRodGunAnimation.poseArm(rightArm, player, HumanoidArm.RIGHT, state, partialTick);
        }
        if (EndRodGunAnimation.holdsGun(player, HumanoidArm.LEFT)) {
            EndRodGunAnimation.poseArm(leftArm, player, HumanoidArm.LEFT, state, partialTick);
        }
    }
}
