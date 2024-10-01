package org.cneko.justarod.client.event;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.cneko.justarod.client.screen.SeeeeexNekoInteractiveScreen;
import org.cneko.justarod.entity.SeeeeexNekoEntity;
import org.cneko.justarod.payload.SeeeeexNekoInteractivePayload;
import org.cneko.toneko.fabric.client.screens.CrystalNekoInteractiveScreen;
import org.cneko.toneko.fabric.entities.NekoEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.UUID;

public class JRClientNetworkingEvents {
    public static void init(){
        ClientPlayNetworking.registerGlobalReceiver(SeeeeexNekoInteractivePayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                String uuid = payload.uuid();
                if (uuid != null && !uuid.isEmpty()) {
                    SeeeeexNekoEntity neko = findNearbySeeeeexNekoByUuid(UUID.fromString(uuid), NekoEntity.DEFAULT_FIND_RANGE);
                    SeeeeexNekoInteractiveScreen.open(neko);

                }

            });
        });
    }

    public static @Nullable SeeeeexNekoEntity findNearbySeeeeexNekoByUuid(UUID targetUuid, double range) {
        LivingEntity var4 = findNearbyEntityByUuid(targetUuid, range);
        if (var4 instanceof SeeeeexNekoEntity nekoEntity) {
            return nekoEntity;
        } else {
            return null;
        }
    }

    public static @Nullable LivingEntity findNearbyEntityByUuid(UUID targetUuid, double range) {
        PlayerEntity player = MinecraftClient.getInstance().player;
        Box box = new Box(player.getX() - range, player.getY() - range, player.getZ() - range, player.getX() + range, player.getY() + range, player.getZ() + range);
        World world = player.getWorld();
        Iterator var6 = world.getOtherEntities(player, box).iterator();

        Entity entity;
        do {
            if (!var6.hasNext()) {
                return null;
            }

            entity = (Entity)var6.next();
        } while(!entity.getUuid().equals(targetUuid));

        if (entity instanceof LivingEntity le) {
            return le;
        } else {
            return null;
        }
    }
}
