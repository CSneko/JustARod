package org.cneko.justarod.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import org.cneko.justarod.client.renderer.empathy.EmpathyAnimation;
import org.cneko.justarod.config.JRConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class EmpathyCameraMixin {
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Inject(method = "update", at = @At("TAIL"))
    private void justarod$empathyCamera(DeltaTracker tracker, CallbackInfo ci) {
        Camera self = (Camera) (Object) this;
        if (self.isDetached() || !JRConfig.isEmpathyDollCameraMotion()) return;
        float partialTick = self.getCameraEntityPartialTicks(tracker);
        var reaction = EmpathyAnimation.reaction(self.entity(), partialTick);
        if (reaction == null) return;
        float shake = EmpathyAnimation.shake(reaction, reaction.age(partialTick));
        setRotation(self.yRot() + shake * 0.09f, self.xRot() + shake * 0.12f);
    }
}
