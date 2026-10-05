package org.cneko.justarod.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.cneko.justarod.entity.slime.TamedSlimeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep dismounting authoritative and deny attacks even when invoked outside the vanilla packet handler. */
@Mixin(Player.class)
public class SlimeCaptivePlayerMixin {
    @Inject(method = "wantsToStopRiding", at = @At("HEAD"), cancellable = true)
    private void slimeDismount(CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) (Object) this;
        if (player.getVehicle() instanceof TamedSlimeEntity) cir.setReturnValue(false);
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void slimeCaptiveAttack(Entity target, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (player.getVehicle() instanceof TamedSlimeEntity slime && slime.isCaptive(player)) ci.cancel();
    }
}
