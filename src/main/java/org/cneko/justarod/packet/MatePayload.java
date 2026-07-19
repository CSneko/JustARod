package org.cneko.justarod.packet;

import static org.cneko.justarod.Justarod.MODID;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// 对方向你发送了一个交配请求，你可以接受或接受
// 不准拒绝！草死你喵！草死你喵！草死你喵！草死你喵！草死你喵！
public record MatePayload(String nekoUuid, double amount, int time) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MatePayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "mate"));
    public static final StreamCodec<RegistryFriendlyByteBuf,MatePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, MatePayload::nekoUuid,
            ByteBufCodecs.DOUBLE, MatePayload::amount,
            ByteBufCodecs.INT, MatePayload::time,
            MatePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
