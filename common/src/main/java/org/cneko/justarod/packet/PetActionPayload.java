package org.cneko.justarod.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.UUID;

/** All interactive actions require a short-lived, per-player server-issued token. */
public record PetActionPayload(int action, int entityId, UUID token) implements CustomPacketPayload {
    public static final int OPEN = 0, ACCEPT = 1, SIT = 2, STAND = 3, FOLLOW = 4,
            STROKE = 5, FEED = 6, TRANSFER = 7, CONFIRM = 8, DECLINE = 9,
            CANCEL = 10, RELEASE = 11, NEKO_FOLLOW = 12;
    public static final UUID NONE = new UUID(0, 0);
    public static final Type<PetActionPayload> ID = new Type<>(Identifier.fromNamespaceAndPath("justarod", "pet_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PetActionPayload> CODEC = StreamCodec.ofMember(
            (p, b) -> { b.writeVarInt(p.action); b.writeVarInt(p.entityId); b.writeUUID(p.token); },
            b -> new PetActionPayload(b.readVarInt(), b.readVarInt(), b.readUUID()));
    @Override public Type<? extends CustomPacketPayload> type() { return ID; }
}
