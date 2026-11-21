package org.cneko.justarod.mixin.client;

import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import org.cneko.justarod.client.pet.PetClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class PetInputMixin extends ClientInput {
    @Inject(method = "tick", at = @At("TAIL"))
    private void justarod$assist(CallbackInfo ci) { moveVector = PetClient.movement(moveVector, keyPresses); }
}
