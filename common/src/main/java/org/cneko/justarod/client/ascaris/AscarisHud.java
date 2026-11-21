package org.cneko.justarod.client.ascaris;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.cneko.justarod.Justarod;
import org.cneko.justarod.ascaris.AscarisEntity;
import org.cneko.justarod.ascaris.AscarisInfectionController;
import org.cneko.justarod.entity.Pregnant;

/**
 * 蛔虫感染 & 粉白长尾缠绕的高辨识度 HUD 视觉引擎：
 * 1. 活体饥饿条：Ⅱ期微光波动，Ⅲ~Ⅳ期粉白分节尾尖探出蠕动并在偷吃时扑咬爆粒子；
 * 2. 腹鸣粉白底边浪 / 移行咳嗽低热晕影 / Ⅳ期肠梗阻剧痛脉动；
 * 3. 3 秒驱虫高潮演出粉白光晕与进度条；
 * 4. 第一人称尾巴缠绕：粉白分节长尾环抱屏幕下沿 + 柔粉光晕 + 治愈/挣扎状态条。
 */
public final class AscarisHud {

    private static final int PINK_MAIN = 0xFF9AC2;
    private static final int PINK_DEEP = 0xE65C92;
    private static final int CREAM_WHITE = 0xFFF3F8;
    private static final int GOLD_BELL = 0xFFD966;

    private AscarisHud() {
    }

