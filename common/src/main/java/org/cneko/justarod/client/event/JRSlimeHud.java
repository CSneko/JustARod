package org.cneko.justarod.client.event;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.cneko.justarod.Justarod;
import org.cneko.justarod.client.JRKeyBindings;
import org.cneko.justarod.client.slime.SlimeDevourClientState;
import org.cneko.justarod.entity.slime.TamedSlimeEntity;

/**
 * 骑着史莱姆时，屏幕右侧的技能提示。
 *
 * <p>两行：
 * <ul>
 *   <li>{@code 按 G 吞噬}——冷却中改显示 {@code 冷却 30s}，附近没有可吞目标时变灰
 *       （三种状态的判定都在 {@link SlimeDevourClientState} 里）；</li>
 *   <li>{@code 按 B 打开背包}。</li>
 * </ul>
 *
 * <p>挂载位置用 {@code attachElementAfter(SUBTITLES)}：这个位置属于「游戏内 HUD」层，
 * 打开暂停菜单 / 选项 / F3 调试屏时原版会整层不画，提示自动跟着消失，
 * 不需要自己判断 {@code screen != null}。若用 {@code addLast} 则画在最外层，
 * 会在调试屏和菜单上留一行浮字。
 */
public final class JRSlimeHud {

    /** 右边缘到面板的间距 */
    private static final int MARGIN = 6;
    /** 面板内边距 */
    private static final int PADDING_X = 5;
    private static final int PADDING_Y = 4;
    /** 两行文字的间距 */
    private static final int LINE_SPACING = 2;

    private static final int BACKGROUND = 0xB0101010;
    private static final int BORDER = 0x60FFFFFF;
    private static final int BAG_COLOR = 0xFFE8E8E8;
    private static final int TITLE_COLOR = 0xFFC8C8C8;

    private JRSlimeHud() {
    }

    public static void init() {
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.SUBTITLES,
                Identifier.fromNamespaceAndPath(Justarod.MODID, "slime_skill_hint"),
                (HudElement) (graphics, deltaTracker) -> render(graphics));
    }

    private static void render(GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) return;
        TamedSlimeEntity slime = SlimeDevourClientState.riddenSlime();
        if (slime == null) return;

        Font font = minecraft.font;
        String devourKey = keyName(JRKeyBindings.SLIME_DEVOUR_KEY);
        String bagKey = keyName(JRKeyBindings.SLIME_BAG_KEY);

        Component title = Component.translatable("justarod.slime.hud.title", slime.getTierLevel())
                .withStyle(ChatFormatting.GRAY);
        Component devourLine = SlimeDevourClientState.devourHint(devourKey);
        Component bagLine = Component.translatable("justarod.slime.bag.hint", bagKey);

        int titleWidth = font.width(title);
        int devourWidth = font.width(devourLine);
        int bagWidth = font.width(bagLine);
        int contentWidth = Math.max(titleWidth, Math.max(devourWidth, bagWidth));
        int lineHeight = font.lineHeight;
        int contentHeight = lineHeight * 3 + LINE_SPACING * 2;

        int panelWidth = contentWidth + PADDING_X * 2;
        int panelHeight = contentHeight + PADDING_Y * 2;
        int x1 = graphics.guiWidth() - MARGIN;
        int x0 = x1 - panelWidth;
        int y0 = Math.max(MARGIN, (graphics.guiHeight() - panelHeight) / 2);
        int y1 = y0 + panelHeight;

        graphics.fill(x0, y0, x1, y1, BACKGROUND);
        graphics.fill(x0, y0, x1, y0 + 1, BORDER);
        graphics.fill(x0, y1 - 1, x1, y1, BORDER);
        graphics.fill(x0, y0, x0 + 1, y1, BORDER);
        graphics.fill(x1 - 1, y0, x1, y1, BORDER);

        int y = y0 + PADDING_Y;
        graphics.text(font, title, x0 + PADDING_X, y, TITLE_COLOR);
        y += lineHeight + LINE_SPACING;
        graphics.text(font, devourLine, x0 + PADDING_X, y, SlimeDevourClientState.devourHintColor());
        y += lineHeight + LINE_SPACING;
        graphics.text(font, bagLine, x0 + PADDING_X, y, BAG_COLOR);
    }

    /** 按键的显示名（跟随玩家的按键绑定设置） */
    private static String keyName(KeyMapping mapping) {
        return mapping.getTranslatedKeyMessage().getString();
    }
}
