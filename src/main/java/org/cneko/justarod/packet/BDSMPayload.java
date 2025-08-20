package org.cneko.justarod.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import static org.cneko.justarod.Justarod.MODID;

public record BDSMPayload(String uuid, boolean ballMouth, boolean electricShock,boolean bundled,boolean eyePatch,boolean earplug) implements CustomPayload{
    public static final CustomPayload.Id<BDSMPayload> ID = new CustomPayload.Id<>(Identifier.of(MODID, "bdsm"));
    public static final PacketCodec<RegistryByteBuf, BDSMPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, BDSMPayload::uuid,
            PacketCodecs.BOOL, BDSMPayload::ballMouth,
            PacketCodecs.BOOL, BDSMPayload::electricShock,
            PacketCodecs.BOOL, BDSMPayload::bundled,
            PacketCodecs.BOOL, BDSMPayload::eyePatch,
            PacketCodecs.BOOL, BDSMPayload::earplug,
            BDSMPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
