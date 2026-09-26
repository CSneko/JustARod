package org.cneko.justarod.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.cneko.justarod.Justarod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 服务端 → 客户端：体内实体列表（供 GUI「实体」页显示名字/占格/挣扎进度）。
 *
 * <p>物品格走原版容器槽位同步，实体列表则用本包一次性推送。
 */
public record SlimeEntityListPayload(int containerId, int tierLevel, int usedSpace, int totalSpace,
                                     int struggleThreshold, List<Entry> entries)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SlimeEntityListPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Justarod.MODID, "slime_entity_list"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SlimeEntityListPayload> CODEC =
            StreamCodec.ofMember(SlimeEntityListPayload::write, SlimeEntityListPayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(containerId);
        buf.writeVarInt(tierLevel);
        buf.writeVarInt(usedSpace);
        buf.writeVarInt(totalSpace);
        buf.writeVarInt(struggleThreshold);
        buf.writeVarInt(entries.size());
        for (Entry entry : entries) {
            buf.writeUUID(entry.uuid());
            ComponentSerialization.STREAM_CODEC.encode(buf, entry.name());
            buf.writeVarInt(entry.entityTypeId());
            buf.writeVarInt(entry.volume());
            buf.writeVarInt(entry.struggle());
            buf.writeBoolean(entry.struggleImmune());
        }
    }

    private static SlimeEntityListPayload read(RegistryFriendlyByteBuf buf) {
        int containerId = buf.readVarInt();
        int tierLevel = buf.readVarInt();
        int usedSpace = buf.readVarInt();
        int totalSpace = buf.readVarInt();
        int threshold = buf.readVarInt();
        int size = buf.readVarInt();
        List<Entry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            entries.add(new Entry(buf.readUUID(),
                    ComponentSerialization.STREAM_CODEC.decode(buf),
                    buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean()));
        }
        return new SlimeEntityListPayload(containerId, tierLevel, usedSpace, totalSpace, threshold, entries);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    /**
     * 单个被吞实体的展示信息。
     *
     * @param struggleImmune 是否免疫挣扎。主人为 true——主人钻进来是为了操控史莱姆，
     *                       不该被自动漂移甩出去（见 {@code SlimeStorage#struggleImmune}），
     *                       所以界面对他显示「主人」而不是一条永远不动的进度条。
     */
    public record Entry(UUID uuid, Component name, int entityTypeId, int volume, int struggle,
                        boolean struggleImmune) {
    }
}
