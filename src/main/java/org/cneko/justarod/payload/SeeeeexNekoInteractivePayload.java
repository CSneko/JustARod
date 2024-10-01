package org.cneko.justarod.payload;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SeeeeexNekoInteractivePayload(String uuid) implements CustomPayload {
    public static final CustomPayload.Id<SeeeeexNekoInteractivePayload> ID = new CustomPayload.Id<>(Identifier.of("justarod", "seeeeex_neko_interactive"));

    public static final PacketCodec<RegistryByteBuf, SeeeeexNekoInteractivePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, SeeeeexNekoInteractivePayload::uuid,
            SeeeeexNekoInteractivePayload::new
    );
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
