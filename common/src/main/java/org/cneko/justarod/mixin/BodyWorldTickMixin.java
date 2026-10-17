package org.cneko.justarod.mixin;

import net.minecraft.server.level.ServerLevel;
import org.cneko.justarod.body.BodyWorld;
import org.cneko.justarod.config.JRConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(value = ServerLevel.class, priority = 2000)
public abstract class BodyWorldTickMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void justarod$stopDisabledBodyWorld(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        ServerLevel level = (ServerLevel) (Object) this;
        if (level.dimension().equals(BodyWorld.DIMENSION) && !JRConfig.isInsideBodyEnabled()) ci.cancel();
    }
}
