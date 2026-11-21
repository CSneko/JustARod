package org.cneko.justarod.ascaris;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.cneko.justarod.body.BodyRegionData;
import org.cneko.justarod.body.BodyWorld;
import org.cneko.justarod.entity.JREntities;
import org.cneko.justarod.entity.Pregnant;
import org.cneko.justarod.sound.JRSounds;

import java.util.List;
import java.util.UUID;

/**
 * 轻量、可插拔的「体内维度 ↔ 蛔虫娘」联动桥。
 * 不改动体内维度地形，仅在访客进入已感染（≥Ⅱ期）宿主体内时按需现身 1 只与宿主病程绑定的蛔虫娘，并双向同步治愈状态。
 */
public final class BodyAscarisBridge {

    private static final int FLOOR_Y = 80;
    private static final DustParticleOptions PINK_DUST = new DustParticleOptions(0xFF9AC2, 1.0f);

    private BodyAscarisBridge() {
    }

    /** 访客成功进入宿主体内维度后调用：优先现身宿主体内共生寄宿的真·粉白蛔虫娘并触发领地护卫！ */
    public static void onVisitorEntered(ServerLevel bodyLevel, long slot, ServerPlayer host, ServerPlayer visitor) {
        net.minecraft.nbt.CompoundTag hosted = AscarisHostController.getHostedTag(host);
        int ticks = ((Pregnant) (Object) host).getAscaris();
        int stage = AscarisInfectionController.stageOf(ticks);
        if (hosted == null && stage < 2) {
            return;
        }

        AscarisEntity existing = findResidentInSlot(bodyLevel, slot, host.getUUID());
        if (existing != null) {
            if (hosted != null) {
                AscarisHostController.applySerializedState(existing, hosted, host);
                if (visitor != host) {
                    existing.playTransientPose(AscarisPoseState.THREATEN, 44);
                    existing.triggerThreatenStrike(false);
                    visitor.sendSystemMessage(Component.literal(
                            "§c§o「喂！不准随便钻进我主人的身体里！快滚出去啦！」§7（宿主寄宿的粉白蛔虫娘触发领地意识！）"));
                }
            } else {
                syncScaleToStage(existing, stage);
                if (!existing.isMovementLocked()) {
                    existing.playTransientPose(AscarisPoseState.WAVE, 56);
                    bodyLevel.playSound(null, existing.blockPosition(), JRSounds.ASCARIS_BELL, SoundSource.NEUTRAL, 0.75f, 1.15f);
                }
            }
            return;
        }

        int cx = BodyRegionData.centerX(slot);
        int cz = BodyRegionData.centerZ(slot);
        AscarisEntity resident = JREntities.ASCARIS.create(bodyLevel, EntitySpawnReason.EVENT);
        if (resident == null) return;

        // 刷在肠道/肛门落点偏内侧，便于访客一眼发现
        resident.setPos(cx + 10.5, FLOOR_Y + 1.0, cz + 0.5);
        resident.setBodyHostUuid(host.getUUID());
        if (hosted != null) {
            AscarisHostController.applySerializedState(resident, hosted, host);
            bodyLevel.addFreshEntity(resident);
            if (visitor != host) {
                resident.playTransientPose(AscarisPoseState.THREATEN, 44);
                resident.triggerThreatenStrike(false);
                visitor.sendSystemMessage(Component.literal(
                        "§c§o「这里是我主人的身体，外人不准擅自闯进来！」§7（住在 " + host.getName().getString() + " 体内的她立刻哈气甩尾护领地！）"));
            }
            return;
        }
        syncScaleToStage(resident, stage);
        resident.setPoseState(stage == 2 ? AscarisPoseState.PRONE : AscarisPoseState.WAVE);
        if (stage >= 3) {
            resident.playTransientPose(AscarisPoseState.WAVE, 56);
        }
        bodyLevel.addFreshEntity(resident);
        bodyLevel.playSound(null, resident.blockPosition(), JRSounds.ASCARIS_BELL, SoundSource.NEUTRAL, 0.8f, 1.1f);
        visitor.sendSystemMessage(Component.literal(
                "§d……深处传来清脆的小金铃声，一只粉白长尾的小家伙正住在 " + host.getName().getString() + " 的体内！"));
    }

    /** 访客退出体内维度时：若刚刚在体内收服了她，一并把她带出体外！ */
    public static void onVisitorExited(ServerLevel bodyLevel, long slot, ServerPlayer visitor, ServerLevel destLevel) {
        AABB box = regionBox(slot);
        List<AscarisEntity> tamedFollowers = bodyLevel.getEntitiesOfClass(AscarisEntity.class, box,
                a -> a.isAlive() && a.isTame() && a.isOwnedBy(visitor));
        for (AscarisEntity a : tamedFollowers) {
            a.setBodyHostUuid(null);
            a.teleportTo(destLevel, visitor.getX(), visitor.getY(), visitor.getZ(),
                    java.util.Set.of(), visitor.getYRot(), visitor.getXRot(), false);
        }
    }

