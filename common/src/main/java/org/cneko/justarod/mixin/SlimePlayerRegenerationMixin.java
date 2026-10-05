package org.cneko.justarod.mixin;

import net.minecraft.server.level.ServerPlayer;
import org.cneko.justarod.entity.slime.TamedSlimeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class SlimePlayerRegenerationMixin {
    @Inject(method = "tickRegeneration", at = @At("HEAD"), cancellable = true)
    private void slimeDigestingPreventsNaturalRegeneration(CallbackInfo ci) {
        var player = (ServerPlayer) (Object) this;
        if (player.getVehicle() instanceof TamedSlimeEntity slime && slime.captivity().isDigesting(player)) ci.cancel();
    }
}
