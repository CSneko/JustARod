package org.cneko.justarod.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.cneko.justarod.body.BodyWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class BodyPlayerDeathMixin {
    @Inject(method = "die", at = @At("TAIL"))
    private void justarod$resetBodyOnDeath(DamageSource source, CallbackInfo ci) {
        BodyWorld.onPlayerDeath((ServerPlayer) (Object) this);
    }
}
