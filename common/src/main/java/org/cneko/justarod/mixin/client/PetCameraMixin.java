package org.cneko.justarod.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.phys.Vec3;
import org.cneko.justarod.client.pet.PetClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class PetCameraMixin {
    @Shadow protected abstract void setPosition(Vec3 position);
    @Inject(method = "update", at = @At("TAIL"))
    private void justarod$seatedCamera(DeltaTracker tracker, CallbackInfo ci) {
        Camera camera = (Camera) (Object) this;
        if (camera.entity() != null && !camera.isDetached() && PetClient.sitting(camera.entity().getId()))
            setPosition(camera.position().add(0, -0.55, 0));
    }
}
