package org.cneko.justarod.packet;

import static org.cneko.justarod.Justarod.MODID;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MedicalPayload(String uuid, boolean isAmputated) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MedicalPayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "medical"));

    // 定义编码与解码逻辑
    public static final StreamCodec<RegistryFriendlyByteBuf, MedicalPayload> CODEC = StreamCodec.ofMember(
            // 编码（写入）
            (payload, buf) -> {
                buf.writeUtf(payload.uuid());
                buf.writeBoolean(payload.isAmputated());
            },
            // 解码（读取）
            buf -> {
                String uuid = buf.readUtf();
                boolean isAmputated = buf.readBoolean();
                return new MedicalPayload(uuid, isAmputated);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
