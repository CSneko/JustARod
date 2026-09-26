package org.cneko.justarod.mixin.client;

// 26.x 迁移说明：
// net.minecraft.client.gui.Gui#render(GuiGraphics, DeltaTracker) 已随 GuiGraphics 的移除而改变签名，
// HUD 现通过 HudElementRegistry / GuiGraphicsExtractor 的提交式管线实现。
// 「熬夜疲劳眼皮/睡着黑屏」HUD 覆盖层暂被停用，后续可在新的 HUD 元素中重写。
/*
// package org.cneko.justarod.mixin.client;
// 
// import net.minecraft.Util;
// import net.minecraft.client.Minecraft;
// import net.minecraft.client.gui.Gui;
// import net.minecraft.client.gui.GuiGraphics;
// import net.minecraft.client.DeltaTracker;
// import net.minecraft.client.player.LocalPlayer;
// import net.minecraft.core.registries.BuiltInRegistries;
// import net.minecraft.network.chat.Component;
// import org.cneko.justarod.config.JRConfig;
// import org.cneko.justarod.effect.JREffects;
// import org.cneko.justarod.entity.Pregnant;
// import org.spongepowered.asm.mixin.Mixin;
// import org.spongepowered.asm.mixin.injection.At;
// import org.spongepowered.asm.mixin.injection.Inject;
// import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// 
// /**
//  * 熬夜的屏幕反馈：
//  * - 重度以上疲劳：眼皮打架（上下眼睑周期性合拢），极限时更频繁更深
//  * - 微睡/晕倒：屏幕几乎全黑，只有一句 "(睡着了……)"
//  * /
// @Mixin(Gui.class)
// public abstract class GuiMixin {
// 
//     @Inject(method = "render", at = @At("TAIL"))
//     private void justarod$renderFatigueOverlay(GuiGraphics graphics, DeltaTracker tickCounter, CallbackInfo ci) {
//         Minecraft mc = Minecraft.getInstance();
//         LocalPlayer player = mc.player;
//         if (player == null || mc.options.hideGui) return;
//         if (!JRConfig.isStayUpLateEnabled()) return;
//         if (!(player instanceof Pregnant pregnant)) return;
// 
//         int w = graphics.guiWidth();
//         int h = graphics.guiHeight();
// 
//         // 微睡/晕倒：眼前一黑
//         var faintHolder = JREffects.Companion.getFAINT_EFFECT();
//         if (player.hasEffect(faintHolder)) {
//             graphics.fill(0, 0, w, h, 0xE60A0612);
//             graphics.drawCenteredString(mc.font, Component.nullToEmpty("§d(睡着了……)"), w / 2, h / 2 - mc.font.lineHeight / 2, 0xFFFFFFFF);
//             return;
//         }
// 
//         int stage = Pregnant.fatigueStage(pregnant.getFatigue());
//         if (stage < 3) return;
// 
//         // 眼皮打架：越困眨得越频繁、闭得越深
//         long period = (stage >= 4) ? 3500L : 6500L;
//         double phase = (Util.getMillis() % period) / (double) period;
//         double closeAmount = 0;
//         if (phase < 0.22) {
//             closeAmount = Math.sin(phase / 0.22 * Math.PI);
//         }
//         double maxClose = (stage >= 4) ? 0.60 : 0.40;
//         int lidHeight = (int) (closeAmount * maxClose * h / 2);
//         if (lidHeight > 0) {
//             graphics.fill(0, 0, w, lidHeight, 0xD2000000);
//             graphics.fill(0, h - lidHeight, w, h, 0xD2000000);
//         }
//     }
// }
 */
public final class GuiMixin {
    private GuiMixin() {
    }
}
