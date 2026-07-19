package org.cneko.justarod.client.screen;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.cneko.justarod.effect.JREffects;
import org.cneko.justarod.entity.LoliNekoEntity;
import org.cneko.justarod.entity.Sexual;
import org.cneko.justarod.packet.PassiveMatingPayload;
import org.cneko.justarod.packet.RavennPassiveMatingPayload;
import org.cneko.toneko.common.mod.client.screens.InteractionScreen;
import org.cneko.toneko.common.mod.client.screens.NekoScreenBuilder;
import org.cneko.toneko.common.mod.client.screens.NekoScreenBuilder.ButtonFactory;
import org.cneko.toneko.common.mod.client.screens.NekoScreenBuilder.TooltipFactory;
import org.cneko.toneko.common.mod.client.screens.NekoScreenRegistry;
import org.cneko.toneko.common.mod.client.screens.factories.ButtonFactories;
import org.cneko.toneko.common.mod.client.screens.factories.ScreenBuilders;
import org.cneko.toneko.common.mod.entities.RavennEntity;
import org.cneko.toneko.common.mod.entities.ToNekoEntities;
import org.cneko.toneko.common.mod.packets.interactives.NekoMatePayload;
import java.util.Random;

import static net.minecraft.client.Minecraft.getInstance;

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


    public static final  NekoScreenBuilder RAVENN_BREED_SCREEN = ScreenBuilders.COMMON_TOOLTIP.clone()
            .addButton(JRButtonFactories.RAVENN_BREED_ATTACKING_BUTTON)
            .addButton(JRButtonFactories.RAVENN_BREED_RECEIVING_BUTTON);

    public static final class JRButtonFactories {
        public static final ButtonFactory SEEEEEX_NEKO_BREED_BUTTON = screen -> Button.builder(Component.translatable("screen.toneko.seeeeeex_neko_entity_interactive.button.breed"), (btn) -> {
            if (screen.getNeko() instanceof Sexual) {
                if (getInstance().player.hasEffect(BuiltInRegistries.MOB_EFFECT.getHolder(JREffects.Companion.getKENJA_TIME_EFFECT()))){
                    getInstance().player.sendSystemMessage(Component.nullToEmpty("§c你现在还不想交配!"));
                }
                Minecraft.getInstance().setScreen(new InteractionScreen(Component.empty(), screen.getNeko(), screen.lastScreen, SEEEEEX_NEKO_BREED_SCREEN));
            }
        });
        public static final ButtonFactory SEEEEEX_NEKO_ATTACKING_BUTTON = screen -> Button.builder(Component.translatable("screen.toneko.neko_entity_interactive.button.attacking"), (btn) -> {
            // 设置为灰色
            if (getInstance().player.getPower() < 60) {
                btn.active = false;
                btn.setTooltip(Tooltip.create(Component.translatable("screen.toneko.neko_entity_interactive.button.attacking.fail"))); // 哼哼~ 不持久我都看不上呢
            }else {
                getInstance().setScreen(new MateScreen(screen.getNeko()));
            }
        });
        public static final ButtonFactory SEEEEEX_NEKO_RECEIVING_BUTTON = screen -> Button.builder(Component.translatable("screen.toneko.neko_entity_interactive.button.receiving"), (btn) -> {
            Player entity = Minecraft.getInstance().player;
            if (screen.getNeko().isBaby()) {
                int i = (new Random()).nextInt(13);
                Minecraft.getInstance().player.sendSystemMessage(Component.translatable("message.toneko.neko.breed_fail_baby." + i)); // FBI! Open door!
            } else {
                ClientPlayNetworking.send(new PassiveMatingPayload(screen.getNeko().getEntity().getUUID().toString(), entity.getUUID().toString()));
                Minecraft.getInstance().setScreen(screen.lastScreen);
            }
        });

        public static final ButtonFactory RAVENN_BREED_BUTTON = screen -> Button.builder(Component.translatable("screen.toneko.neko_entity_interactive.button.breed"), (btn) -> {
            if (screen.getNeko() instanceof RavennEntity) {
                Minecraft.getInstance().setScreen(new InteractionScreen(Component.empty(), screen.getNeko(), screen.lastScreen, RAVENN_BREED_SCREEN));
            }
        });
        public static final ButtonFactory RAVENN_BREED_ATTACKING_BUTTON = screen -> Button.builder(Component.translatable("screen.toneko.neko_entity_interactive.button.attacking"), (btn) -> {
            // 设置为灰色
            if (getInstance().player.getPower() < 60) {
                btn.active = false;
                btn.setTooltip(Tooltip.create(Component.translatable("screen.toneko.neko_entity_interactive.button.attacking.fail"))); // 哼哼~ 不持久我都看不上呢
            }
        });
        public static final ButtonFactory RAVENN_BREED_RECEIVING_BUTTON = screen -> Button.builder(Component.translatable("screen.toneko.neko_entity_interactive.button.receiving"), (btn) -> {
            ClientPlayNetworking.send(new RavennPassiveMatingPayload(screen.getNeko().getEntity().getUUID().toString(), screen.getNeko().getUUID().toString()));
        });
    }
    public static final class JRTooltipFactories{
        public static final TooltipFactory SEEEEEX_NEKO_SEXUAL_DESIRE_TOOLTIP = screen -> {
            if (screen.getNeko() instanceof Sexual neko){
                return Component.translatable("screen.justarod.seeeeex_neko_mating.tooltip.sexual_desire",neko.getSexualDesire());
            }
            return Component.translatable("screen.justarod.seeeeex_neko_mating.tooltip.sexual_desire",0);
        };
        public static final TooltipFactory SEEEEEX_NEKO_COMPLETE_PRECESS_TOOLTIP = screen -> {
            if (screen.getNeko() instanceof Sexual neko){
                return Component.translatable("screen.justarod.seeeeex_neko_mating.tooltip.enable_complete_process",neko.enableCompleteProcess()? Component.translatable("misc.toneko.is_or_not.is").getString() : Component.translatable("misc.toneko.is_or_not.not"));
            }
            return Component.translatable("screen.justarod.seeeeex_neko_mating.tooltip.enable_complete_process",Component.translatable("misc.toneko.is_or_not.not.not"));
        };
        public static final TooltipFactory SEEEEEX_NEKO_AGE_LIMIT_TOOLTIP = screen -> {
            if (screen.getNeko() instanceof Sexual neko){
                return Component.translatable("screen.justarod.seeeeex_neko_mating.tooltip.enable_age_limit",neko.enableAgeLimit()? Component.translatable("misc.toneko.is_or_not.is").getString() : Component.translatable("misc.toneko.is_or_not.not"));
            }
            return Component.translatable("screen.justarod.seeeeex_neko_mating.tooltip.enable_age_limit",Component.translatable("misc.toneko.is_or_not.not.not"));
        };
        public static final TooltipFactory LOLI_NEKO_SHOWING_AGE = screen -> {
            if (screen.getNeko() instanceof LoliNekoEntity neko){
                return Component.translatable("screen.justarod.loli_neko.tooltip.showing_age",neko.getShowingAge());
            }
            return Component.translatable("screen.justarod.loli_neko.tooltip.showing_age",18);
        };
    }
}
