package org.cneko.justarod.client.event;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.cneko.justarod.client.screen.FrictionScreen;
import org.cneko.justarod.entity.BallMouthable;
import org.cneko.justarod.packet.BallMouthPayload;
import org.cneko.justarod.packet.FrictionPayload;
import org.cneko.justarod.packet.JRSyncPayload;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.UUID;

import static net.minecraft.client.MinecraftClient.getInstance;

public class JRClientNetworkingEvents {
    public static void init(){
        ClientPlayNetworking.registerGlobalReceiver(FrictionPayload.ID,((payload, context) -> {
            getInstance().setScreen(new FrictionScreen());
        }));
        ClientPlayNetworking.registerGlobalReceiver(JRSyncPayload.ID, (payload, context) -> {
            PlayerEntity player = getInstance().player;
            player.setPower(payload.power());
            player.setFemale(payload.isFemale());
            player.setMale(payload.isMale());
            player.setPregnant(payload.pregnant());
            player.setSyphilis(payload.syphilis());
        });
        ClientPlayNetworking.registerGlobalReceiver(BallMouthPayload.ID,(payload,contextt)->{
           UUID uuid = UUID.fromString(payload.uuid());
           if (getInstance().player.getUuid().equals(uuid)){
               int time = 0;
               if (payload.status()){
                   time = 2;
               }
               getInstance().player.setBallMouth(time);
           }
           LivingEntity entity = findNearbyEntityByUuid(uuid,10);
           if (entity instanceof BallMouthable bm){
               int time = 0;
               if (payload.status()){
                   time = 2;
               }
               bm.setBallMouth(time);
           }
        });
    }

    public static @Nullable LivingEntity findNearbyEntityByUuid(UUID targetUuid, double range) {
        PlayerEntity player = MinecraftClient.getInstance().player;
        Box box = new Box(player.getX() - range, player.getY() - range, player.getZ() - range, player.getX() + range, player.getY() + range, player.getZ() + range);
        World world = player.getWorld();
        Iterator<Entity> var6 = world.getOtherEntities(player, box).iterator();

        Entity entity;
        do {
            if (!var6.hasNext()) {
                return null;
            }

            entity = var6.next();
        } while(!entity.getUuid().equals(targetUuid));

        if (entity instanceof LivingEntity le) {
            return le;
        } else {
            return null;
        }
    }
}
