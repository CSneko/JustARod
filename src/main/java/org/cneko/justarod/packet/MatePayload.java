package org.cneko.justarod.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import static org.cneko.justarod.Justarod.MODID;

// 对方向你发送了一个交配请求，你可以接受或接受
public record MatePayload(String nekoUuid, double amount, int time) implements CustomPayload {
    public static final CustomPayload.Id<MatePayload> ID = new CustomPayload.Id<>(Identifier.of(MODID, "mate"));
    public static final PacketCodec<RegistryByteBuf,MatePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, MatePayload::nekoUuid,
            PacketCodecs.DOUBLE, MatePayload::amount,
            PacketCodecs.INTEGER, MatePayload::time,
            MatePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
