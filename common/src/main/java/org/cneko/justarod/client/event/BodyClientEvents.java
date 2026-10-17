package org.cneko.justarod.client.event;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import org.cneko.justarod.body.BodyWorld;
import org.cneko.justarod.client.JRKeyBindings;
import org.cneko.justarod.client.screen.BodyEntryScreen;
import org.cneko.justarod.item.JRItems;
import org.cneko.justarod.packet.BodyTravelPayload;
import java.util.UUID;

public final class BodyClientEvents {
    private BodyClientEvents() { }

    public static void init() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (!world.isClientSide() || hand != InteractionHand.MAIN_HAND || !(entity instanceof Player host)
                    || entity == player) return InteractionResult.PASS;
            if (player.getItemInHand(hand).is(JRItems.Companion.getBODY_INFILTRATOR())
                    || player.getOffhandItem().is(JRItems.Companion.getBODY_INFILTRATOR())) {
                ClientPlayNetworking.send(new BodyTravelPayload(host.getUUID(), BodyWorld.ANYWHERE));
                return InteractionResult.SUCCESS;
            }
            if (player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty()) {
                Minecraft.getInstance().setScreen(new BodyEntryScreen(host.getUUID(), host.getName()));
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (JRKeyBindings.BODY_EXIT_KEY.consumeClick()) {
                if (client.level != null && client.level.dimension().equals(BodyWorld.DIMENSION)) {
                    ClientPlayNetworking.send(new BodyTravelPayload(new UUID(0, 0), BodyTravelPayload.EXIT));
                }
            }
        });
    }
}
