package org.cneko.justarod.sound;

import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import org.cneko.justarod.Justarod;

/**
 * 本模组自己的音效事件。
 *
 * <p>26.x 里不需要（也没有）独立的 SoundEvent 注册表：{@code SoundEvent} 只是
 * 「id + 传播距离」的载体，客户端拿到 id 后去 {@code assets/justarod/sounds.json} 查文件。
 * 所以这里用 {@code createVariableRangeEvent} 构造，服务端播给客户端时只发 id。
 *
 * <p>对应资源：{@code assets/justarod/sounds/slime_swallow.ogg}
 * （用 {@code .tmp/slime_sound/gen_swallow.py} 合成的湿黏吞咽声）。
 */
public final class JRSounds {

    /**
     * 手动「吞噬」技能的音效：黏糊糊的吞咽声。
     *
     * <p>刻意不用 {@code ENTITY_GENERIC_EAT} 那类「嚼东西」的声音——需求要的是
     * 「被一层胶质吸进去」的湿声（吸吮 + 咕嘟 + 黏泡破裂 + 收尾闷响）。
     */
    public static final SoundEvent SLIME_SWALLOW =
            SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Justarod.MODID, "slime_swallow"));

    public static final SoundEvent DOLL_SQUEEZE =
            SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Justarod.MODID, "doll_squeeze"));

    public static final SoundEvent PET_PURR = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Justarod.MODID, "pet_purr"));
    public static final SoundEvent PET_NEKO_VOICE = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Justarod.MODID, "pet_neko_voice"));

    // ===== 蛔虫感染 / 粉白蛔虫娘 =====
    public static final SoundEvent ASCARIS_BELL =
            SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Justarod.MODID, "ascaris_bell"));
    public static final SoundEvent ASCARIS_RUMBLE =
            SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Justarod.MODID, "ascaris_rumble"));
    public static final SoundEvent ASCARIS_SLITHER =
            SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Justarod.MODID, "ascaris_slither"));
    public static final SoundEvent ASCARIS_GIGGLE =
            SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Justarod.MODID, "ascaris_giggle"));
    public static final SoundEvent ASCARIS_HEARTBEAT =
            SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Justarod.MODID, "ascaris_heartbeat"));

    // ===== 粉白蛔虫娘语气词语音（11 组 / 26 变体） =====
    public static final SoundEvent ASCARIS_VOICE_GREET =
            SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Justarod.MODID, "ascaris_voice_greet"));
    public static final SoundEvent ASCARIS_VOICE_HAPPY =
            SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Justarod.MODID, "ascaris_voice_happy"));
    public static final SoundEvent ASCARIS_VOICE_CURIOUS =
            SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Justarod.MODID, "ascaris_voice_curious"));
    public static final SoundEvent ASCARIS_VOICE_HOP =
            SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Justarod.MODID, "ascaris_voice_hop"));
    public static final SoundEvent ASCARIS_VOICE_SLEEPY =
            SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Justarod.MODID, "ascaris_voice_sleepy"));
    public static final SoundEvent ASCARIS_VOICE_HISS =
            SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Justarod.MODID, "ascaris_voice_hiss"));
    public static final SoundEvent ASCARIS_VOICE_HURT =
            SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Justarod.MODID, "ascaris_voice_hurt"));
    public static final SoundEvent ASCARIS_VOICE_WHIMPER =
            SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Justarod.MODID, "ascaris_voice_whimper"));
    public static final SoundEvent ASCARIS_VOICE_YANDERE_GIGGLE =
            SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Justarod.MODID, "ascaris_voice_yandere_giggle"));
    public static final SoundEvent ASCARIS_VOICE_YANDERE_CLING =
            SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Justarod.MODID, "ascaris_voice_yandere_cling"));
    public static final SoundEvent ASCARIS_VOICE_JEALOUS =
            SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Justarod.MODID, "ascaris_voice_jealous"));

    private JRSounds() {
    }
}
