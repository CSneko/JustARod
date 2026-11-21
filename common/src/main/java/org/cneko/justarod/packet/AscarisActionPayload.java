package org.cneko.justarod.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.cneko.justarod.Justarod;

/**
 * C2S actions sent by a player currently wrapped by an Ascaris-chan.
 */
public record AscarisActionPayload(int action, int entityId) implements CustomPacketPayload {
    public static final int ACTION_STRUGGLE = 0;
    public static final int ACTION_RELEASE = 1;
    public static final int ACTION_CHEST_HOP = 2;

    public static final Type<AscarisActionPayload> ID =
            new Type<>(Identifier.fromNamespaceAndPath(Justarod.MODID, "ascaris_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AscarisActionPayload> CODEC = StreamCodec.ofMember(
            (p, b) -> {
                b.writeVarInt(p.action);
                b.writeVarInt(p.entityId);
            },
            b -> new AscarisActionPayload(b.readVarInt(), b.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
