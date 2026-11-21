package org.cneko.justarod.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** reaction: 0 state only, 1 fish gaze, 2 head pat. */
public record PetStatePayload(int playerId, int mode, int ownerId, int reaction) implements CustomPacketPayload {
    public static final Type<PetStatePayload> ID = new Type<>(Identifier.fromNamespaceAndPath("justarod", "pet_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PetStatePayload> CODEC = StreamCodec.ofMember(
            (p, b) -> { b.writeVarInt(p.playerId); b.writeVarInt(p.mode); b.writeVarInt(p.ownerId); b.writeVarInt(p.reaction); },
            b -> new PetStatePayload(b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt()));
    @Override public Type<? extends CustomPacketPayload> type() { return ID; }
}
