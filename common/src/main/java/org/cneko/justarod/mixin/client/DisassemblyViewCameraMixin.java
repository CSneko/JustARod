package org.cneko.justarod.mixin.client;

import net.minecraft.client.*;
import net.minecraft.world.phys.Vec3;
import org.cneko.justarod.client.disassembly.DisassemblyClient;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class DisassemblyViewCameraMixin {
    @Shadow protected abstract void setPosition(Vec3 position);
    @Shadow protected abstract void setRotation(float yaw,float pitch);
    @Inject(method="update",at=@At("HEAD"))
    private void justarod$selectEye(DeltaTracker tracker,CallbackInfo ci) { DisassemblyClient.beginCamera((Camera)(Object)this,tracker); }
    @Inject(method="update",at=@At("TAIL"))
    private void justarod$eyePosition(DeltaTracker tracker,CallbackInfo ci) {
        if (DisassemblyClient.remoteFrame() && DisassemblyClient.anchorEye()!=null) {
            setPosition(DisassemblyClient.anchorEye()); setRotation(DisassemblyClient.anchorYaw(),DisassemblyClient.anchorPitch());
        }
    }
}
