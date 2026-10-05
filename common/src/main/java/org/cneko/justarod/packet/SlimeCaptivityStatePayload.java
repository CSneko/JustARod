package org.cneko.justarod.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.cneko.justarod.Justarod;

/** Sent only to the captive player; grace -1 means indefinite protection below level 5. */
public record SlimeCaptivityStatePayload(int slimeEntityId, int graceTicks, int digestionTicks,
                                       int blurRampTicks, float struggle) implements CustomPacketPayload {
    public static final Type<SlimeCaptivityStatePayload> ID = new Type<>(Identifier.fromNamespaceAndPath(Justarod.MODID, "slime_captivity"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SlimeCaptivityStatePayload> CODEC = StreamCodec.ofMember(
            SlimeCaptivityStatePayload::write, SlimeCaptivityStatePayload::read);
    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(slimeEntityId);
        buf.writeVarInt(graceTicks);
        buf.writeVarInt(digestionTicks);
        buf.writeVarInt(blurRampTicks);
        buf.writeFloat(struggle);
    }
    private static SlimeCaptivityStatePayload read(RegistryFriendlyByteBuf buf) {
        return new SlimeCaptivityStatePayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readFloat());
    }
    @Override public Type<? extends CustomPacketPayload> type() { return ID; }
}
