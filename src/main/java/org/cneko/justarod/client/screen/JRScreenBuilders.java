package org.cneko.justarod.client.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.cneko.justarod.entity.SeeeeexNekoEntity;
import org.cneko.justarod.entity.Sexual;
import org.cneko.toneko.common.mod.client.screens.InteractionScreen;
import org.cneko.toneko.common.mod.client.screens.NekoScreenBuilder;
import org.cneko.toneko.common.mod.client.screens.NekoScreenBuilder.ButtonFactory;
import org.cneko.toneko.common.mod.client.screens.NekoScreenBuilder.TooltipFactory;
import org.cneko.toneko.common.mod.client.screens.factories.ButtonFactories;
import org.cneko.toneko.common.mod.client.screens.factories.ScreenBuilders;

public class JRScreenBuilders {
    public static final NekoScreenBuilder SEEEEEX_NEKO_INTERACTIVE_SCREEN = ScreenBuilders.COMMON_START.clone()
            .addButton(ButtonFactories.CHAT_BUTTON).
            addButton(ButtonFactories.GIFT_BUTTON).
            addButton(ButtonFactories.ACTION_BUTTON).
            addButton(JRButtonFactories.SEEEEEX_NEKO_BREED_BUTTON);
    public static final NekoScreenBuilder SEEEEEX_NEKO_BREED_SCREEN = ScreenBuilders.COMMON_TOOLTIP.clone()
            .setStartY(5)
            .addTooltip(JRTooltipFactories.SEEEEEX_NEKO_SEXUAL_DESIRE_TOOLTIP)
            .addButton(ButtonFactories.BREED_BUTTON);

    public static final class JRButtonFactories{
        public static final ButtonFactory SEEEEEX_NEKO_BREED_BUTTON = screen -> ButtonWidget.builder(Text.translatable("screen.toneko.neko_entity_interactive.button.breed"),(btn)->{
            if (screen.getNeko() instanceof Sexual){
                MinecraftClient.getInstance().setScreen(new InteractionScreen(Text.empty(),screen.getNeko(), screen.lastScreen, SEEEEEX_NEKO_BREED_SCREEN));
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
    }
}
