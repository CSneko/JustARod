package org.cneko.justarod.client.screen;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.entity.model.ParrotEntityModel;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.cneko.justarod.entity.SeeeeexNekoEntity;
import org.cneko.toneko.common.mod.client.api.ClientEntityPoseManager;
import org.cneko.toneko.common.mod.entities.INeko;
import org.cneko.toneko.common.mod.packets.interactives.FollowOwnerPayload;
import org.cneko.toneko.common.mod.packets.interactives.GiftItemPayload;
import org.cneko.toneko.common.mod.packets.interactives.NekoPosePayload;
import org.cneko.toneko.common.mod.packets.interactives.RideEntityPayload;
import org.cneko.toneko.common.mod.util.EntityUtil;
import org.cneko.toneko.fabric.client.screens.INekoScreen;
import org.cneko.toneko.fabric.client.screens.InteractionScreen;
import org.cneko.toneko.fabric.client.screens.NekoActionScreen;
import org.cneko.toneko.fabric.client.screens.NekoMateScreen;
import org.cneko.toneko.fabric.entities.NekoEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;


public class SeeeeexNekoInteractiveScreen extends InteractionScreen implements INekoScreen {
    private final SeeeeexNekoEntity neko;
    public SeeeeexNekoInteractiveScreen(@NotNull SeeeeexNekoEntity neko, @Nullable Screen lastScreen) {
        super(Text.empty(), lastScreen, (screen) -> getButtonBuilders(neko));
        this.neko = neko;
    }

    public static Map<String, ButtonWidget.Builder> getButtonBuilders(SeeeeexNekoEntity neko) {
        Map<String, ButtonWidget.Builder> builders = new LinkedHashMap();
        builders.put("screen.toneko.neko_entity_interactive.button.gift", ButtonWidget.builder(Text.translatable("screen.toneko.neko_entity_interactive.button.gift"), (btn) -> {
            ItemStack stack = MinecraftClient.getInstance().player.getMainHandStack();
            int slot = MinecraftClient.getInstance().player.getInventory().getSlotWithStack(stack);
            if (!stack.isEmpty()) {
                ClientPlayNetworking.send(new GiftItemPayload(neko.getUuid().toString(), slot));
            }

        }));
        builders.put("screen.toneko.neko_entity_interactive.button.action", ButtonWidget.builder(Text.translatable("screen.toneko.neko_entity_interactive.button.action"), (btn) -> {
            SeeeeexNekoActionScreen.open(neko);
        }));
        builders.put("screen.toneko.neko_entity_interactive.button.breed", ButtonWidget.builder(Text.translatable("screen.toneko.neko_entity_interactive.button.breed"), (btn) -> {
            if (neko.isBaby()) {
                int i = (new Random()).nextInt(13);
                MinecraftClient.getInstance().player.sendMessage(Text.translatable("message.toneko.neko.breed_fail_baby." + i));
            } else {
                List<INeko> entities = new ArrayList();
                Iterator var3 = EntityUtil.getLivingEntitiesInRange(neko, MinecraftClient.getInstance().player.getWorld(), (float)NekoEntity.DEFAULT_FIND_RANGE).iterator();

                while(var3.hasNext()) {
                    LivingEntity entity = (LivingEntity)var3.next();
                    if (entity instanceof INeko) {
                        INeko o = (INeko)entity;
                        if (o != neko) {
                            entities.add(o);
                        }
                    }
                }

                NekoMateScreen.open(neko, entities, (Screen)null);
            }
        }));
        return builders;
    }

    public static void open(SeeeeexNekoEntity neko) {
        MinecraftClient.getInstance().setScreen(new SeeeeexNekoInteractiveScreen(neko, MinecraftClient.getInstance().currentScreen));
    }


    @Override
    public SeeeeexNekoEntity getNeko() {
        return this.neko;
    }
}
