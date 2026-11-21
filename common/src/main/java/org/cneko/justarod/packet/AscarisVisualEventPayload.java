package org.cneko.justarod.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.cneko.justarod.Justarod;

/**
 * S2C visual & feedback events for Ascaris infection and Ascaris-chan interactions.
 */
public record AscarisVisualEventPayload(int eventType, int entityId, int param, int value) implements CustomPacketPayload {
    public static final int EVENT_HUNGER_NIBBLE = 0;
    public static final int EVENT_BELLY_RUMBLE = 1;
    public static final int EVENT_LOFFLER_COUGH = 2;
    public static final int EVENT_OBSTRUCTION_CRAMP = 3;
    public static final int EVENT_EXPULSION_START = 4;
    public static final int EVENT_EXPULSION_FINISH = 5;
    public static final int EVENT_WRAP_SYNC = 6;
    public static final int EVENT_YANDERE_TIGHTEN = 7;
    public static final int EVENT_HOST_SYNC = 8;

    public static final Type<AscarisVisualEventPayload> ID =
            new Type<>(Identifier.fromNamespaceAndPath(Justarod.MODID, "ascaris_visual_event"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AscarisVisualEventPayload> CODEC = StreamCodec.ofMember(
            (p, b) -> {
                b.writeVarInt(p.eventType);
                b.writeVarInt(p.entityId);
                b.writeVarInt(p.param);
                b.writeVarInt(p.value);
            },
            b -> new AscarisVisualEventPayload(b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
