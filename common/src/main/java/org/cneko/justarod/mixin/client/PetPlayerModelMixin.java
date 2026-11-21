package org.cneko.justarod.mixin.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.cneko.justarod.client.pet.PetClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class PetPlayerModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
    private void justarod$petPose(HumanoidRenderState state, CallbackInfo ci) {
        if (state instanceof AvatarRenderState avatar) PetClient.pose((HumanoidModel<?>) (Object) this, avatar);
    }
}