    public static void init() {
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.SUBTITLES,
                Identifier.fromNamespaceAndPath(Justarod.MODID, "ascaris_visual_hud"),
                (HudElement) (graphics, deltaTracker) -> render(graphics)
        );
    }

    private static void render(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui) return;

        int w = g.guiWidth();
        int h = g.guiHeight();
        float time = (player.tickCount + mc.getDeltaTracker().getGameTimeDeltaPartialTick(false)) / 20.0f;

        int ascarisTicks = (player instanceof Pregnant p) ? p.getAscaris() : 0;
        int stage = AscarisInfectionController.stageOf(ascarisTicks);
        boolean hosted = AscarisClientState.hasHostedAscaris()
                || (player instanceof Pregnant p && p.hasHostedAscaris());

        // 1. 感染大气晕影（咳嗽低热 / 腹鸣尾影 / Ⅳ期剧痛脉动）
        renderInfectionAtmosphere(g, mc.font, w, h, time, stage);

        // 2. 活体饥饿条（生存/冒险模式：野生感染Ⅱ期+ 或 已驯服共生寄宿在体内时均探出粉白小尾尖！）
        if (!player.isCreative() && !player.isSpectator() && (hosted || stage >= 2 || AscarisClientState.isNibbling())) {
            renderAliveHungerWorm(g, w, h, time, hosted ? 3 : Math.max(stage, 3));
        }

        // 3. 3 秒驱虫高潮演出覆盖层
        if (AscarisClientState.isExpelling()) {
            renderExpulsionOverlay(g, mc.font, w, h, time);
        }

        // 4. 双人尾巴缠绕第一人称视框（带平滑淡入/淡出）与极简按键提示
        if (AscarisClientState.hasWrapVisual()) {
            renderTailWrapOverlay(g, mc.font, w, h, time);
        }
    }

    private static void renderInfectionAtmosphere(GuiGraphicsExtractor g, Font font, int w, int h, float time, int stage) {
        // Ⅱ期咳嗽短暂低热泛红
        float cough = AscarisClientState.coughIntensity();
        if (cough > 0.01f) {
            int a = (int) (cough * 70);
            int top = (a << 24) | 0xFF6B8A;
            g.fillGradient(0, 0, w, h / 5, top, 0x00FF6B8A);
            g.fillGradient(0, h - h / 5, w, h, 0x00FF6B8A, top);
        }

        // Ⅲ期腹鸣：屏幕底沿滑过一条半透明粉白分节暗影波纹
        float rumble = AscarisClientState.rumbleIntensity();
        if (rumble > 0.01f) {
            int segCount = 22;
            float slide = (1.0f - rumble) * (w + 160) - 80;
            int alpha = (int) (Math.sin(rumble * Math.PI) * 115);
            if (alpha > 4) {
                for (int i = 0; i < segCount; i++) {
                    float sx = slide - i * 11f;
                    float sy = h - 14f + Mth.sin(time * 7f + i * 0.55f) * 6f;
                    int r = Math.max(2, 7 - i / 4);
                    int col = (alpha << 24) | ((i % 2 == 0) ? PINK_MAIN : CREAM_WHITE);
                    g.fill((int) sx - r, (int) sy - r / 2, (int) sx + r, (int) sy + r / 2, col);
                }
            }
        }

        // Ⅳ期重度肠梗阻：持续脉动粉红警示晕影 + 剧痛爆发红边
        if (stage >= 4) {
            float cramp = AscarisClientState.crampIntensity();
            float pulse = 0.45f + 0.55f * (0.5f + 0.5f * Mth.sin(time * 4.5f));
            int edgeAlpha = (int) ((32 + cramp * 95) * pulse);
            int edgeCol = (edgeAlpha << 24) | (cramp > 0.2f ? 0xE03060 : PINK_DEEP);
            int band = h / 6;
            g.fillGradient(0, 0, w, band, edgeCol, 0x00E03060);
            g.fillGradient(0, h - band, w, h, 0x00E03060, edgeCol);

            if (cramp > 0.15f) {
                Component warn = Component.literal("§c§l⚠ 肠梗阻绞痛 §r§d· 快服宝塔糖/阿苯达唑");
                int tw = font.width(warn);
                int bx = (w - tw) / 2 - 6;
                int by = 18;
                g.fill(bx, by - 3, bx + tw + 12, by + font.lineHeight + 3, 0xA8200814);
                g.text(font, warn, bx + 6, by, 0xFFFFDCE8);
            }
        }
    }

    /** 活体饥饿条：粉白分节小尾尖从原版鸡腿条右端探出，随正弦波轻蠕，偷吃时扑咬爆粒子 */
    private static void renderAliveHungerWorm(GuiGraphicsExtractor g, int w, int h, float time, int stage) {
        int barLeft = w / 2 + 10;
        int barRight = barLeft + 81;
        int barY = h - 39;

        // 饥饿条上的柔和粉白波浪微光（模拟鸡腿在轻微蠕动）
        for (int i = 0; i < 10; i++) {
            float wave = Mth.sin(time * (stage >= 3 ? 5.2f : 3.2f) + i * 0.7f);
            if (wave > 0.25f) {
                int a = (int) ((wave - 0.25f) * (stage >= 3 ? 85 : 48));
                int ix = barLeft + i * 8;
                int iy = barY + (int) (wave * 1.5f);
                g.fill(ix, iy, ix + 7, iy + 2, (a << 24) | ((i % 2 == 0) ? PINK_MAIN : CREAM_WHITE));
            }
        }

        if (stage < 3 && !AscarisClientState.isNibbling()) {
            return;
        }

        // Ⅲ~Ⅳ期：绘制从饥饿条右侧钻出的一条灵动粉白分节小尾巴！
        float nibble = AscarisClientState.nibbleProgress();
        boolean biting = AscarisClientState.isNibbling();
        // 偷吃时尾巴先探前扑咬到鸡腿上再缩回
        float lunge = biting ? Mth.sin(nibble * Mth.PI) * 18.0f : 0f;

        int segments = 11;
        for (int i = segments - 1; i >= 0; i--) {
            float frac = i / (float) (segments - 1); // 0 = 尾尖，1 = 根部
            float x = barRight + 4 - lunge * (1.0f - frac * 0.65f) + frac * 17.0f;
            float waveAmp = biting ? 3.2f : 2.1f;
            float y = barY + 4 + Mth.sin(time * (biting ? 12.0f : 5.5f) - frac * 3.8f) * waveAmp;
            int radius = (i == 0) ? 1 : (i < 3 ? 2 : 3);
            int rgb = (i % 2 == 0) ? PINK_MAIN : CREAM_WHITE;
            int outline = 0xCC8A385C;
            int fill = 0xFF000000 | rgb;
            int ix = Math.round(x);
            int iy = Math.round(y);
            g.fill(ix - radius - 1, iy - radius, ix + radius + 1, iy + radius, outline);
            g.fill(ix - radius, iy - radius + 1, ix + radius, iy + radius - 1, fill);
        }

        // 偷吃爆发瞬间：在扑咬点弹出粉白像素粒子 + 金色小叮铃闪点
        if (biting && nibble > 0.15f && nibble < 0.85f) {
            float burst = (nibble - 0.15f) / 0.70f;
            float cx = barRight - 10;
            float cy = barY + 3;
            int pa = (int) ((1.0f - burst) * 230);
            if (pa > 10) {
                for (int k = 0; k < 6; k++) {
                    float ang = k * (Mth.TWO_PI / 6f) + 0.4f;
                    int px = Math.round(cx + Mth.cos(ang) * (4f + burst * 14f));
                    int py = Math.round(cy + Mth.sin(ang) * (3f + burst * 11f) - burst * 4f);
                    int col = (pa << 24) | (k % 3 == 0 ? GOLD_BELL : (k % 2 == 0 ? PINK_MAIN : CREAM_WHITE));
                    g.fill(px - 1, py - 1, px + 1, py + 1, col);
                }
            }
        }
    }

    private static void renderExpulsionOverlay(GuiGraphicsExtractor g, Font font, int w, int h, float time) {
        float p = AscarisClientState.expulsionProgress();
        boolean pagoda = AscarisClientState.expulsionDrugType() == AscarisInfectionController.DRUG_PAGODA;
        int tint = pagoda ? PINK_MAIN : 0x88D8FF;
        int alpha = (int) ((45 + p * 125) * (0.78f + 0.22f * Mth.sin(time * 18f)));
        int edge = (alpha << 24) | tint;
        int clear = tint & 0x00FFFFFF;

        g.fillGradient(0, 0, w, h / 4, edge, clear);
        g.fillGradient(0, h - h / 4, w, h, clear, edge);

        // 中心下方进度条
        int barW = 156;
        int barH = 7;
        int x0 = (w - barW) / 2;
        int y0 = h - 76;
        Component label = Component.literal(pagoda
                ? "§d✦ 宝塔糖麻痹排虫中……（叮铃！）"
                : "§b✦ 阿苯达唑强力排虫中……");
        g.centeredText(font, label, w / 2, y0 - 12, 0xFFFFE6F0);
        g.fill(x0 - 1, y0 - 1, x0 + barW + 1, y0 + barH + 1, 0xD0381428);
        g.fill(x0, y0, x0 + barW, y0 + barH, 0x90140A10);
        int fillW = Math.round(barW * p);
        g.fill(x0, y0, x0 + fillW, y0 + barH, 0xFF000000 | tint);
        // 粉白条纹闪光点
        for (int sx = x0 + 6; sx < x0 + fillW - 4; sx += 12) {
            g.fill(sx, y0 + 1, sx + 4, y0 + barH - 1, 0xAAFFFFFF);
        }
    }

    /** 第一人称尾巴缠绕演出：粉白长尾分节环抱视口左右下角 + 平滑淡入淡出光晕 + 极简按键提示（无冗余文本面板） */
    private static void renderTailWrapOverlay(GuiGraphicsExtractor g, Font font, int w, int h, float time) {
        int mode = AscarisClientState.visualWrapMode();
        boolean affectionate = mode == AscarisEntity.WRAP_AFFECTIONATE;
        boolean yandere = mode == AscarisEntity.WRAP_YANDERE;
        float ramp = AscarisClientState.wrapVisualWeight();
        float tighten = AscarisClientState.tightenIntensity();
        float breath = 0.5f + 0.5f * Mth.sin(time * (yandere ? 4.2f : 2.6f));

        // 四周氛围光晕：亲密=樱花粉，寄生=莓粉，病娇=深玫暗紫脉冲+勒紧红闪
        int baseRgb = yandere ? (tighten > 0.1f ? 0xB80E3E : 0x8E1042) : (affectionate ? PINK_MAIN : PINK_DEEP);
        int glowAlpha = (int) (((yandere ? 98 : (affectionate ? 54 : 74)) + tighten * 62) * ramp * (0.82f + 0.18f * breath));
        glowAlpha = Math.min(205, glowAlpha);
        int glowColor = (glowAlpha << 24) | baseRgb;
        int clear = baseRgb & 0x00FFFFFF;
        g.fillGradient(0, 0, w, h / (yandere ? 4 : 5), glowColor, clear);
        g.fillGradient(0, h - h / (yandere ? 3 : 4), w, h, clear, glowColor);

        // 视口左右下角：两条对称环抱上卷的粉白条纹分节尾弧（随 ramp 平滑滑入与退场！）
        int segs = 16;
        float squeeze = yandere ? (1.14f + tighten * 0.18f) : 1.0f;
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < segs; i++) {
                float t = i / (float) (segs - 1);
                float sway = Mth.sin(time * (yandere ? 4.4f : 3.0f) + i * 0.42f) * (3.5f * (1f - t * 0.4f));
                float x = (side < 0)
                        ? (t * (w * 0.24f * squeeze) * ramp - 8f)
                        : (w - t * (w * 0.24f * squeeze) * ramp + 8f);
                float archY = h - 12f - Mth.sin(t * Mth.PI * 0.78f) * (h * 0.16f * squeeze) * ramp + sway;
                int thickness = Math.max(4, (int) ((14 - t * 6) * ramp));
                int alpha = (int) ((195 - t * 35) * ramp);
                if (alpha <= 4) continue;
                int evenCol = yandere ? 0xE8689C : PINK_MAIN;
                int oddCol = yandere ? 0xFFE2EE : CREAM_WHITE;
                int fill = (alpha << 24) | ((i % 2 == 0) ? evenCol : oddCol);
                int border = ((alpha / 2) << 24) | (yandere ? 0x6A1032 : 0xA83C68);
                int ix = Math.round(x);
                int iy = Math.round(archY);
                g.fill(ix - thickness - 1, iy - thickness / 2 - 1, ix + thickness + 1, iy + thickness / 2 + 1, border);
                g.fill(ix - thickness, iy - thickness / 2, ix + thickness, iy + thickness / 2, fill);
            }
        }

        // 仅在仍处于缠绕中时显示极简按键提示（去掉文本板，只提示按什么键挣脱/松开）
        if (!AscarisClientState.isWrapped()) {
            return;
        }
        int keyAlpha = (int) (235 * ramp);
        if (keyAlpha <= 12) return;
        int cy = h - 68;
        if (affectionate) {
            g.centeredText(font, Component.literal("按 [X] / [R] 松开"), w / 2, cy, (keyAlpha << 24) | 0xFFEAF2);
        } else {
            float struggle = AscarisClientState.wrapStruggle();
            g.centeredText(font, Component.literal("连按 [空格] / [R] 挣脱"), w / 2, cy - 8, (keyAlpha << 24) | (yandere ? 0xFFD4E4 : 0xFFE6F0));
            int barW = 108;
            int x0 = (w - barW) / 2;
            int bgA = (int) (130 * ramp);
            g.fill(x0, cy + 4, x0 + barW, cy + 8, (bgA << 24) | 0x16060C);
            g.fill(x0, cy + 4, x0 + Math.round(barW * struggle), cy + 8, (keyAlpha << 24) | (yandere ? 0xE83A76 : 0xFFA8CC));
        }
    }
}
