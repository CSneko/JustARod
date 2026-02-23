package org.cneko.justarod.client.event;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.cneko.justarod.client.gui.ScanType;
import org.cneko.justarod.client.gui.UterusScanScreen;
import org.cneko.justarod.client.screen.FrictionScreen;
import org.cneko.justarod.entity.BDSMable;
import org.cneko.justarod.entity.Pregnant;
import org.cneko.justarod.packet.*;
import org.cneko.justarod.property.JRProperty;
import org.cneko.justarod.property.JRRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static net.minecraft.client.MinecraftClient.getInstance;

public class JRClientNetworkingEvents {
    public static void init(){
        ClientPlayNetworking.registerGlobalReceiver(FrictionPayload.ID,((payload, context) -> {
            getInstance().setScreen(new FrictionScreen());
        }));
        ClientPlayNetworking.registerGlobalReceiver(JRSyncPayload.ID, (payload, context) -> {
            // 确保在主线程执行
            context.client().execute(() -> {
                if (context.client().player instanceof Pregnant clientPregnant) {
                    List<Object> values = payload.values();
                    List<JRProperty<?>> properties = JRRegistry.INSTANCE.getPROPERTIES();

                    // 自动将收到的数据塞回客户端玩家体内
                    for (int i = 0; i < properties.size(); i++) {
                        @SuppressWarnings("unchecked")
                        JRProperty<Object> prop = (JRProperty<Object>) properties.get(i);
                        Object value = values.get(i);

                        // 调用 Kotlin 中的 setter
                        prop.getSetter().invoke(clientPregnant, value);
                    }
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(BDSMPayload.ID, (payload, context) -> {
            UUID uuid = UUID.fromString(payload.uuid());
            PlayerEntity player = MinecraftClient.getInstance().player;

            Consumer<BDSMable> processBDSM = bdsmEntity -> {
                int ballMouth = payload.ballMouth() ? 2 : 0;
                int electricShock = payload.electricShock() ? 2 : 0;
                bdsmEntity.setBallMouth(ballMouth);
                bdsmEntity.setElectricShock(electricShock);
                bdsmEntity.setBundled(payload.bundled() ? 2 : 0);
                bdsmEntity.setEyePatch(payload.eyePatch() ?2 : 0);
                bdsmEntity.setEarplug(payload.earplug() ? 2 : 0);
                bdsmEntity.setHandcuffed(payload.handcuffed() ? 2 : 0);
                bdsmEntity.setShackled(payload.shackled() ? 2 : 0);
                bdsmEntity.setNoMatingPlz(payload.noMatingPlz() ? 2 : 0);
            };

            if (player.getUuid().equals(uuid)) {
                processBDSM.accept(player);
            }

            LivingEntity entity = findNearbyEntityByUuid(uuid, 10);
            if (entity instanceof BDSMable bm) {
                processBDSM.accept(bm);
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(MedicalPayload.ID,((payload, context) -> {
            UUID uuid = UUID.fromString(payload.uuid());
            PlayerEntity player = MinecraftClient.getInstance().player;

            Consumer<Pregnant> processMedical = medicalEntity -> {
                boolean amputated = payload.isAmputated();
                medicalEntity.setAmputated(amputated);
            };

            if (player.getUuid().equals(uuid)) {
                processMedical.accept(player);
            }

            LivingEntity entity = findNearbyEntityByUuid(uuid, 10);
            if (entity instanceof Pregnant pre) {
                processMedical.accept(pre);
            }
        }));

        ClientPlayNetworking.registerGlobalReceiver(XRayScanScreenPayload.ID,(payload,context)->{
            int id = payload.targetEntityId();
            ScanType type = payload.scanType();
            Entity entity = null;
            if (MinecraftClient.getInstance().world != null) {
                entity = MinecraftClient.getInstance().world.getEntityById(id);
            }
            if(entity instanceof LivingEntity le && entity instanceof Pregnant) {
                getInstance().execute(() -> {
                    if (type== ScanType.UTERUS) {
                        getInstance().setScreen(new UterusScanScreen(le));
                    }
                });
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
