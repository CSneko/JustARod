package org.cneko.justarod.packet;

import static org.cneko.justarod.Justarod.MODID;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// 好烫
public record FullHeatPayload(String message) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<FullHeatPayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "full_heat"));
    public static final StreamCodec<RegistryFriendlyByteBuf,FullHeatPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            FullHeatPayload::message,
            FullHeatPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
