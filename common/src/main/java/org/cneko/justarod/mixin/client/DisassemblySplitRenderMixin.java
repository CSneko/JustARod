package org.cneko.justarod.mixin.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.cneko.justarod.client.disassembly.SplitBodyView;
import org.cneko.justarod.client.disassembly.DisassemblyClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class DisassemblySplitRenderMixin {
    @Inject(method="extract",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/GameRenderer;extractGui(Lnet/minecraft/client/DeltaTracker;ZZ)V"))
    private void justarod$keepBodyControls(DeltaTracker tracker,boolean renderLevel,CallbackInfo ci) {
        // The remote entity is needed only while extracting the secondary world view.
        // Restore the body before extracting HUD and before the next movement tick.
        DisassemblyClient.restoreBodyEntity();
    }
    @Inject(method="renderLevel",at=@At("TAIL"))
    private void justarod$captureEye(DeltaTracker tracker,CallbackInfo ci) { SplitBodyView.capture(); }
    @Inject(method="close",at=@At("HEAD"))
    private void justarod$closeEyes(CallbackInfo ci) { SplitBodyView.close(); }
}
