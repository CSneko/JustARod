package org.cneko.justarod.mixin.client;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public class ClientPlayerEntityMixin {
    @Inject(method = "dropSelectedItem", at = @At("HEAD"), cancellable = true)
    private void disableDropSelectedItem(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (player.getHandcuffed() > 0) {
            cir.setReturnValue(false);
        }
    }
}
