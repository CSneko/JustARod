package org.cneko.justarod.mixin.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.cneko.justarod.client.slime.SlimeCaptivityClientState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Use vanilla's actual blur post-processing after the world; preserve the player's saved graphics settings. */
@Mixin(GameRenderer.class)
public class SlimeDigestionBlurMixin {
    @Inject(method = "extractOptions", at = @At("TAIL"))
    private void slimeBlurRadius(CallbackInfo ci) {
        float blur = SlimeCaptivityClientState.blur();
        if (blur > 0) {
            var options = ((GameRenderer) (Object) this).getGameRenderState().optionsRenderState;
            int radius = Math.round(blur * 10);
            options.menuBackgroundBlurriness = net.minecraft.client.Minecraft.getInstance().screen == null
                    ? radius : Math.max(options.menuBackgroundBlurriness, radius);
        }
    }
    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void slimeBlurWorld(DeltaTracker deltaTracker, CallbackInfo ci) {
        if (SlimeCaptivityClientState.blur() > 0) ((GameRenderer) (Object) this).processBlurEffect();
    }
}
