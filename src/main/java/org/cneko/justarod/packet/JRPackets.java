package org.cneko.justarod.packet;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class JRPackets {
    public static void init(){
        PayloadTypeRegistry.playS2C().register(FrictionPayload.ID, FrictionPayload.CODEC);
    }
}
