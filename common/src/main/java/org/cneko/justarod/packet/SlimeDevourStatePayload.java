package org.cneko.justarod.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.cneko.justarod.Justarod;

/**
 * 服务端 → 客户端：手动「吞噬」技能的状态，供屏幕右侧的提示使用。
 *
 * <p>只发「客户端自己算不出来」的两件事：
 * <ul>
 *   <li>{@code cooldownTicks}：冷却剩余 tick。每头史莱姆各自记账（存在实体上、随存档保存），
 *       而冷却不是同步实体数据，所以要走包；</li>
 *   <li>{@code targetCount}：附近真正会被吞掉的目标数。过滤规则（体积、剩余空间、
 *       视线锥体、友军判定）全在服务端，客户端不复制一份——否则两边迟早不一致，
 *       出现「提示亮着却吞不到」的怪事。</li>
 * </ul>
 *
 * <p>放大动画的进度**不在这里**：它已经通过 {@code TamedSlimeEntity} 的同步实体数据
 * （起始时刻 + 总时长 + 进行中标志）发给所有追踪者了，客户端渲染器直接读实体即可。
 *
 * <p>只在「玩家骑着自己的史莱姆」时由 {@code TamedSlimeEntity#tickDevourSync} 节流发送
 * （状态没变就不发），正常游玩时每 5 tick 一个十几字节的小包。
 */
public record SlimeDevourStatePayload(int slimeEntityId, int cooldownTicks, int targetCount)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SlimeDevourStatePayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Justarod.MODID, "slime_devour_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SlimeDevourStatePayload> CODEC =
            StreamCodec.ofMember(SlimeDevourStatePayload::write, SlimeDevourStatePayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(slimeEntityId);
        buf.writeVarInt(cooldownTicks);
        buf.writeVarInt(targetCount);
    }

    private static SlimeDevourStatePayload read(RegistryFriendlyByteBuf buf) {
        return new SlimeDevourStatePayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
