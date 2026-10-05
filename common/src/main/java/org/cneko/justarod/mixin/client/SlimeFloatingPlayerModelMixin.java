package org.cneko.justarod.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.cneko.justarod.entity.slime.TamedSlimeEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Gently spread the limbs, including armor models, instead of using vanilla's bent seated legs. */
@Mixin(HumanoidModel.class)
public abstract class SlimeFloatingPlayerModelMixin {
    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart leftArm;
    @Shadow @Final public ModelPart rightLeg;
    @Shadow @Final public ModelPart leftLeg;
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
    private void floatingLimbs(HumanoidRenderState state, CallbackInfo ci) {
        if (!(state instanceof AvatarRenderState avatar)) return;
        var level = Minecraft.getInstance().level;
        var player = level == null ? null : level.getEntity(avatar.id);
        if (player == null || !(player.getVehicle() instanceof TamedSlimeEntity slime) || !slime.isFloatingCaptive(player)) return;
        float sway = (float) Math.sin(state.ageInTicks * 0.08f) * 0.035f;
        rightArm.xRot = leftArm.xRot = -0.12f + sway;
        rightArm.zRot = 0.25f + sway;
        leftArm.zRot = -0.25f - sway;
        rightLeg.xRot = -0.10f + sway;
        leftLeg.xRot = 0.08f - sway;
        rightLeg.yRot = leftLeg.yRot = 0;
        rightLeg.zRot = 0.06f;
        leftLeg.zRot = -0.06f;
    }
}
