package org.cneko.justarod.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.UUID;

/** Client request only. The server resolves the target and validates every condition. */
public record BodyTravelPayload(UUID target, int route) implements CustomPacketPayload {
    public static final int EXIT = 4;
    public static final Type<BodyTravelPayload> ID = new Type<>(Identifier.fromNamespaceAndPath("justarod", "body_travel"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BodyTravelPayload> CODEC =
            StreamCodec.ofMember(BodyTravelPayload::write, BodyTravelPayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeLong(target.getMostSignificantBits());
        buf.writeLong(target.getLeastSignificantBits());
        buf.writeVarInt(route);
    }

    private static BodyTravelPayload read(RegistryFriendlyByteBuf buf) {
        return new BodyTravelPayload(new UUID(buf.readLong(), buf.readLong()), buf.readVarInt());
    }

    @Override public Type<? extends CustomPacketPayload> type() { return ID; }
}
