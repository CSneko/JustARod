package org.cneko.justarod.client.event;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.cneko.justarod.client.screen.FrictionScreen;
import org.cneko.justarod.entity.BDSMable;
import org.cneko.justarod.entity.Pregnant;
import org.cneko.justarod.packet.BDSMPayload;
import org.cneko.justarod.packet.FrictionPayload;
import org.cneko.justarod.packet.JRSyncPayload;
import org.cneko.justarod.packet.MedicalPayload;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.UUID;
import java.util.function.Consumer;

import static net.minecraft.client.MinecraftClient.getInstance;

public class JRClientNetworkingEvents {
    public static void init(){
        ClientPlayNetworking.registerGlobalReceiver(FrictionPayload.ID,((payload, context) -> {
            getInstance().setScreen(new FrictionScreen());
        }));
        ClientPlayNetworking.registerGlobalReceiver(JRSyncPayload.ID, (payload, context) -> {
            // 获取客户端实例，确保在主线程执行
            context.client().execute(() -> {
                var player = context.player();

                if (player == null) return;

                // --- 1. 基础数值直接同步 ---
                player.setPower(payload.power());
                player.setPregnant(payload.pregnant());
                player.setMenstruation(payload.menstruation());
                player.setMenstruationComfort(payload.menstruationComfort());
                player.setBabyCount(payload.babyCount());
                player.setExcretion(payload.excretion());
                player.setUrination(payload.urination());
                player.setSyphilis(payload.syphilis());

                // --- 2. 复杂对象处理 ---
                // Optional 解包：如果有值则设置，没有则设为 null
                player.setChildrenType(payload.childrenType().orElse(null));

                // --- 3. 纯 Boolean 状态同步 ---
                player.setMale(payload.male());
                player.setFemale(payload.female());
                player.setSterilization(payload.sterilization());
                player.setEctopicPregnancy(payload.ectopicPregnancy());
                player.setHydatidiformMole(payload.hydatidiformMole());
                player.setImmune2Aids(payload.immune2Aids());
                player.setImmune2HPV(payload.immune2HPV());
                player.setHasUterus(payload.hasUterus());
                player.setPCOS(payload.isPCOS());
                player.setAmputated(payload.amputated());
                player.setOrchiectomy(payload.orchiectomy());

                // --- 4. Boolean 转 Int (False->0, True->22) ---
                // 针对 aids, hpv, birthControlling, ovarianCancer, breastCancer

                player.setAids(payload.aids() ? 22 : 0);
                player.setHPV(payload.hpv() ? 22 : 0);
                player.setBrithControlling(payload.brithControlling() ? 22 : 0);
                player.setOvarianCancer(payload.ovarianCancer() ? 22 : 0);
                player.setBreastCancer(payload.breastCancer() ? 22 : 0);
                player.setProstatitis(payload.prostatitis() ? 22 : 0);
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
