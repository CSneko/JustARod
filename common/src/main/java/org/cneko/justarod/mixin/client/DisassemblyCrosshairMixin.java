package org.cneko.justarod.mixin.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.cneko.justarod.client.disassembly.DisassemblyClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class DisassemblyCrosshairMixin {
    @Inject(method="extractCrosshair",at=@At("HEAD"),cancellable=true)
    private void justarod$splitCrosshair(GuiGraphicsExtractor g,DeltaTracker tracker,CallbackInfo ci) {
        if (DisassemblyClient.splitting()) ci.cancel();
    }
}
