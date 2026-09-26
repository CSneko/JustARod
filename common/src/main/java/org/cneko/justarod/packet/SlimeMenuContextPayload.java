package org.cneko.justarod.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.cneko.justarod.Justarod;

/**
 * 服务端 → 客户端：打开体内容器前的「上下文」。
 *
 * <p>为什么需要它：26.x 的 {@code MenuType} 工厂只拿到 {@code (containerId, Inventory)}，
 * 客户端构造菜单时**没有实体**，因此算不出容器槽位数。而
 * {@code ClientboundContainerSetSlotPacket} 是按槽位下标写进客户端的
 * {@code NonNullList}，两端槽位数不一致就会直接
 * {@code IndexOutOfBoundsException} 掉线。
 *
 * <p>所以服务端在 {@code openMenu} **之前**先发本包（同一连接、有序），
 * 客户端据此在构造菜单时就建出与服务端完全一致的槽位数。
 */
public record SlimeMenuContextPayload(int containerId, int slimeEntityId, int tierLevel)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SlimeMenuContextPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Justarod.MODID, "slime_menu_context"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SlimeMenuContextPayload> CODEC =
            StreamCodec.ofMember(SlimeMenuContextPayload::write, SlimeMenuContextPayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(containerId);
        buf.writeVarInt(slimeEntityId);
        buf.writeVarInt(tierLevel);
    }

    private static SlimeMenuContextPayload read(RegistryFriendlyByteBuf buf) {
        return new SlimeMenuContextPayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
