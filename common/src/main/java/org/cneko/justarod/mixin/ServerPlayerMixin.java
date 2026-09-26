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
    @Inject(method = "restoreFrom",at = @At("HEAD"))
    public void copyFrom(ServerPlayer oldPlayer, boolean alive, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        // 26.x：编译期接口由 mixin 注入，需显式转换
        ((org.cneko.justarod.entity.Pregnant) (Object) player).setSterilization(((org.cneko.justarod.entity.Pregnant) (Object) oldPlayer).isSterilization());
        ((org.cneko.justarod.entity.Pregnant) (Object) player).setImmune2HPV(((org.cneko.justarod.entity.Pregnant) (Object) oldPlayer).isImmune2HPV());
    }

    @Unique
    private int slowTick =0;
    @Inject(method = "tick",at = @At("HEAD"))
    public void tick(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        slowTick++;
        if (slowTick >=10){
            player.level().getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(10), (e) -> true).forEach(e -> {
                ServerPlayNetworking.send(e, new BDSMPayload(player.getStringUUID(), ((org.cneko.justarod.entity.BDSMable) (Object) player).getBallMouth() > 0, ((org.cneko.justarod.entity.BDSMable) (Object) player).getElectricShock() > 0, ((org.cneko.justarod.entity.BDSMable) (Object) player).getBundled() > 0, ((org.cneko.justarod.entity.BDSMable) (Object) player).getEyePatch() > 0, ((org.cneko.justarod.entity.BDSMable) (Object) player).getEarplug() > 0, ((org.cneko.justarod.entity.BDSMable) (Object) player).getHandcuffed() > 0, ((org.cneko.justarod.entity.BDSMable) (Object) player).getShackled() > 0, ((org.cneko.justarod.entity.BDSMable) (Object) player).getNoMatingPlz() > 0));
            });
            ServerPlayNetworking.send(player, new BDSMPayload(player.getStringUUID(), ((org.cneko.justarod.entity.BDSMable) (Object) player).getBallMouth() > 0, ((org.cneko.justarod.entity.BDSMable) (Object) player).getElectricShock() > 0, ((org.cneko.justarod.entity.BDSMable) (Object) player).getBundled() > 0, ((org.cneko.justarod.entity.BDSMable) (Object) player).getEyePatch() > 0, ((org.cneko.justarod.entity.BDSMable) (Object) player).getEarplug() > 0, ((org.cneko.justarod.entity.BDSMable) (Object) player).getHandcuffed() > 0, ((org.cneko.justarod.entity.BDSMable) (Object) player).getShackled() > 0, ((org.cneko.justarod.entity.BDSMable) (Object) player).getNoMatingPlz() > 0));
            slowTick = 0;
        }
    }

}
