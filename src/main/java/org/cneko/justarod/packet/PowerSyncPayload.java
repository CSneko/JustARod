package org.cneko.justarod.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import static org.cneko.justarod.Justarod.MODID;

public record PowerSyncPayload(double power) implements CustomPayload {
    public static final CustomPayload.Id<PowerSyncPayload> ID = new CustomPayload.Id<>(Identifier.of(MODID, "power_sync"));
    public static final PacketCodec<RegistryByteBuf,PowerSyncPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.DOUBLE,  PowerSyncPayload::power,
            PowerSyncPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
