package org.cneko.justarod.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.cneko.justarod.Justarod;

/** A shot, rather than a held trigger, starts recoil on the shooter and other observers. */
public record EndRodGunShotPayload(int playerId, int hands, int interval) implements CustomPacketPayload {
    public static final Type<EndRodGunShotPayload> ID = new Type<>(Identifier.fromNamespaceAndPath(Justarod.MODID, "end_rod_gun_shot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EndRodGunShotPayload> CODEC = StreamCodec.ofMember(
            (payload, buf) -> {
                buf.writeVarInt(payload.playerId());
                buf.writeByte(payload.hands());
                buf.writeByte(payload.interval());
            }, buf -> new EndRodGunShotPayload(buf.readVarInt(), buf.readUnsignedByte(), buf.readUnsignedByte()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
