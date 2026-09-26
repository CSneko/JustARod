package org.cneko.justarod.event;

import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.cneko.justarod.config.JRConfig;
import org.cneko.justarod.entity.Pregnant;

/**
 * 睡眠相关事件：睡了一整夜到天亮时，疲劳值直接清零。
 * （原版睡觉会瞬间跳过夜晚，仅靠睡觉期间的逐 tick 恢复是清不完的）
 */
public class SleepEvents {
    public static void init() {
        EntitySleepEvents.STOP_SLEEPING.register((entity, pos) -> {
            if (!JRConfig.isStayUpLateEnabled()) return;
            if (!(entity instanceof Pregnant pregnant)) return;

            int fatigue = pregnant.getFatigue();
            if (fatigue <= 0) return;

            // 醒来时天已经亮了（自然睡到早晨，或睡过了整夜跳过）→ 疲劳清零
            // 26.x：DimensionType#natural 与 Level#isNight 移除，改用时钟判断
            long tod = entity.level().getOverworldClockTime() % 24000L;
            if (!entity.level().dimensionType().hasFixedTime() && !(tod >= 13000L && tod <= 23000L)) {
                pregnant.setFatigue(0);
                if (fatigue >= Pregnant.FATIGUE_STAGE_2 && entity instanceof ServerPlayer sp) {
                    sp.sendSystemMessage(Component.nullToEmpty("§a你睡了个好觉，感觉神清气爽！（疲劳已消除）"));
                }
            }
        });
    }
}
