package org.cneko.justarod.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import static org.cneko.justarod.Justarod.MODID;

// 好烫
public record FullHeatPayload(String message) implements CustomPayload {
    public static final CustomPayload.Id<FullHeatPayload> ID = new CustomPayload.Id<>(Identifier.of(MODID, "full_heat"));
    public static final PacketCodec<RegistryByteBuf,FullHeatPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING,
            FullHeatPayload::message,
            FullHeatPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
