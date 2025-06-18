package org.cneko.justarod.client.screen;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.cneko.justarod.entity.LoliNekoEntity;
import org.cneko.justarod.entity.Sexual;
import org.cneko.justarod.packet.PassiveMatingPayload;
import org.cneko.toneko.common.mod.client.screens.InteractionScreen;
import org.cneko.toneko.common.mod.client.screens.NekoScreenBuilder;
import org.cneko.toneko.common.mod.client.screens.NekoScreenBuilder.ButtonFactory;
import org.cneko.toneko.common.mod.client.screens.NekoScreenBuilder.TooltipFactory;
import org.cneko.toneko.common.mod.client.screens.factories.ButtonFactories;
import org.cneko.toneko.common.mod.client.screens.factories.ScreenBuilders;
import org.cneko.toneko.common.mod.packets.interactives.NekoMatePayload;
import java.util.Random;

import static net.minecraft.client.MinecraftClient.getInstance;

public class JRScreenBuilders {
    public static final NekoScreenBuilder SEEEEEX_NEKO_INTERACTIVE_SCREEN = ScreenBuilders.COMMON_START.clone()
            .addButton(ButtonFactories.CHAT_BUTTON).
            addButton(ButtonFactories.GIFT_BUTTON).
            addButton(ButtonFactories.ACTION_BUTTON).
            addButton(JRButtonFactories.SEEEEEX_NEKO_BREED_BUTTON);
    public static final NekoScreenBuilder SEEEEEX_NEKO_BREED_SCREEN = ScreenBuilders.COMMON_TOOLTIP.clone()
            .setStartY(5)
            .addTooltip(JRTooltipFactories.SEEEEEX_NEKO_SEXUAL_DESIRE_TOOLTIP)
            .addTooltip(JRTooltipFactories.SEEEEEX_NEKO_COMPLETE_PRECESS_TOOLTIP)
            .addTooltip(JRTooltipFactories.SEEEEEX_NEKO_AGE_LIMIT_TOOLTIP)
            .addButton(JRButtonFactories.SEEEEEX_NEKO_ATTACKING_BUTTON)
            .addButton(JRButtonFactories.SEEEEEX_NEKO_RECEIVING_BUTTON);
    public static final NekoScreenBuilder LOLI_NEKO_INTERACTIVE_SCREEN = ScreenBuilders.COMMON_TOOLTIP.clone()
            .addTooltip(JRTooltipFactories.LOLI_NEKO_SHOWING_AGE)
            .addButton(ButtonFactories.CHAT_BUTTON)
            .addButton(ButtonFactories.GIFT_BUTTON)
            .addButton(ButtonFactories.ACTION_BUTTON)
            .addButton(ButtonFactories.BREED_BUTTON);

    public static final class JRButtonFactories {
        public static final ButtonFactory SEEEEEX_NEKO_BREED_BUTTON = screen -> ButtonWidget.builder(Text.translatable("screen.toneko.seeeeeex_neko_entity_interactive.button.breed"), (btn) -> {
            if (screen.getNeko() instanceof Sexual) {
                MinecraftClient.getInstance().setScreen(new QuestionScreen(Questions.randomQuestion(),
                        ()-> MinecraftClient.getInstance().setScreen(new InteractionScreen(Text.empty(), screen.getNeko(), screen.lastScreen, SEEEEEX_NEKO_BREED_SCREEN)),
                        ()-> getInstance().player.sendMessage(Text.of("§c这都答错了呢~ 杂鱼杂鱼♡~~")), Questions::randomQuestion));
            }
        });
        public static final ButtonFactory SEEEEEX_NEKO_ATTACKING_BUTTON = screen -> ButtonWidget.builder(Text.translatable("screen.toneko.neko_entity_interactive.button.attacking"), (btn) -> {
            // 设置为灰色
            if (getInstance().player.getPower() < 60) {
                btn.active = false;
                btn.setTooltip(Tooltip.of(Text.translatable("screen.toneko.neko_entity_interactive.button.attacking.fail"))); // 哼哼~ 不持久我都看不上呢
            }else {
                getInstance().setScreen(new MateScreen(screen.getNeko()));
            }
        });
        public static final ButtonFactory SEEEEEX_NEKO_RECEIVING_BUTTON = screen -> ButtonWidget.builder(Text.translatable("screen.toneko.neko_entity_interactive.button.receiving"), (btn) -> {
            PlayerEntity entity = MinecraftClient.getInstance().player;
            if (screen.getNeko().isBaby()) {
                int i = (new Random()).nextInt(13);
                MinecraftClient.getInstance().player.sendMessage(Text.translatable("message.toneko.neko.breed_fail_baby." + i)); // FBI! Open door!
            } else {
                ClientPlayNetworking.send(new PassiveMatingPayload(screen.getNeko().getEntity().getUuid().toString(), entity.getUuid().toString()));
                MinecraftClient.getInstance().setScreen(screen.lastScreen);
            }
        });
    }
    public static final class JRTooltipFactories{
        public static final TooltipFactory SEEEEEX_NEKO_SEXUAL_DESIRE_TOOLTIP = screen -> {
            if (screen.getNeko() instanceof Sexual neko){
                return Text.translatable("screen.justarod.seeeeex_neko_mating.tooltip.sexual_desire",neko.getSexualDesire());
            }
            return Text.translatable("screen.justarod.seeeeex_neko_mating.tooltip.sexual_desire",0);
        };
        public static final TooltipFactory SEEEEEX_NEKO_COMPLETE_PRECESS_TOOLTIP = screen -> {
            if (screen.getNeko() instanceof Sexual neko){
                return Text.translatable("screen.justarod.seeeeex_neko_mating.tooltip.enable_complete_process",neko.enableCompleteProcess()? Text.translatable("misc.toneko.is_or_not.is").getString() : Text.translatable("misc.toneko.is_or_not.not"));
            }
            return Text.translatable("screen.justarod.seeeeex_neko_mating.tooltip.enable_complete_process",Text.translatable("misc.toneko.is_or_not.not.not"));
        };
        public static final TooltipFactory SEEEEEX_NEKO_AGE_LIMIT_TOOLTIP = screen -> {
            if (screen.getNeko() instanceof Sexual neko){
                return Text.translatable("screen.justarod.seeeeex_neko_mating.tooltip.enable_age_limit",neko.enableAgeLimit()? Text.translatable("misc.toneko.is_or_not.is").getString() : Text.translatable("misc.toneko.is_or_not.not"));
            }
            return Text.translatable("screen.justarod.seeeeex_neko_mating.tooltip.enable_age_limit",Text.translatable("misc.toneko.is_or_not.not.not"));
        };
        public static final TooltipFactory LOLI_NEKO_SHOWING_AGE = screen -> {
            if (screen.getNeko() instanceof LoliNekoEntity neko){
                return Text.translatable("screen.justarod.loli_neko.tooltip.showing_age",neko.getShowingAge());
            }
            return Text.translatable("screen.justarod.loli_neko.tooltip.showing_age",18);
        };
    }
}
