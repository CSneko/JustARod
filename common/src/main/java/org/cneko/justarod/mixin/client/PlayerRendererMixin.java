package org.cneko.justarod.mixin.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.cneko.justarod.client.feature.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(EntityRendererProvider.Context ctx, boolean slim, CallbackInfo ci) {
        PlayerRenderer self = (PlayerRenderer) (Object) this;
        self.addLayer(new RashFeatureRenderer(self));
        self.addLayer(new BallMouthFeatureRenderer(self));
        self.addLayer(new ElectricShockFeatureRenderer(self));
        self.addLayer(new BundledFeatureRenderer(self));
        self.addLayer(new EyePatchFeatureRenderer(self));
        self.addLayer(new EarplugFeatureRenderer(self));
        self.addLayer(new HandcuffFeatureRenderer(self,ctx.getItemInHandRenderer()));
        self.addLayer(new ShacklesFeatureRenderer(self,ctx.getItemInHandRenderer()));
    }

}