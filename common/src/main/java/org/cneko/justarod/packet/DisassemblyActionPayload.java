package org.cneko.justarod.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record DisassemblyActionPayload(int action, int part) implements CustomPacketPayload {
    public static final int DETACH = 0, RECALL = 1, VIEW = 2, EXIT = 3, MINE = 4, RANDOM = 5, ATTACH = 6, PLACE = 7, DUAL = 8, REMOVE_EYES = 9;
    public static final Type<DisassemblyActionPayload> ID = new Type<>(Identifier.fromNamespaceAndPath("justarod", "disassembly_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DisassemblyActionPayload> CODEC = StreamCodec.ofMember(
        (p,b) -> { b.writeVarInt(p.action); b.writeVarInt(p.part); }, b -> new DisassemblyActionPayload(b.readVarInt(), b.readVarInt()));
    @Override public Type<? extends CustomPacketPayload> type() { return ID; }
}
