package org.cneko.justarod.packet;

import static org.cneko.justarod.Justarod.MODID;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record BDSMPayload(String uuid, boolean ballMouth, boolean electricShock,boolean bundled,boolean eyePatch,boolean earplug,boolean handcuffed,boolean shackled,boolean noMatingPlz) implements CustomPacketPayload{
    public static final CustomPacketPayload.Type<BDSMPayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "bdsm"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BDSMPayload> CODEC = StreamCodec.ofMember(
            // 编码（写入）
            (payload, buf) -> {
                buf.writeUtf(payload.uuid());

                int flags = 0;
                flags |= (payload.ballMouth() ? 1 : 0);
                flags |= (payload.electricShock() ? 1 : 0) << 1;
                flags |= (payload.bundled()       ? 1 : 0) << 2;
                flags |= (payload.eyePatch()      ? 1 : 0) << 3;
                flags |= (payload.earplug()       ? 1 : 0) << 4;
                flags |= (payload.handcuffed()    ? 1 : 0) << 5;
                flags |= (payload.shackled()      ? 1 : 0) << 6;
                flags |= (payload.noMatingPlz()    ? 1 : 0) << 7;

                buf.writeInt(flags);
            },
            // 解码（读取）
            buf -> {
                String uuid = buf.readUtf();
                int flags = buf.readInt();

                return new BDSMPayload(
                        uuid,
                        (flags & 1) == 1,
                        (flags >> 1 & 1) == 1,
                        (flags >> 2 & 1) == 1,
                        (flags >> 3 & 1) == 1,
                        (flags >> 4 & 1) == 1,
                        (flags >> 5 & 1) == 1,
                        (flags >> 6 & 1) == 1,
                        (flags >> 7 & 1) == 1
                );
            }
    );


    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
