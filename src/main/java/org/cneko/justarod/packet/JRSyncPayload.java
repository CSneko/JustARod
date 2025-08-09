package org.cneko.justarod.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import static org.cneko.justarod.Justarod.MODID;

public record JRSyncPayload(double power,boolean isFemale,boolean isMale,int pregnant,int syphilis) implements CustomPayload {
    public static final CustomPayload.Id<JRSyncPayload> ID = new CustomPayload.Id<>(Identifier.of(MODID, "sync"));
    public static final PacketCodec<RegistryByteBuf, JRSyncPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.DOUBLE,  JRSyncPayload::power,
            PacketCodecs.BOOL, JRSyncPayload::isFemale,
            PacketCodecs.BOOL, JRSyncPayload::isMale,
            PacketCodecs.INTEGER,  JRSyncPayload::pregnant,
            PacketCodecs.INTEGER,  JRSyncPayload::syphilis,
            JRSyncPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
