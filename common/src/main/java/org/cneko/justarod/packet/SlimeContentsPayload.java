package org.cneko.justarod.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.cneko.justarod.Justarod;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务端 → 客户端：史莱姆**体内的物品图标**（只给渲染用）。
 *
 * <p>为什么不用实体同步数据：物品列表最长 724 格，塞不进 {@code SynchedEntityData}。
 * 为什么只发物品 id：体内渲染只要画个图标，不需要组件/数量，
 * 用 id 的话一包最多 25 个 varint，几十字节，随便发（大箱子/背包里的组件也不会被带出去）。
 *
 * <p>实体（被吞的乘客）不走这个包——它们是真实实体，由
 * {@code SlimeContentsLayer} 直接按乘客绘制。
 */
public record SlimeContentsPayload(int entityId, List<Integer> itemIds) implements CustomPacketPayload {

    /** 体内最多同步多少个物品图标（渲染层也只会用到这么多） */
    public static final int MAX_ITEMS = 25;

    public static final CustomPacketPayload.Type<SlimeContentsPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Justarod.MODID, "slime_contents"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SlimeContentsPayload> CODEC =
            StreamCodec.ofMember(SlimeContentsPayload::write, SlimeContentsPayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeVarInt(itemIds.size());
        for (int id : itemIds) {
            buf.writeVarInt(id);
        }
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    private static SlimeContentsPayload read(RegistryFriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        int size = Math.min(MAX_ITEMS, buf.readVarInt());
        List<Integer> ids = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ids.add(buf.readVarInt());
        }
        return new SlimeContentsPayload(entityId, ids);
    }
}
