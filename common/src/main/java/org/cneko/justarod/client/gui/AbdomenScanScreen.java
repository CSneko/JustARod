package org.cneko.justarod.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.cneko.justarod.ascaris.AscarisInfectionController;

/**
 * 实时动态腹部超声扫描界面（继承 MedicalScanScreen 的 0.5x~5.0x 缩放与拖拽平移框架）。
 * <ul>
 *   <li>0 期：平静肠道蠕动波；</li>
 *   <li>Ⅰ 期：漂浮发光虫卵 + 细小扭动幼虫；</li>
 *   <li>Ⅱ 期：向上游走的分节虫影 + 声呐脉冲；</li>
 *   <li>Ⅲ~Ⅳ 期：放大可清晰辨认蜷缩在腹中、随呼吸轻颤尾环、摆动触须并偶发眨眼回望探头的粉白蛔虫娘剪影！</li>
 * </ul>
 */
public class AbdomenScanScreen extends MedicalScanScreen {

    private static final int US_BG = 0xFF071620;
    private static final int US_GRID = 0x2638A8B8;
    private static final int US_WALL = 0x552E8898;
    private static final int US_PINK = 0xFFFF9EC4;
    private static final int US_WHITE = 0xFFFFF2F7;
    private static final int US_BERRY = 0xFFE6396E;
    private static final int US_BELL = 0xFFFFD866;

    public AbdomenScanScreen(LivingEntity targetEntity) {
        super(targetEntity);
    }

    @Override
    protected void renderOrganLayers(GuiGraphicsExtractor g, int x, int y, float delta) {
        int ticks = pregnantData.getAscaris();
        int stage = AscarisInfectionController.stageOf(ticks);
        float t = (targetEntity.tickCount + delta) / 20.0f;

        int pad = 10;
        int vx = x + pad;
        int vy = y + pad;
        int vw = this.imageWidth - pad * 2;
        int vh = this.imageHeight - pad * 2;
        int cx = x + this.imageWidth / 2;
        int cy = y + this.imageHeight / 2;

        // 1. 深色医用超声底幕 + 细网格 + 扇形深度环
        g.fill(vx, vy, vx + vw, vy + vh, US_BG);
        for (int gx = vx + 16; gx < vx + vw; gx += 20) {
            g.fill(gx, vy, gx + 1, vy + vh, US_GRID);
        }
        for (int gy = vy + 16; gy < vy + vh; gy += 20) {
            g.fill(vx, gy, vx + vw, gy + 1, US_GRID);
        }

        // 扇形超声边界与深度标尺
        drawCircleOutline(g, cx, vy + 18, 86, 0x3036C4D4);
        drawCircleOutline(g, cx, vy + 18, 138, 0x2836C4D4);
        drawCircleOutline(g, cx, vy + 18, 190, 0x2236C4D4);

        // 2. 动态腹腔与肠道壁轮廓（带自然蠕动波）
        renderIntestinalCavity(g, cx, cy, t, stage);

        // 3. 按感染/共生寄宿阶段绘制活体内容
        boolean hosted = isTargetHosted();
        if (hosted) {
            renderStage34CurledAscarisChan(g, cx, cy, t, false);
            // 温馨共生飘动小爱心
            for (int i = 0; i < 4; i++) {
                float hx = cx - 44 + i * 28 + Mth.sin(t * 2.2f + i) * 5f;
                float hy = cy - 42 + ((t * 14f + i * 21f) % 24f) * -1f + 16f;
                g.fill(Math.round(hx) - 2, Math.round(hy), Math.round(hx) + 2, Math.round(hy) + 3, 0xCCFF78B4);
                g.fill(Math.round(hx) - 1, Math.round(hy) - 1, Math.round(hx) + 1, Math.round(hy) + 4, 0xEEFFA8D0);
            }
        } else if (stage == 0) {
            renderHealthyPeristalsis(g, cx, cy, t);
        } else if (stage == 1) {
            renderStage1EggsAndLarvae(g, cx, cy, t);
        } else if (stage == 2) {
            renderStage2MigratingWorms(g, cx, cy, t);
        } else {
            renderStage34CurledAscarisChan(g, cx, cy, t, stage >= 4);
        }

        // 4. 实时扫频声呐光束（从探头原点左右往复扫过）
        float sweepAngle = Mth.sin(t * 1.45f) * 0.62f;
        int ox = cx;
        int oy = vy + 10;
        for (int dist = 12; dist < 218; dist += 3) {
            int sx = ox + Math.round(Mth.sin(sweepAngle) * dist);
            int sy = oy + Math.round(Mth.cos(sweepAngle) * dist);
            if (sx >= vx && sx < vx + vw && sy >= vy && sy < vy + vh) {
                int a = Math.max(18, 115 - dist / 2);
                g.fill(sx - 1, sy, sx + 2, sy + 2, (a << 24) | 0x62F0E8);
            }
        }
    }

