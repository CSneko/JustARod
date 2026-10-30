package org.cneko.justarod.client.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * 注入式末地烛喷入后的全屏滤镜。
 *
 * <p>服务端只告诉客户端「哪种流体 + 持续多久」，颜色和淡出都在客户端算，
 * 因此不影响服务端逻辑，也不占实体/效果槽位。</p>
 */
public final class InjectionOverlay {
    private static final int FADE_TICKS = 60;

    /** 水：蓝色；岩浆：红橙；牛奶：白色。 */
    private static final int WATER_COLOR = 0x553FA9F5;
    private static final int LAVA_COLOR = 0x55FF4A00;
    private static final int MILK_COLOR = 0x55FFFFFF;

    private static int color;
    private static long endTick;

    private InjectionOverlay() {
    }

    public static void show(String fluid, int ticks) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        color = colorFor(fluid);
        endTick = level.getGameTime() + ticks;
    }

    /** 当前帧应该叠加的颜色（ARGB）；返回 0 表示不画。 */
    public static int currentColor() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || color == 0) {
            return 0;
        }
        long now = level.getGameTime();
        if (now >= endTick) {
            color = 0;
            return 0;
        }
        long left = endTick - now;
        float alphaScale = left < FADE_TICKS ? left / (float) FADE_TICKS : 1.0f;
        int alpha = (int) (((color >>> 24) & 0xFF) * alphaScale);
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private static int colorFor(String fluid) {
        return switch (fluid) {
            case "minecraft:water" -> WATER_COLOR;
            case "minecraft:lava" -> LAVA_COLOR;
            case "minecraft:milk" -> MILK_COLOR;
            default -> 0;
        };
    }
}
