package org.cneko.justarod.mixin;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import org.cneko.justarod.packet.BDSMPayload;
import org.cneko.justarod.packet.MedicalPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
    @Inject(method = "copyFrom",at = @At("HEAD"))
    public void copyFrom(ServerPlayer oldPlayer, boolean alive, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        player.setSterilization(oldPlayer.isSterilization());
        player.setImmune2HPV(oldPlayer.isImmune2HPV());
    }

    @Unique
    private int slowTick =0;
    @Inject(method = "tick",at = @At("HEAD"))
    public void tick(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        slowTick++;
        if (slowTick >=10){
            player.level().getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(10), (e) -> true).forEach(e -> {
                ServerPlayNetworking.send(e, new BDSMPayload(player.getStringUUID(), player.getBallMouth() > 0, player.getElectricShock() > 0, player.getBundled() > 0, player.getEyePatch() > 0, player.getEarplug() > 0, player.getHandcuffed() > 0, player.getShackled() > 0, player.getNoMatingPlz() > 0));
            });
            ServerPlayNetworking.send(player, new BDSMPayload(player.getStringUUID(), player.getBallMouth() > 0, player.getElectricShock() > 0, player.getBundled() > 0, player.getEyePatch() > 0, player.getEarplug() > 0, player.getHandcuffed() > 0, player.getShackled() > 0, player.getNoMatingPlz() > 0));
            slowTick = 0;
        }
    }

}
