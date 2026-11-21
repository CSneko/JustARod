package org.cneko.justarod.mixin;

import net.minecraft.world.entity.Mob;
import org.cneko.justarod.pet.PetOwnerActivity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Suspend autonomous travel only for participating absurdity actors, without changing the saved NoAI flag. */
@Mixin(Mob.class)
public abstract class PetOwnerAiMixin {
    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    private void justarod$ownerActor(CallbackInfo ci) {
        if (PetOwnerActivity.controlAi((Mob) (Object) this)) ci.cancel();
    }
}
