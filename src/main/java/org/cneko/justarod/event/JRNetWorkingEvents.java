package org.cneko.justarod.event;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.Registries;
import org.cneko.justarod.effect.JREffects;
import org.cneko.justarod.packet.FullHeatPayload;

public class JRNetWorkingEvents {
    public static void init(){
        ServerPlayNetworking.registerGlobalReceiver(FullHeatPayload.ID, (payload,context) -> {
            // 给玩家晕倒效果
            context.player().addStatusEffect(new StatusEffectInstance(Registries.STATUS_EFFECT.getEntry(JREffects.Companion.getFAINT_EFFECT()), 1000));
        });
    }
}
