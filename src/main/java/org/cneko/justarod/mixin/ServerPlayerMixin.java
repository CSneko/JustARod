package org.cneko.justarod.mixin;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import org.cneko.justarod.packet.BDSMPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerMixin {
    @Inject(method = "copyFrom",at = @At("HEAD"))
    public void copyFrom(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        player.setSterilization(oldPlayer.isSterilization());
        player.setImmune2HPV(oldPlayer.isImmune2HPV());
    }

    @Inject(method = "tick",at = @At("HEAD"))
    public void tick(CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        // 寻找周围的玩家
        player.getWorld().getEntitiesByClass(ServerPlayerEntity.class, player.getBoundingBox().expand(10), (e) -> true).forEach(e -> {
            ServerPlayNetworking.send(e, new BDSMPayload(player.getUuidAsString(), player.getBallMouth() > 0, player.getElectricShock()>0,player.getBundled()>0,player.getEyePatch()>0,player.getEarplug()>0));
        });
        ServerPlayNetworking.send(player, new BDSMPayload(player.getUuidAsString(), player.getBallMouth() > 0, player.getElectricShock()>0,player.getBundled()>0, player.getEyePatch()>0, player.getEarplug()>0));
    }

}
