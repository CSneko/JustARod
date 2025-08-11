package org.cneko.justarod.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

import static org.cneko.justarod.Justarod.MODID;

public record BallMouthPayload(String uuid,boolean status) implements CustomPayload{
    public static final CustomPayload.Id<BallMouthPayload> ID = new CustomPayload.Id<>(Identifier.of(MODID, "ball_mouth"));
    public static final PacketCodec<RegistryByteBuf,BallMouthPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, BallMouthPayload::uuid,
            PacketCodecs.BOOL, BallMouthPayload::status,
            BallMouthPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
