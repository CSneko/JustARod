package org.cneko.justarod.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.cneko.justarod.Justarod;

/**
 * S2C visual & feedback events for Ascaris infection and Ascaris-chan interactions.
 */
public record AscarisVisualEventPayload(int eventType, int entityId, int param, int value) implements CustomPacketPayload {
    public static final int EVENT_HUNGER_NIBBLE = 0;
    public static final int EVENT_BELLY_RUMBLE = 1;
    public static final int EVENT_LOFFLER_COUGH = 2;
    public static final int EVENT_OBSTRUCTION_CRAMP = 3;
    public static final int EVENT_EXPULSION_START = 4;
    public static final int EVENT_EXPULSION_FINISH = 5;
    public static final int EVENT_WRAP_SYNC = 6;
    public static final int EVENT_YANDERE_TIGHTEN = 7;
    public static final int EVENT_HOST_SYNC = 8;
    /** 体内神经视窗状态同步：param=1开启/0结束，value=打包状态(坐/爬/饱食/血量%) */
    public static final int EVENT_NEURAL_LINK = 9;
    /** 粉白蛔虫娘体内暖桃色体温呼吸晕影同步：param=0离开/1常规/2暖窝/3高执念，value=执念% */
    public static final int EVENT_BODY_WARMTH = 10;
    /** 粉白蛔虫娘体内硬核生理体征同步：entityId=打包生命体征(静水压/糖原/代谢废物/体温)，param=器官状态与药理，value=生殖/食糜/破口与警戒位；entityId=-1清除 */
    public static final int EVENT_PHYSIOLOGY_SYNC = 11;

    public static final Type<AscarisVisualEventPayload> ID =
            new Type<>(Identifier.fromNamespaceAndPath(Justarod.MODID, "ascaris_visual_event"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AscarisVisualEventPayload> CODEC = StreamCodec.ofMember(
            (p, b) -> {
                b.writeVarInt(p.eventType);
                b.writeVarInt(p.entityId);
                b.writeVarInt(p.param);
                b.writeVarInt(p.value);
            },
            b -> new AscarisVisualEventPayload(b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