    /** 体内蛔虫娘自身慢速同步：若宿主既无共生寄宿又已治愈，自动消散 */
    public static void tickResident(ServerLevel level, AscarisEntity resident, UUID hostUuid) {
        if (!level.dimension().equals(BodyWorld.DIMENSION)) {
            return;
        }
        ServerPlayer host = level.getServer().getPlayerList().getPlayer(hostUuid);
        if (host == null) return;
        boolean hasHosted = AscarisHostController.hasHostedAscaris(host);
        if (resident.isTame()) {
            // 宿主寄宿的真身镜像：若宿主已在外部把她唤出，则清理体内镜像避免双身
            if (!hasHosted) {
                level.sendParticles(PINK_DUST, resident.getX(), resident.getY() + 0.7, resident.getZ(), 10, 0.3, 0.3, 0.3, 0.03);
                resident.remove(Entity.RemovalReason.DISCARDED);
            }
            return;
        }
        int stage = AscarisInfectionController.stageOf(((Pregnant) (Object) host).getAscaris());
        if (stage < 2) {
            level.sendParticles(PINK_DUST, resident.getX(), resident.getY() + 0.7, resident.getZ(), 14, 0.4, 0.4, 0.4, 0.04);
            resident.remove(Entity.RemovalReason.DISCARDED);
        } else {
            syncScaleToStage(resident, stage);
        }
    }

    /** 宿主体外治愈、排出或唤出后，清理体内同源蛔虫娘（防刷/防分身） */
    public static void despawnForHost(MinecraftServer server, UUID hostUuid) {
        ServerLevel body = server.getLevel(BodyWorld.DIMENSION);
        if (body == null) return;
        Long slot = BodyRegionData.get(server).currentRegion(hostUuid);
        if (slot == null) return;
        AscarisEntity resident = findResidentInSlot(body, slot, hostUuid);
        if (resident != null) {
            body.sendParticles(PINK_DUST, resident.getX(), resident.getY() + 0.7, resident.getZ(), 14, 0.4, 0.4, 0.4, 0.04);
            resident.remove(Entity.RemovalReason.DISCARDED);
        }
    }

    /** 访客在体内安抚收服她后，双向清除宿主体外感染 */
    public static void onResidentTamedInBody(ServerLevel level, AscarisEntity resident, Player tamer, UUID hostUuid) {
        resident.setBodyHostUuid(null);
        ServerPlayer host = level.getServer().getPlayerList().getPlayer(hostUuid);
        if (host != null) {
            ((Pregnant) (Object) host).setAscaris(0);
            host.sendSystemMessage(Component.literal(
                    "§a" + tamer.getName().getString() + " 在你体内把粉白蛔虫娘安抚带走了，你的蛔虫感染瞬间痊愈！"));
        }
    }

    /** 访客在体内喂驱虫药，双向清除宿主体外感染 */
    public static void onResidentDewormedInBody(ServerLevel level, AscarisEntity resident, Player medic, UUID hostUuid, boolean harsh) {
        resident.setBodyHostUuid(null);
        ServerPlayer host = level.getServer().getPlayerList().getPlayer(hostUuid);
        if (host != null) {
            ((Pregnant) (Object) host).setAscaris(0);
            host.sendSystemMessage(Component.literal(
                    "§a" + medic.getName().getString() + " 在你体内完成了" + (harsh ? "阿苯达唑强力驱虫" : "宝塔糖温和驱虫") + "，你的腹痛消失了！"));
        }
    }

    private static void syncScaleToStage(AscarisEntity entity, int stage) {
        float targetScale = switch (stage) {
            case 2 -> 0.50f;
            case 3 -> 0.82f;
            default -> 1.0f;
        };
        entity.setVisualScale(targetScale);
    }

    private static AscarisEntity findResidentInSlot(ServerLevel bodyLevel, long slot, UUID hostUuid) {
        List<AscarisEntity> list = bodyLevel.getEntitiesOfClass(AscarisEntity.class, regionBox(slot),
                a -> a.isAlive() && hostUuid.equals(a.getBodyHostUuid()));
        return list.isEmpty() ? null : list.get(0);
    }

    private static AABB regionBox(long slot) {
        int cx = BodyRegionData.centerX(slot);
        int cz = BodyRegionData.centerZ(slot);
        return new AABB(cx - 48, FLOOR_Y - 4, cz - 48, cx + 48, FLOOR_Y + 24, cz + 48);
    }
}
