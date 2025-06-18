package org.cneko.justarod.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import static org.cneko.justarod.Justarod.MODID;

// 哼哼
public record FrictionPayload(String message) implements CustomPayload {
    public static final CustomPayload.Id<FrictionPayload> ID = new CustomPayload.Id<>(Identifier.of(MODID, "friction"));
    public static final PacketCodec<RegistryByteBuf,FrictionPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING,
            FrictionPayload::message,
            FrictionPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
