package org.cneko.justarod.event;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import org.cneko.justarod.effect.JREffects;
import org.cneko.justarod.packet.FullHeatPayload;

public class JRNetWorkingEvents {
    public static void init(){
        ServerPlayNetworking.registerGlobalReceiver(FullHeatPayload.ID, (payload,context) -> {
            // 消耗体力
            PlayerEntity player = context.player();
            player.setPower(player.getPower()-80);
        });
    }
}
