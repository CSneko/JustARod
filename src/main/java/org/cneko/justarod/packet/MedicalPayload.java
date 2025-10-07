package org.cneko.justarod.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import static org.cneko.justarod.Justarod.MODID;

public record MedicalPayload(String uuid, boolean isAmputated) implements CustomPayload {
    public static final CustomPayload.Id<MedicalPayload> ID = new CustomPayload.Id<>(Identifier.of(MODID, "medical"));

    // 定义编码与解码逻辑
    public static final PacketCodec<RegistryByteBuf, MedicalPayload> CODEC = PacketCodec.of(
            // 编码（写入）
            (payload, buf) -> {
                buf.writeString(payload.uuid());
                buf.writeBoolean(payload.isAmputated());
            },
            // 解码（读取）
            buf -> {
                String uuid = buf.readString();
                boolean isAmputated = buf.readBoolean();
                return new MedicalPayload(uuid, isAmputated);
            }
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