    private void renderIntestinalCavity(GuiGraphicsExtractor g, int cx, int cy, float t, int stage) {
        int cavityColor = (stage >= 4) ? 0x3A601830 : 0x32164656;
        fillEllipse(g, cx, cy + 6, 86, 74, cavityColor);

        // 肠道蠕动环形轮廓
        int ringColor = (stage >= 4) ? 0xAAFF5A88 : US_WALL;
        for (int i = 0; i < 64; i++) {
            float ang = i * (Mth.TWO_PI / 64f);
            float pulse = Mth.sin(t * (stage >= 4 ? 5.0f : 2.0f) + ang * 4f) * (stage >= 4 ? 4.5f : 2.2f);
            int rx = cx + Math.round(Mth.cos(ang) * (82f + pulse));
            int ry = cy + 6 + Math.round(Mth.sin(ang) * (70f + pulse));
            g.fill(rx - 1, ry - 1, rx + 2, ry + 2, ringColor);
        }
    }

    private void renderHealthyPeristalsis(GuiGraphicsExtractor g, int cx, int cy, float t) {
        for (int row = -2; row <= 2; row++) {
            int baseY = cy + row * 18;
            for (int dx = -56; dx <= 56; dx += 4) {
                int wy = baseY + Math.round(Mth.sin(t * 2.2f + dx * 0.08f + row) * 4.0f);
                g.fill(cx + dx, wy, cx + dx + 3, wy + 2, 0x8055D8D0);
            }
        }
    }

    private void renderStage1EggsAndLarvae(GuiGraphicsExtractor g, int cx, int cy, float t) {
        // 漂浮发光粉白虫卵
        for (int i = 0; i < 14; i++) {
            float ang = i * 2.399f + t * 0.35f;
            float r = 18f + (i * 11 % 46) + Mth.sin(t * 1.8f + i) * 4f;
            int ex = cx + Math.round(Mth.cos(ang) * r);
            int ey = cy + 6 + Math.round(Mth.sin(ang) * r * 0.85f);
            g.fill(ex - 2, ey - 2, ex + 3, ey + 3, 0x70FF9EC4);
            g.fill(ex - 1, ey - 1, ex + 2, ey + 2, US_WHITE);
        }
        // 3 条扭动的小幼虫
        for (int w = 0; w < 3; w++) {
            float wx0 = cx - 32 + w * 32 + Mth.sin(t * 1.1f + w * 2f) * 14f;
            float wy0 = cy - 10 + w * 16 + Mth.cos(t * 1.4f + w) * 10f;
            for (int s = 0; s < 10; s++) {
                int px = Math.round(wx0 + s * 2.2f);
                int py = Math.round(wy0 + Mth.sin(t * 6.5f + s * 0.7f + w) * 3.0f);
                g.fill(px - 1, py - 1, px + 2, py + 2, (s % 2 == 0) ? US_PINK : US_WHITE);
            }
        }
    }

    private void renderStage2MigratingWorms(GuiGraphicsExtractor g, int cx, int cy, float t) {
        for (int idx = 0; idx < 4; idx++) {
            float phase = t * 1.3f + idx * 1.57f;
            float baseX = cx + Mth.sin(phase * 0.7f + idx) * 42f;
            float baseY = cy + 36 - ((t * 18f + idx * 34f) % 92f) + 12f;
            for (int s = 0; s < 18; s++) {
                float sx = baseX + Mth.sin(t * 5.2f + s * 0.45f + idx) * 7.5f;
                float sy = baseY + s * 2.4f;
                int rad = (s < 3 || s > 15) ? 1 : 2;
                int col = (s % 2 == 0) ? US_PINK : US_WHITE;
                g.fill(Math.round(sx) - rad, Math.round(sy) - rad, Math.round(sx) + rad + 1, Math.round(sy) + rad + 1, col);
            }
        }
    }

