package org.cneko.justarod.client.event;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import org.cneko.justarod.ascaris.AscarisEntity;
import org.cneko.justarod.body.BodyWorld;
import org.cneko.justarod.client.JRKeyBindings;
import org.cneko.justarod.client.screen.BodyEntryScreen;
import org.cneko.justarod.item.JRItems;
import org.cneko.justarod.packet.BodyTravelPayload;
import org.cneko.justarod.scale.CuteScaleController;
import java.util.UUID;

public final class BodyClientEvents {
    private BodyClientEvents() { }

    public static void init() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (!world.isClientSide() || hand != InteractionHand.MAIN_HAND || entity == player) {
                return InteractionResult.PASS;
            }
            boolean holdingInfiltrator = player.getItemInHand(hand).is(JRItems.Companion.getBODY_INFILTRATOR())
                    || player.getOffhandItem().is(JRItems.Companion.getBODY_INFILTRATOR());

            if (entity instanceof Player host) {
                if (holdingInfiltrator) {
                    ClientPlayNetworking.send(new BodyTravelPayload(host.getUUID(), BodyWorld.ANYWHERE));
                    return InteractionResult.SUCCESS;
                }
                if (player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty()) {
                    Minecraft.getInstance().setScreen(new BodyEntryScreen(host.getUUID(), host.getName()));
                    return InteractionResult.SUCCESS;
                }
                return InteractionResult.PASS;
            }

            if (entity instanceof AscarisEntity ascaris) {
                if (holdingInfiltrator) {
                    ClientPlayNetworking.send(new BodyTravelPayload(ascaris.getUUID(), BodyWorld.ANYWHERE));
                    return InteractionResult.SUCCESS;
                }
                boolean ownedByPlayer = ascaris.isTame() && ascaris.isOwnedBy(player);
                boolean canSneakEnter = !ownedByPlayer || CuteScaleController.isSmallEnoughForChest(player);
                if (player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty() && canSneakEnter) {
                    Minecraft.getInstance().setScreen(new BodyEntryScreen(ascaris.getUUID(), ascaris.getDisplayName()));
                    return InteractionResult.SUCCESS;
                }
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
