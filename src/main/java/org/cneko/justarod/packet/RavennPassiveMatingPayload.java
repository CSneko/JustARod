package org.cneko.justarod.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import static org.cneko.justarod.Justarod.MODID;

public record RavennPassiveMatingPayload (String uuid, String mateUuid) implements CustomPayload {
    public static final CustomPayload.Id<RavennPassiveMatingPayload> ID = new  CustomPayload.Id<>(Identifier.of(MODID, "ravenn_passive_mate"));
    public static final PacketCodec<RegistryByteBuf, RavennPassiveMatingPayload> CODEC;

    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }

    static {
        CODEC = PacketCodec.tuple(PacketCodecs.STRING, RavennPassiveMatingPayload::uuid, PacketCodecs.STRING, RavennPassiveMatingPayload::mateUuid, RavennPassiveMatingPayload::new);
    }
}
