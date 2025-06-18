package org.cneko.justarod.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import static org.cneko.justarod.Justarod.MODID;

// 北朝了~
public record PassiveMatingPayload(String uuid, String mateUuid) implements CustomPayload {
    public static final CustomPayload.Id<PassiveMatingPayload> ID = new  CustomPayload.Id<>(Identifier.of(MODID, "neko_passive_mate"));
    public static final PacketCodec<RegistryByteBuf, PassiveMatingPayload> CODEC;

    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }

    static {
        CODEC = PacketCodec.tuple(PacketCodecs.STRING, PassiveMatingPayload::uuid, PacketCodecs.STRING, PassiveMatingPayload::mateUuid, PassiveMatingPayload::new);
    }
}
