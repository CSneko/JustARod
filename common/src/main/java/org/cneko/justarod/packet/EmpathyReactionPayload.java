package org.cneko.justarod.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.cneko.justarod.Justarod;

/** -1 clears an interrupted reaction; the world timestamp keeps all observers in phase. */
public record EmpathyReactionPayload(int playerId, int reaction, long startedAt) implements CustomPacketPayload {
    public static final Type<EmpathyReactionPayload> ID = new Type<>(Identifier.fromNamespaceAndPath(Justarod.MODID, "empathy_reaction"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EmpathyReactionPayload> CODEC = StreamCodec.ofMember(
            (value, buf) -> {
                buf.writeVarInt(value.playerId());
                buf.writeVarInt(value.reaction());
                buf.writeLong(value.startedAt());
            }, buf -> new EmpathyReactionPayload(buf.readVarInt(), buf.readVarInt(), buf.readLong()));

    @Override public Type<? extends CustomPacketPayload> type() { return ID; }
}
