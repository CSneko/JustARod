package org.cneko.justarod.client.event;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import org.cneko.justarod.Justarod;
import org.cneko.justarod.effect.JREffects;

/**
 * 26.x HUD：原 HudRenderCallback / GameRenderer 后处理已在 26.1.2 移除，
 * 这里用新的 HudElementRegistry + GuiGraphicsExtractor 恢复高潮粉红覆盖层。
 */
public class JRHudRenderEvent {

    public static void init() {
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(Justarod.MODID, "orgasm_overlay"),
                (HudElement) (guiGraphics, deltaTracker) -> {
                    Player player = Minecraft.getInstance().player;
                    if (player == null) {
                        return;
                    }
                    if (!player.hasEffect(JREffects.Companion.getORGASM_EFFECT())) {
                        return;
                    }
                    renderOrgasmOverlay(guiGraphics);
                }
        );
    }

    private static void renderOrgasmOverlay(GuiGraphicsExtractor g) {
        int width = g.guiWidth();
        int height = g.guiHeight();

        // 高潮时的粉红晕影：顶部/底部渐变 + 左右边缘常量填充
        int pink = 0xE6FF69B4; // 亮粉，带高 alpha
        int clear = 0x00FFFFFF; // 全透明

        float innerY1 = height * 0.25f;
        float innerY2 = height * 0.75f;

        g.fillGradient(0, 0, width, (int) innerY1, pink, clear);
        g.fillGradient(0, (int) innerY2, width, height, clear, pink);
        g.fill(0, 0, (int) (width * 0.25f), height, pink);
        g.fill((int) (width * 0.75f), 0, width, height, pink);
    }
}
