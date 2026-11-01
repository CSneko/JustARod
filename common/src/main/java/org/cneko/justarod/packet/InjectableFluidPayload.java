package org.cneko.justarod.packet;

import static org.cneko.justarod.Justarod.MODID;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** 注入式末地烛喷入流体后，让客户端叠加对应颜色的滤镜。 */
public record InjectableFluidPayload(String fluid, int ticks) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<InjectableFluidPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MODID, "injectable_fluid"));

    public static final StreamCodec<RegistryFriendlyByteBuf, InjectableFluidPayload> CODEC = StreamCodec.ofMember(
            (payload, buf) -> {
                buf.writeUtf(payload.fluid());
                buf.writeVarInt(payload.ticks());
            },
            buf -> new InjectableFluidPayload(buf.readUtf(), buf.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
