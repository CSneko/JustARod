package org.cneko.justarod.mixin;

import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.*;
import org.cneko.justarod.disassembly.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(Player.class)
public abstract class DisassemblyPlayerMixin implements DisassemblyState.Holder {
    @Unique private final DisassemblyState justarod$parts = new DisassemblyState();
    @Override public DisassemblyState justarod$disassembly() { return justarod$parts; }
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void justarod$saveParts(ValueOutput output, CallbackInfo ci) { justarod$parts.save(output); }
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void justarod$loadParts(ValueInput input, CallbackInfo ci) { justarod$parts.load(input); }
    @Inject(method = "updatePlayerPose", at = @At("TAIL"))
    private void justarod$crawl(CallbackInfo ci) {
        Player p = (Player)(Object)this;
        if (justarod$parts.crawling() && !p.isPassenger() && !p.isSleeping() && !p.getAbilities().flying && !p.isFallFlying()) p.setPose(Pose.SWIMMING);
    }
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void justarod$missingArm(net.minecraft.world.entity.Entity target, CallbackInfo ci) {
        if (!(target instanceof DetachedPartEntity) && DisassemblyController.blocked((Player)(Object)this, net.minecraft.world.InteractionHand.MAIN_HAND)) ci.cancel();
    }
}
