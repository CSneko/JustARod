package org.cneko.justarod.packet;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class JRPackets {
    public static void init(){
        PayloadTypeRegistry.playS2C().register(FrictionPayload.ID, FrictionPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(FullHeatPayload.ID, FullHeatPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PowerSyncPayload.ID, PowerSyncPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(MatePayload.ID, MatePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PassiveMatingPayload.ID, PassiveMatingPayload.CODEC);
    }
}
