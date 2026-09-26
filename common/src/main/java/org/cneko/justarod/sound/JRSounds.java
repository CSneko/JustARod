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

    private JRSounds() {
    }
}
