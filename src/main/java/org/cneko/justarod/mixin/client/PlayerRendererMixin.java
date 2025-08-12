package org.cneko.justarod.mixin.client;

import net.minecraft.client.render.entity.PlayerEntityRenderer;
import org.cneko.justarod.client.feature.BallMouthFeatureRenderer;
import org.cneko.justarod.client.feature.ElectricShockFeatureRenderer;
import org.cneko.justarod.client.feature.RashFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerRendererMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        PlayerEntityRenderer self = (PlayerEntityRenderer) (Object) this;
        self.addFeature(new RashFeatureRenderer(self));
        self.addFeature(new BallMouthFeatureRenderer(self));
        self.addFeature(new ElectricShockFeatureRenderer(self));
    }

}