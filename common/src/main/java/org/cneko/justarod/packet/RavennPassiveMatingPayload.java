package org.cneko.justarod.packet;

import static org.cneko.justarod.Justarod.MODID;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RavennPassiveMatingPayload (String uuid, String mateUuid) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RavennPassiveMatingPayload> ID = new  CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "ravenn_passive_mate"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RavennPassiveMatingPayload> CODEC;

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    static {
        CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, RavennPassiveMatingPayload::uuid, ByteBufCodecs.STRING_UTF8, RavennPassiveMatingPayload::mateUuid, RavennPassiveMatingPayload::new);
    }
}
