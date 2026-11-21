package org.cneko.justarod.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.UUID;

public record PetMenuPayload(int kind, int npcId, String npcName, String candidateName, UUID token,
                             boolean absurd) implements CustomPacketPayload {
    public static final int OFFER = 0, OWNER = 1, TRANSFER = 2;
    public static final Type<PetMenuPayload> ID = new Type<>(Identifier.fromNamespaceAndPath("justarod", "pet_menu"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PetMenuPayload> CODEC = StreamCodec.ofMember(
            (p, b) -> {
                b.writeVarInt(p.kind); b.writeVarInt(p.npcId); b.writeUtf(p.npcName, 256);
                b.writeUtf(p.candidateName, 256); b.writeUUID(p.token); b.writeBoolean(p.absurd);
            }, b -> new PetMenuPayload(b.readVarInt(), b.readVarInt(), b.readUtf(256), b.readUtf(256), b.readUUID(), b.readBoolean()));
    @Override public Type<? extends CustomPacketPayload> type() { return ID; }
}
