package org.cneko.justarod.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import static org.cneko.justarod.Justarod.MODID;

public record BDSMPayload(String uuid, boolean ballMouth, boolean electricShock,boolean bundled,boolean eyePatch,boolean earplug,boolean handcuffed) implements CustomPayload{
    public static final CustomPayload.Id<BDSMPayload> ID = new CustomPayload.Id<>(Identifier.of(MODID, "bdsm"));

    public static final PacketCodec<RegistryByteBuf, BDSMPayload> CODEC = PacketCodec.of(
            // 编码（写入）
            (payload, buf) -> {
                buf.writeString(payload.uuid());

                int flags = 0;
                flags |= (payload.ballMouth() ? 1 : 0);
                flags |= (payload.electricShock() ? 1 : 0) << 1;
                flags |= (payload.bundled()       ? 1 : 0) << 2;
                flags |= (payload.eyePatch()      ? 1 : 0) << 3;
                flags |= (payload.earplug()       ? 1 : 0) << 4;
                flags |= (payload.handcuffed()    ? 1 : 0) << 5;

                buf.writeInt(flags);
            },
            // 解码（读取）
            buf -> {
                String uuid = buf.readString();
                int flags = buf.readInt();

                return new BDSMPayload(
                        uuid,
                        (flags & 1) == 1,
                        (flags >> 1 & 1) == 1,
                        (flags >> 2 & 1) == 1,
                        (flags >> 3 & 1) == 1,
                        (flags >> 4 & 1) == 1,
                        (flags >> 5 & 1) == 1
                );
            }
    );


    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
