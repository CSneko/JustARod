package org.cneko.justarod.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.cneko.justarod.Justarod;

/**
 * 客户端 → 服务端：翻页 / 切换页签 / 取出体内实体 / 挣扎 / 喂催化物。
 *
 * <p>动作用 {@code action} 区分，避免为每个小操作单独开一个包。
 */
public record SlimeContainerActionPayload(int containerId, int action, int value, int extra,
                                          long uuidHigh, long uuidLow)
        implements CustomPacketPayload {

    public static final int ACTION_SET_PAGE = 0;
    public static final int ACTION_SET_TAB = 1;
    public static final int ACTION_RELEASE_ENTITY = 2;
    public static final int ACTION_STRUGGLE = 3;
    public static final int ACTION_FEED_CATALYST = 4;
    public static final int ACTION_SET_COMMAND = 5;
    /** 只请求一次实体列表刷新（GUI 定时刷新用），不改变任何状态 */
    public static final int ACTION_REFRESH = 6;
    /**
     * 体内按「背包键」：请求打开史莱姆背包。
     *
     * <p>为什么需要一个专门的动作：玩家钻进体内后，右键交互全部走「操控载具」那条路
     * （{@code Player#interactOn} 不会调到 {@code TamedSlimeEntity#mobInteract}），
     * 所以「体内开背包」只能靠按键 + 本包，由服务端校验「载具是不是你自己的史莱姆」。
     */
    public static final int ACTION_OPEN_BAG = 7;
    /** 手动「吞噬」技能：骑着史莱姆时按吞噬键 */
    public static final int ACTION_DEVOUR = 8;

    public static final CustomPacketPayload.Type<SlimeContainerActionPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Justarod.MODID, "slime_container_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SlimeContainerActionPayload> CODEC =
            StreamCodec.ofMember(SlimeContainerActionPayload::write, SlimeContainerActionPayload::read);

    public static SlimeContainerActionPayload of(int containerId, int action, int value, int extra) {
        return new SlimeContainerActionPayload(containerId, action, value, extra, 0L, 0L);
    }

    public static SlimeContainerActionPayload ofUuid(int containerId, int action, java.util.UUID uuid) {
        return new SlimeContainerActionPayload(containerId, action, 0, 0, uuid.getMostSignificantBits(),
                uuid.getLeastSignificantBits());
    }

    public java.util.UUID uuid() {
        return new java.util.UUID(uuidHigh, uuidLow);
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(containerId);
        buf.writeVarInt(action);
        buf.writeVarInt(value);
        buf.writeVarInt(extra);
        buf.writeLong(uuidHigh);
        buf.writeLong(uuidLow);
    }

    private static SlimeContainerActionPayload read(RegistryFriendlyByteBuf buf) {
        return new SlimeContainerActionPayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readVarInt(), buf.readLong(), buf.readLong());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