    /** Ⅲ~Ⅳ期招牌彩蛋：腹腔中央蜷缩着一只会呼吸、摇触须、眨莓红像素眼看你的粉白蛔虫娘剪影！ */
    private void renderStage34CurledAscarisChan(GuiGraphicsExtractor g, int cx, int cy, float t, boolean stage4) {
        float breath = Mth.sin(t * 2.6f);
        int bobY = Math.round(breath * 2.2f);
        int by = cy + 6 + bobY;

        // 1. 28 节盘绕粉白条纹长尾（环绕身体外圈并随呼吸波浪轻蠕，不遮挡刘海和触须）
        int tailSegs = 28;
        for (int i = 0; i < tailSegs; i++) {
            float frac = i / (float) (tailSegs - 1);
            float ang = -0.25f + frac * Mth.TWO_PI * 1.12f;
            float wave = Mth.sin(t * 3.8f - frac * 6.5f) * 3.0f;
            float rx = 52f - frac * 6f + wave;
            float ry = 44f - frac * 5f + wave * 0.8f;
            int tx = cx + Math.round(Mth.cos(ang) * rx);
            int ty = by + 6 + Math.round(Mth.sin(ang) * ry);
            int r = Math.max(2, Math.round(6.2f - frac * 4.0f));
            int col = (i % 2 == 0) ? US_PINK : US_WHITE;
            g.fill(tx - r - 1, ty - r - 1, tx + r + 1, ty + r + 1, 0x80B04070);
            g.fill(tx - r, ty - r, tx + r, ty + r, col);
        }

        // 2. 柔粉长发剪影 + 圆润脑袋 + 软萌小身子
        fillEllipse(g, cx, by - 4, 22, 24, 0xE8F698BE);
        // 两侧垂落发束
        g.fill(cx - 23, by - 8, cx - 15, by + 18, 0xD8EE86B2);
        g.fill(cx + 15, by - 8, cx + 23, by + 18, 0xD8EE86B2);

        // 头顶两根标志性弯曲触须（左右轻摆！）
        float antSway = Mth.sin(t * 3.2f) * 2.5f;
        for (int side = -1; side <= 1; side += 2) {
            for (int k = 0; k <= 12; k++) {
                float u = k / 12f;
                float a = Mth.PI * u;
                int ax = cx + side * (8 + Math.round(u * 12f + antSway * u));
                int ay = by - 24 - Math.round(Mth.sin(a) * 14f);
                g.fill(ax - 1, ay - 1, ax + 1, ay + 1, US_PINK);
            }
        }

        // 蕾丝发带高光 + 脸颊
        g.fill(cx - 14, by - 21, cx + 14, by - 18, US_WHITE);
        g.fill(cx - 13, by - 15, cx + 13, by + 2, 0xFFFFE4EC);

        // 莓红方形像素双眼 + 每 3.6 秒自然眨一次眼！
        float blinkCycle = t % 3.6f;
        boolean blinking = blinkCycle > 3.35f && blinkCycle < 3.52f;
        if (blinking) {
            g.fill(cx - 10, by - 7, cx - 4, by - 5, US_BERRY);
            g.fill(cx + 4, by - 7, cx + 10, by - 5, US_BERRY);
        } else {
            // 左眼
            g.fill(cx - 10, by - 10, cx - 4, by - 4, US_BERRY);
            g.fill(cx - 9, by - 9, cx - 7, by - 7, US_WHITE);
            // 右眼
            g.fill(cx + 4, by - 10, cx + 10, by - 4, US_BERRY);
            g.fill(cx + 5, by - 9, cx + 7, by - 7, US_WHITE);
        }

        // 小小弯弯闭嘴笑
        g.fill(cx - 2, by - 1, cx + 2, by, US_BERRY);

        // 脖子上的标志性小金铃（随呼吸微闪！）
        int bellY = by + 5;
        g.fill(cx - 3, bellY, cx + 3, bellY + 6, US_BELL);
        g.fill(cx - 1, bellY + 1, cx + 1, bellY + 3, US_WHITE);

        // Ⅳ期肠梗阻红色声呐警示框
        if (stage4) {
            int blinkAlpha = (Mth.sin(t * 8f) > 0) ? 0xDD : 0x55;
            int red = (blinkAlpha << 24) | 0xFF3A64;
            int bx0 = cx - 56, by0 = cy - 48, bx1 = cx + 56, by1 = cy + 60;
            // 四角医疗标定括弧
            g.fill(bx0, by0, bx0 + 14, by0 + 2, red);
            g.fill(bx0, by0, bx0 + 2, by0 + 14, red);
            g.fill(bx1 - 14, by0, bx1, by0 + 2, red);
            g.fill(bx1 - 2, by0, bx1, by0 + 14, red);
            g.fill(bx0, by1 - 2, bx0 + 14, by1, red);
            g.fill(bx0, by1 - 14, bx0 + 2, by1, red);
            g.fill(bx1 - 14, by1 - 2, bx1, by1, red);
            g.fill(bx1 - 2, by1 - 14, bx1, by1, red);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        // 在扫描仪外框上下绘制清晰的非缩放医学读数面板
        int ticks = pregnantData.getAscaris();
        int stage = AscarisInfectionController.stageOf(ticks);
        boolean hosted = isTargetHosted();
        String targetName = targetEntity.getDisplayName() != null ? targetEntity.getDisplayName().getString() : "目标";
        int titleColor = hosted ? 0xFFFFA8D4 : switch (stage) {
            case 0 -> 0xFF7AF2D8;
            case 1 -> 0xFFFFE09A;
            case 2 -> 0xFFFFB870;
            case 3 -> 0xFFFF9EC4;
            default -> 0xFFFF527A;
        };

        // 将读数条绘制在扫描仪视窗内部上下缘，确保在任何分辨率与 GUI 缩放下 100% 完整可见
        int topY = Math.max(6, this.y + 12);
        int botY = Math.min(this.height - 20, this.y + this.imageHeight - 26);

        Component header = Component.literal("腹部超声 · " + targetName + "  ["
                + (hosted ? "♡ 共生寄宿中" : AscarisInfectionController.stageLabel(stage)) + "]");
        context.fill(this.x + 12, topY, this.x + this.imageWidth - 12, topY + 14, 0xC8071622);
        context.fill(this.x + 12, topY + 13, this.x + this.imageWidth - 12, topY + 14, hosted ? 0x90FF8EC2 : 0x8050D0DC);
        context.centeredText(this.font, header, this.x + this.imageWidth / 2, topY + 3, titleColor);

        String sub;
        if (hosted) {
            if (targetEntity == net.minecraft.client.Minecraft.getInstance().player
                    && org.cneko.justarod.client.ascaris.AscarisClientState.hasHostedAscaris()) {
                sub = "暖腹共生中 · 饱食 " + org.cneko.justarod.client.ascaris.AscarisClientState.hostedHunger()
                        + "% · 亲密 " + org.cneko.justarod.client.ascaris.AscarisClientState.hostedAffection()
                        + "% · 小金铃可唤出";
            } else {
                sub = "检出已驯服粉白蛔虫娘安心寄宿中 · 滚轮缩放 " + String.format("%.1fx", this.zoom);
            }
        } else {
            sub = switch (stage) {
                case 0 -> "未见寄生虫影 · 滚轮缩放 " + String.format("%.1fx", this.zoom);
                case 1 -> "检出漂浮虫卵与微幼虫 · 早期服药可直清";
                case 2 -> "检出移行幼虫群 · 建议及时驱虫";
                case 3 -> "检出粉白成体！滚轮放大 · 宝塔糖可活体排出";
                default -> "⚠ 严重肠梗阻！请立即服宝塔糖或阿苯达唑";
            };
        }
        context.fill(this.x + 12, botY, this.x + this.imageWidth - 12, botY + 14, 0xC8071622);
        context.fill(this.x + 12, botY, this.x + this.imageWidth - 12, botY + 1, hosted ? 0x90FF8EC2 : 0x8050D0DC);
        context.centeredText(this.font, Component.literal(sub), this.x + this.imageWidth / 2, botY + 3, 0xFFD8F0F8);
    }

    private boolean isTargetHosted() {
        if (pregnantData.hasHostedAscaris()) return true;
        return targetEntity == net.minecraft.client.Minecraft.getInstance().player
                && org.cneko.justarod.client.ascaris.AscarisClientState.hasHostedAscaris();
    }

    private static void fillEllipse(GuiGraphicsExtractor g, int cx, int cy, int rx, int ry, int color) {
        for (int dy = -ry; dy <= ry; dy += 2) {
            float ratio = dy / (float) ry;
            int halfW = Math.round(rx * Mth.sqrt(Math.max(0f, 1f - ratio * ratio)));
            if (halfW > 0) {
                g.fill(cx - halfW, cy + dy, cx + halfW, cy + dy + 2, color);
            }
        }
    }

    private static void drawCircleOutline(GuiGraphicsExtractor g, int cx, int cy, int r, int color) {
        int steps = 48;
        for (int i = 0; i < steps; i++) {
            float a = i * (Mth.TWO_PI / steps);
            int px = cx + Math.round(Mth.cos(a) * r);
            int py = cy + Math.round(Mth.sin(a) * r);
            g.fill(px, py, px + 1, py + 1, color);
        }
    }
}
