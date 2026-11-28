package org.cneko.justarod.body;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.cneko.justarod.ascaris.AscarisEntity;
import org.cneko.justarod.ascaris.AscarisPoseState;
import org.cneko.justarod.block.JRBlocks;
import org.cneko.justarod.block.ascaris.AscarisChymeBlock;
import org.cneko.justarod.config.JRConfig;
import org.cneko.justarod.effect.JREffects;
import org.cneko.justarod.entity.JREntities;
import org.cneko.justarod.entity.Pregnant;
import org.cneko.justarod.packet.AscarisVisualEventPayload;
import org.cneko.justarod.sound.JRSounds;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 粉白蛔虫娘体内各解剖器官的实时交互、生机循环、温馨陪伴与反向寄生联动总控器：
 * <ul>
 *   <li>实时解剖分区检测、ActionBar 状态提示与暖桃色体温呼吸微晕同步；</li>
 *   <li>围咽神经环：右键安抚减执念 / 潜行右键开启神经视窗遥控外界 / 持粉白虫蜕触发组织温和自愈（不碰玩家建筑）；</li>
 *   <li>体温暖窝软垫：右键依偎回血降执念 / 潜行右键安心入眠跳过外界黑夜风雨并付与「暖腹余温」；</li>
 *   <li>病娇模式对抗反馈：破坏组织方块或攻击体腔免疫小细胞会令病娇执念上升（越折腾越兴奋、越难离开），必须温柔安抚；</li>
 *   <li>多风味甜食食糜、滚动蠕动波、活体体腔小细胞、H 形排泄管排毒、双管子宫卵囊再生、口腔/肛门自然滑出。</li>
 * </ul>
 */
public final class AscarisBodyInteractions {

    private static final DustParticleOptions PINK_DUST = new DustParticleOptions(0xFF9AC2, 0.95f);
    private static final DustParticleOptions PEARL_DUST = new DustParticleOptions(0xFFF2F7, 0.85f);
    private static final DustParticleOptions WARM_PEACH = new DustParticleOptions(0xFFD8B0, 0.9f);
    private static final DustParticleOptions NERVE_GOLD = new DustParticleOptions(0xFFE27A, 0.85f);
    private static final DustParticleOptions AQUA_DUST = new DustParticleOptions(0x98EEFF, 0.85f);
    private static final DustParticleOptions YANDERE_DUST = new DustParticleOptions(0xC2184E, 1.0f);

    private static final int[][] CHYME_ANCHORS = {
            {-6, -2}, {-6, 2}, {10, 2}, {10, -2}
    };
    private static final int[] EGG_SAC_XS = {-4, 0, 4, 8, 12};

    private static final double[][] COELOMOCYTE_HOMES = {
            {2.5, 2.2, 0.0},
            {-6.0, 2.4, -3.5},
            {-6.0, 2.4, 3.5},
            {11.0, 2.6, -2.5},
            {11.0, 2.6, 2.5}
    };

    private static final Map<UUID, Long> OVERLAY_SILENCE_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_PAIN_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_SOOTHE_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_SLEEP_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_REPRO_LINE_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_WHISPER_TICK = new ConcurrentHashMap<>();

    private AscarisBodyInteractions() {
    }

    public enum OrganZone {
        LIPS("品字形三唇 · 空手右键自然滑出"),
        NERVE_RING("围咽神经环 · 右键安抚 | 潜行右键神经视窗"),
        PHARYNX("Y 形三辐射肌肉咽 · 持甜食投喂 / 持药给药 / 空手刺激吞咽"),
        INTESTINE("笔直扁平肠道 · 潜行抗蠕动 | 绒毛可探查"),
        RECTUM("尾端直肠 · 肛门空手右键自然滑出"),
        EXCRETORY("H 形排泄管横桥 · 右键疏通排毒"),
        VAGINA("前 1/3 腹侧阴门 · 双管产道（空手右键可滑出）"),
        UTERUS("左右双管子宫 · 卵囊可采集"),
        OVARY("输卵管 · 盘绕丝状卵巢（空手右键刺激排卵）"),
        WARM_NOOK("体温暖窝 · 右键依偎 | 潜行右键安心小睡"),
        PSEUDOCOELOM("中央假体腔");

        final String label;

        OrganZone(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public static OrganZone detectZone(long slot, double worldX, double worldY, double worldZ) {
        int dx = (int) Math.floor(worldX) - BodyRegionData.centerX(slot);
        int dy = (int) Math.floor(worldY) - AscarisAnatomyBuilder.FLOOR_Y;
        int dz = (int) Math.floor(worldZ) - BodyRegionData.centerZ(slot);

        if (dy >= 5) {
            if (dx <= -40) return OrganZone.LIPS;
            if (dx <= -25) {
                return Math.abs(dx - AscarisAnatomyBuilder.NERVE_RING_X) <= 2 ? OrganZone.NERVE_RING : OrganZone.PHARYNX;
            }
            if (dx <= 32 && Math.abs(dz) <= 4) return OrganZone.INTESTINE;
            if (dx >= 33 && Math.abs(dz) <= 4) return OrganZone.RECTUM;
            if (dx >= 20) return OrganZone.OVARY;
            return OrganZone.PSEUDOCOELOM;
        }

        if (Math.abs(dx - AscarisAnatomyBuilder.EXCRETORY_BRIDGE_X) <= 2) return OrganZone.EXCRETORY;
        if (dx >= 1 && dx <= 4 && Math.abs(dz) <= 3) return OrganZone.WARM_NOOK;
        if (dx >= -18 && dx <= -7 && Math.abs(dz) <= 5) return OrganZone.VAGINA;
        if (dx >= -6 && dx <= 15 && Math.abs(dz) >= 6) return OrganZone.UTERUS;
        if (dx >= 16) return OrganZone.OVARY;
        return OrganZone.PSEUDOCOELOM;
    }

    public static void clearPlayerState(UUID uuid) {
        OVERLAY_SILENCE_UNTIL.remove(uuid);
        LAST_PAIN_TICK.remove(uuid);
        LAST_SOOTHE_TICK.remove(uuid);
        LAST_SLEEP_TICK.remove(uuid);
        LAST_REPRO_LINE_TICK.remove(uuid);
        LAST_WHISPER_TICK.remove(uuid);
    }

    public static void silenceZoneHudPublic(ServerPlayer player, int ticks) {
        silenceZoneHud(player, ticks);
    }

    private static void silenceZoneHud(ServerPlayer player, int ticks) {
        OVERLAY_SILENCE_UNTIL.put(player.getUUID(), ((ServerLevel) player.level()).getGameTime() + ticks);
    }

    private static AABB slotEntityBox(long slot) {
        int cx = BodyRegionData.centerX(slot);
        int cz = BodyRegionData.centerZ(slot);
        return new AABB(cx - 50, AscarisAnatomyBuilder.FLOOR_Y - 4, cz - 24,
                cx + 50, AscarisAnatomyBuilder.FLOOR_Y + 28, cz + 24);
    }

    /** 确保体内维度槽位里始终有 5 只自由飘游的粉白体腔免疫小细胞陪伴访客 */
    public static void ensureCoelomocytesPresent(ServerLevel bodyLevel, long slot) {
        List<AscarisCoelomocyteEntity> existing = bodyLevel.getEntitiesOfClass(
                AscarisCoelomocyteEntity.class, slotEntityBox(slot), e -> !e.isRemoved());
        if (existing.size() >= COELOMOCYTE_HOMES.length) {
            return;
        }
        int cx = BodyRegionData.centerX(slot);
        int cz = BodyRegionData.centerZ(slot);
        for (int i = existing.size(); i < COELOMOCYTE_HOMES.length; i++) {
            double[] offset = COELOMOCYTE_HOMES[i % COELOMOCYTE_HOMES.length];
            AscarisCoelomocyteEntity cell = JREntities.ASCARIS_COELOMOCYTE.create(bodyLevel, EntitySpawnReason.EVENT);
            if (cell == null) continue;
            cell.configureForSlot(slot,
                    cx + offset[0],
                    AscarisAnatomyBuilder.FLOOR_Y + offset[1],
                    cz + offset[2],
                    i * 1.35f);
            bodyLevel.addFreshEntity(cell);
        }
    }

    public static void alertCoelomocytesInSlot(ServerLevel bodyLevel, long slot, int ticks) {
        List<AscarisCoelomocyteEntity> cells = bodyLevel.getEntitiesOfClass(
                AscarisCoelomocyteEntity.class, slotEntityBox(slot), e -> !e.isRemoved());
        for (AscarisCoelomocyteEntity cell : cells) {
            cell.setAlertTicks(ticks);
        }
    }

    public static void calmCoelomocytesInSlot(ServerLevel bodyLevel, long slot) {
        List<AscarisCoelomocyteEntity> cells = bodyLevel.getEntitiesOfClass(
                AscarisCoelomocyteEntity.class, slotEntityBox(slot), e -> !e.isRemoved());
        for (AscarisCoelomocyteEntity cell : cells) {
            cell.calmDown();
        }
    }

    /** 每 tick 驱动：器官 HUD、体温呼吸微晕、滚动蠕动波、分器官活体微粒、体腔细胞生态、子宫卵囊再生、温馨悄悄话 */
    public static void tickVisitorOrgans(ServerPlayer visitor, @Nullable AscarisEntity ascaris, BodyRegionData.Visit visit) {
        ServerLevel bodyLevel = (ServerLevel) visitor.level();
        int ticks = visitor.tickCount;
        long gameTime = bodyLevel.getGameTime();
        long slot = visit.slot();
        OrganZone zone = detectZone(slot, visitor.getX(), visitor.getY(), visitor.getZ());
        int effObs = BodyWorld.getEffectiveObsession(ascaris, visit);
        int effHunger = BodyWorld.getEffectiveHunger(ascaris, visit);
        boolean yandereLocked = JRConfig.isAscarisYandereEnabled()
                && BodyWorld.isOwnedByVisitor(ascaris, visit, visitor)
                && effObs >= BodyWorld.YANDERE_LOCK_OBSESSION;

        // 0. 定期补全假体腔里飘游的粉白体腔小细胞
        if (ticks % 100 == 0) {
            ensureCoelomocytesPresent(bodyLevel, slot);
        }

        // 0.5 驱动硬核体内生理循环引擎（静水压、糖原吸收、滚动蠕动、排泄稳态、神经脉冲、生殖周期、双药药理）
        AscarisPhysiologyEngine.tickSlotPhysiology(bodyLevel, slot, ascaris, visitor, visit);
        AscarisPhysiologyState phys = BodyRegionData.get(bodyLevel.getServer()).physiologyFor(slot);

        // 1. 实时解剖部位 HUD + 生理响应式微晕同步
        if (ticks % 20 == 0) {
            int warmthMode;
            if (phys.isParalyzedByPiperazine()) {
                warmthMode = 4; // 哌嗪弛缓麻痹冷光微晕
            } else if (phys.isBlockedByAlbendazole() || phys.hasFever()) {
                warmthMode = 5; // 阿苯达唑毒发/炎症高热微晕
            } else if (phys.hasAcidosis()) {
                warmthMode = 6; // 代谢酸中毒青雾微晕
            } else if (yandereLocked) {
                warmthMode = 3;
            } else if (zone == OrganZone.WARM_NOOK) {
                warmthMode = 2;
            } else {
                warmthMode = 1;
            }
            ServerPlayNetworking.send(visitor, new AscarisVisualEventPayload(
                    AscarisVisualEventPayload.EVENT_BODY_WARMTH, visitor.getId(), warmthMode, effObs));

            Long silentUntil = OVERLAY_SILENCE_UNTIL.get(visitor.getUUID());
            if (silentUntil == null || gameTime >= silentUntil) {
                String obsPart = "";
                if (JRConfig.isAscarisYandereEnabled()) {
                    if (yandereLocked && phys.isSphincterRelaxed()) {
                        obsPart = " §7| §b执念 " + effObs + "% (麻痹松弛可出)";
                    } else {
                        obsPart = " §7| " + (yandereLocked ? "§c执念 " + effObs + "% (出口紧闭)" : "§a执念 " + effObs + "%");
                    }
                }
                String status = "§d" + zone.label() + " §7| §e饱食 " + effHunger + "%" + obsPart;
                visitor.sendOverlayMessage(Component.literal(status));
            }
        }

        // 2. 分器官专属活体微粒生态（神经火花 / 排泄气泡 / 暖窝桃光与爱心 / 卵囊孢子尘）
        if (ticks % 16 == 0) {
            spawnOrganAmbientParticles(bodyLevel, visitor, zone, slot, yandereLocked);
        }

        // 3. （肠道滚动蠕动波与食糜推进已由 AscarisPhysiologyEngine 实景物理驱动）

        // 4. 子宫 / 盘绕卵巢区域专属反应
        if ((zone == OrganZone.UTERUS || zone == OrganZone.OVARY) && ticks % 40 == 0) {
            Long prev = LAST_REPRO_LINE_TICK.get(visitor.getUUID());
            if (prev == null || gameTime - prev >= 700) {
                LAST_REPRO_LINE_TICK.put(visitor.getUUID(), gameTime);
                if (yandereLocked) {
                    visitor.sendSystemMessage(Component.literal(
                            "§c§o「这么喜欢待在我最深处的子宫和卵巢旁边吗♡ 要不要干脆永远住在这里呢……」"));
                } else {
                    visitor.sendSystemMessage(Component.literal(
                            "§d「咿呀？！那、那边是装满虫卵的子宫和卵巢啦，别乱碰呀 >~<」"));
                }
            }
        }

        // 5. 温馨「隔肚皮悄悄话」（每 ~50 秒克制轻语一句）
        if (ticks > 120 && ticks % 100 == 0) {
            Long prevW = LAST_WHISPER_TICK.get(visitor.getUUID());
            if (prevW == null || gameTime - prevW >= 1000) {
                LAST_WHISPER_TICK.put(visitor.getUUID(), gameTime);
                sendCozyBellyWhisper(bodyLevel.getServer(), visitor, zone, yandereLocked);
            }
        }

        // 6. 活体产卵生态：粉白蛔虫娘饱食度充足时，左右双管子宫缓慢再生新的蛔虫卵囊块
        if (ticks % 400 == 0) {
            if (effHunger >= 55 && tryRegenerateUterineEggSac(bodyLevel, slot)) {
                BodyWorld.adjustHostStats(bodyLevel.getServer(), visitor, visit, 0, -4, 0);
            }
        }
    }

    private static void spawnOrganAmbientParticles(ServerLevel bodyLevel, ServerPlayer visitor,
                                                   OrganZone zone, long slot, boolean yandere) {
        var rand = visitor.getRandom();
        double px = visitor.getX() + (rand.nextDouble() - 0.5) * 6.0;
        double py = visitor.getY() + 0.5 + rand.nextDouble() * 2.6;
        double pz = visitor.getZ() + (rand.nextDouble() - 0.5) * 6.0;

        switch (zone) {
            case NERVE_RING -> bodyLevel.sendParticles(NERVE_GOLD, px, py, pz, 4, 0.35, 0.35, 0.35, 0.02);
            case EXCRETORY -> {
                bodyLevel.sendParticles(AQUA_DUST, px, AscarisAnatomyBuilder.FLOOR_Y + 0.7, pz, 3, 0.4, 0.2, 0.4, 0.02);
                bodyLevel.sendParticles(ParticleTypes.BUBBLE_POP, px, AscarisAnatomyBuilder.FLOOR_Y + 0.9, pz, 2, 0.3, 0.25, 0.3, 0.02);
            }
            case WARM_NOOK -> {
                int cx = BodyRegionData.centerX(slot);
                int cz = BodyRegionData.centerZ(slot);
                bodyLevel.sendParticles(WARM_PEACH, cx + 2.5 + (rand.nextDouble() - 0.5) * 4.0,
                        AscarisAnatomyBuilder.FLOOR_Y + 1.4 + rand.nextDouble() * 1.6,
                        cz + (rand.nextDouble() - 0.5) * 4.5, 4, 0.3, 0.25, 0.3, 0.01);
                if (rand.nextInt(3) == 0) {
                    bodyLevel.sendParticles(ParticleTypes.HEART, cx + 2.5 + (rand.nextDouble() - 0.5) * 3.0,
                            AscarisAnatomyBuilder.FLOOR_Y + 2.0, cz + (rand.nextDouble() - 0.5) * 3.5,
                            1, 0.2, 0.2, 0.2, 0.01);
                }
            }
            case UTERUS, OVARY -> bodyLevel.sendParticles(PEARL_DUST, px, py, pz, 3, 0.4, 0.3, 0.4, 0.01);
            default -> bodyLevel.sendParticles(yandere ? YANDERE_DUST : WARM_PEACH, px, py, pz, 2, 0.45, 0.3, 0.45, 0.01);
        }
    }

    private static void sendCozyBellyWhisper(MinecraftServer server, ServerPlayer visitor, OrganZone zone, boolean yandere) {
        ServerLevel overworld = server.overworld();
        long tod = overworld.getOverworldClockTime() % 24000L;
        boolean isNight = tod >= 13000L && tod <= 23000L;
        boolean raining = overworld.isRaining();

        String line;
        if (visitor.getHealth() < visitor.getMaxHealth() * 0.6f) {
            line = "§d「受伤了吗……别怕，先去中间的柔暖软垫上窝一会儿，外面的危险我都帮你挡着♡」";
        } else if (zone == OrganZone.WARM_NOOK) {
            line = "§d「能感觉到你的体温贴在我最温暖的地方……潜行右键软垫就能安心睡到天亮哦♡」";
        } else if (raining) {
            line = "§d「外面下起雨了哦……待在我暖呼呼的身体里，就完全淋不到雨啦~♡」";
        } else if (isNight) {
            line = "§d「外面天黑下来了呢……不用急着出去，在体温暖窝潜行小睡就能安稳度过黑夜哦♡」";
        } else if (zone == OrganZone.NERVE_RING) {
            line = "§d「唔……如果好奇外面怎么样了，潜行右键我的围咽神经环，我把眼睛借给你看哦~♡」";
        } else if (yandere) {
            line = "§c§o「一步、两步……能清楚感觉到你在我里面走动呢，先把我哄乖了再想出门的事吧♡」";
        } else {
            line = "§d「里面还暖和吗？要是觉得饿了，笔直肠道里还有留给你的甜味食糜团哦~♡」";
        }
        visitor.sendSystemMessage(Component.literal(line));
    }

    /** 围咽神经环潜行+空手右键：开启跨维度「神经视窗」，透过粉白蛔虫娘双眼观察并遥控外界！ */
    public static boolean tryOpenNeuralWindow(ServerPlayer visitor, BlockPos pos) {
        ServerLevel bodyLevel = (ServerLevel) visitor.level();
        MinecraftServer server = bodyLevel.getServer();
        BodyRegionData.Visit visit = BodyRegionData.get(server).visit(visitor.getUUID());
        if (visit == null || !visit.isAscaris()) return false;
        int dx = pos.getX() - BodyRegionData.centerX(visit.slot());
        if (Math.abs(dx - AscarisAnatomyBuilder.NERVE_RING_X) > 3) {
            return false;
        }
        return AscarisNeuralLinkController.startLink(visitor);
    }

    /** 围咽神经环手持「粉白虫蜕」右键：触发器官温和自愈（仅修补挖空的原始结构空气孔洞，绝不破坏玩家建筑！） */
    public static boolean trySelfHealFromNerveRing(ServerPlayer visitor, BlockPos pos, ItemStack moltStack) {
        ServerLevel bodyLevel = (ServerLevel) visitor.level();
        MinecraftServer server = bodyLevel.getServer();
        BodyRegionData.Visit visit = BodyRegionData.get(server).visit(visitor.getUUID());
        if (visit == null || !visit.isAscaris()) return false;

        int dx = pos.getX() - BodyRegionData.centerX(visit.slot());
        if (Math.abs(dx - AscarisAnatomyBuilder.NERVE_RING_X) > 4) {
            return false;
        }

        int healed = AscarisAnatomyBuilder.healDamagedAnatomy(bodyLevel, visit.slot(), 256);
        if (healed <= 0) {
            silenceZoneHud(visitor, 60);
            visitor.sendOverlayMessage(Component.literal("§d她的体内器官结构已经完好无损，不需要修补啦~♡"));
            return true;
        }
        if (!visitor.isCreative()) {
            moltStack.shrink(1);
        }
        AscarisPhysiologyEngine.syncBreachesAfterHeal(bodyLevel, visit.slot());
        calmCoelomocytesInSlot(bodyLevel, visit.slot());
        BodyWorld.adjustHostStats(server, visitor, visit, -10, 0, 5);
        bodyLevel.playSound(null, pos, SoundEvents.SLIME_SQUISH_SMALL, SoundSource.BLOCKS, 0.7f, 1.15f);
        bodyLevel.sendParticles(WARM_PEACH, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 16, 0.5, 0.4, 0.5, 0.03);
        bodyLevel.sendParticles(ParticleTypes.HEART, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 6, 0.4, 0.3, 0.4, 0.02);
        silenceZoneHud(visitor, 70);
        visitor.sendOverlayMessage(Component.literal(
                "§d粉白虫蜕融进了围咽神经环，温和愈合了 §f" + healed + " §d处破损组织（你摆放的箱子与装饰丝毫未动~♡）"));
        return true;
    }

    /** 体温暖窝软垫潜行+右键：安心入眠（外界黑夜/风暴直接跳过至清早！）或午后温柔小憩，付与「暖腹余温」并大幅安抚执念！ */
    public static boolean trySleepInWarmNook(ServerPlayer visitor, BlockPos pos) {
        ServerLevel bodyLevel = (ServerLevel) visitor.level();
        MinecraftServer server = bodyLevel.getServer();
        BodyRegionData.Visit visit = BodyRegionData.get(server).visit(visitor.getUUID());
        if (visit == null || !visit.isAscaris()) return false;

        long now = bodyLevel.getGameTime();
        Long prev = LAST_SLEEP_TICK.get(visitor.getUUID());
        if (prev != null && now - prev < 60L) {
            return true;
        }
        LAST_SLEEP_TICK.put(visitor.getUUID(), now);

        ServerLevel overworld = server.overworld();
        long tod = overworld.getOverworldClockTime() % 24000L;
        boolean nightOrStorm = tod >= 12541L || overworld.isRaining() || overworld.isThundering();

        calmCoelomocytesInSlot(bodyLevel, visit.slot());
        int prevObs = BodyWorld.getEffectiveObsession(BodyWorld.findAscarisHost(server, visit), visit);
        int obsDrop = nightOrStorm ? 30 : 20;
        int newObs = BodyWorld.adjustHostStats(server, visitor, visit, -obsDrop, 0, nightOrStorm ? 8 : 5);

        if (nightOrStorm) {
            overworld.dimensionType().defaultClock().ifPresent(clock ->
                    overworld.clockManager().moveToTimeMarker(clock, ClockTimeMarkers.WAKE_UP_FROM_SLEEP));
            overworld.resetWeatherCycle();
            visitor.setHealth(visitor.getMaxHealth());
            visitor.getFoodData().eat(12, 1.0f);
        } else {
            visitor.heal(8.0f);
            visitor.getFoodData().eat(6, 0.6f);
        }
        visitor.removeEffect(MobEffects.DARKNESS);
        visitor.removeEffect(MobEffects.WEAKNESS);
        visitor.removeEffect(MobEffects.MINING_FATIGUE);
        visitor.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 0, true, false));
        visitor.addEffect(new MobEffectInstance(
                JREffects.Companion.getWARM_BELLY_EFFECT(), nightOrStorm ? 3600 : 2400, 0, false, true, true));

        if (visitor instanceof Pregnant p) {
            if (p.getFatigue() > 0) p.setFatigue(0);
            if (p.getUterineCold() > 0) p.setUterineCold(0);
        }

        AscarisEntity ascaris = BodyWorld.findAscarisHost(server, visit);
        AscarisPhysiologyState sleepPhys = BodyRegionData.get(server).physiologyFor(visit.slot());
        if (sleepPhys.temperatureC > 37.3f) {
            sleepPhys.temperatureC = Math.max(37.2f, sleepPhys.temperatureC - 0.7f);
        }
        sleepPhys.metabolicWaste = Math.max(0f, sleepPhys.metabolicWaste - 15.0f);
        if (ascaris != null && ascaris.isAlive()) {
            ascaris.heal(6.0f);
            ascaris.playVoice(JRSounds.ASCARIS_VOICE_SLEEPY, 0.85f, 45, true);
        }

        bodyLevel.playSound(null, pos, SoundEvents.WOOL_STEP, SoundSource.BLOCKS, 0.7f, 0.85f);
        bodyLevel.playSound(null, pos, JRSounds.ASCARIS_VOICE_SLEEPY, SoundSource.VOICE, 0.75f, 1.0f);
        bodyLevel.sendParticles(WARM_PEACH, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 22, 0.6, 0.35, 0.6, 0.02);
        bodyLevel.sendParticles(ParticleTypes.HEART, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 10, 0.5, 0.35, 0.5, 0.02);

        silenceZoneHud(visitor, 80);
        if (nightOrStorm) {
            visitor.sendOverlayMessage(Component.literal("§d你在暖融融的体腔小窝安稳睡到了天亮，外界黑夜与风雨已散去~♡"));
            visitor.sendSystemMessage(Component.literal(
                    "§d「早安呀~♡ 昨晚你在我暖呼呼的身体里睡得很安稳呢，外面的黑夜与风雨已经全部过去啦♡」"));
        } else {
            visitor.sendOverlayMessage(Component.literal("§d你在柔暖软垫上舒舒服服地小憩了一会儿，获得了「暖腹余温」~♡"));
            visitor.sendSystemMessage(Component.literal(
                    "§d「嗯……午后的阳光隔着我的身体变得软绵绵的，再多依偎一会儿也可以哦♡」"));
        }
        announceIfCalmedBelowLock(visitor, prevObs, newObs);
        return true;
    }

    /** 依偎在体温暖窝的柔暖体腔软垫上：温馨回血安神 + 安抚执念与体腔细胞 + 暖心回应！ */
    public static void onWarmCushionSnuggled(ServerPlayer visitor, BlockPos pos) {
        ServerLevel bodyLevel = (ServerLevel) visitor.level();
        MinecraftServer server = bodyLevel.getServer();
        BodyRegionData.Visit visit = BodyRegionData.get(server).visit(visitor.getUUID());
        if (visit == null || !visit.isAscaris()) return;

        calmCoelomocytesInSlot(bodyLevel, visit.slot());
        AscarisEntity ascaris = BodyWorld.findAscarisHost(server, visit);
        int prevObs = BodyWorld.getEffectiveObsession(ascaris, visit);
        int newObs = BodyWorld.adjustHostStats(server, visitor, visit, -15, 0, 5);
        if (ascaris != null && ascaris.isAlive()) {
            ascaris.heal(2.0f);
            ascaris.playVoice(JRSounds.ASCARIS_VOICE_SLEEPY, 0.8f, 45, false);
        }
        ServerPlayNetworking.send(visitor, new AscarisVisualEventPayload(
                AscarisVisualEventPayload.EVENT_BODY_WARMTH, visitor.getId(), 2, newObs));
        silenceZoneHud(visitor, 65);
        visitor.sendOverlayMessage(Component.literal("§d你窝进了暖融融的体腔软垫~♡ §7（潜行+右键可安心小睡；当前执念 " + newObs + "%）"));
        visitor.sendSystemMessage(Component.literal(
                "§d「嗯……就这样乖乖窝在我暖呼呼的身体里歇一会儿吧，外面的一切都交给我守着♡」"));
        announceIfCalmedBelowLock(visitor, prevObs, newObs);
    }

    /** 在体内给粉白体腔小细胞喂甜食时，同步滋养外侧的粉白蛔虫娘并安抚执念 */
    public static void onCoelomocyteFedSweet(ServerPlayer visitor) {
        ServerLevel bodyLevel = (ServerLevel) visitor.level();
        MinecraftServer server = bodyLevel.getServer();
        BodyRegionData.Visit visit = BodyRegionData.get(server).visit(visitor.getUUID());
        if (visit == null || !visit.isAscaris()) return;

        int prevObs = BodyWorld.getEffectiveObsession(BodyWorld.findAscarisHost(server, visit), visit);
        int newObs = BodyWorld.adjustHostStats(server, visitor, visit, -12, 12, 4);
        silenceZoneHud(visitor, 55);
        announceIfCalmedBelowLock(visitor, prevObs, newObs);
    }

    /** 攻击体腔免疫小细胞：唤醒全部体腔细胞防御；病娇模式下反而让她的执念暴涨（+8%）更兴奋！ */
    public static void onCoelomocyteAttacked(ServerPlayer attacker, AscarisCoelomocyteEntity cell) {
        ServerLevel bodyLevel = (ServerLevel) attacker.level();
        MinecraftServer server = bodyLevel.getServer();
        BodyRegionData.Visit visit = BodyRegionData.get(server).visit(attacker.getUUID());
        if (visit == null || !visit.isAscaris()) return;

        alertCoelomocytesInSlot(bodyLevel, visit.slot(), 200);
        AscarisEntity ascaris = BodyWorld.findAscarisHost(server, visit);
        boolean yandereOwned = JRConfig.isAscarisYandereEnabled() && BodyWorld.isOwnedByVisitor(ascaris, visit, attacker);
        silenceZoneHud(attacker, 65);
        if (yandereOwned) {
            int newObs = BodyWorld.adjustHostStats(server, attacker, visit, +8, 0, 0);
            bodyLevel.playSound(null, attacker.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.65f, 0.95f);
            bodyLevel.playSound(null, attacker.blockPosition(), JRSounds.ASCARIS_VOICE_YANDERE_CLING, SoundSource.VOICE, 0.85f, 1.0f);
            bodyLevel.sendParticles(YANDERE_DUST, cell.getX(), cell.getY() + 0.4, cell.getZ(), 14, 0.4, 0.35, 0.4, 0.04);
            attacker.sendOverlayMessage(Component.literal(
                    "§c§o「连我的体腔小细胞都要欺负吗♡ 坏主人……我反而更兴奋了呢♡」§7（执念 +8% → §c" + newObs + "%§7）"));
        } else {
            attacker.sendOverlayMessage(Component.literal("§d粉白体腔小细胞委屈地泛红围了过来，快安抚神经环或喂颗甜食哄好它们吧！"));
        }
    }

    /** 围咽神经环 / 神经索空手右键安抚：显著降低执念（-20%）、平复体腔免疫细胞、解开病娇出口锁定！ */
    public static void onNerveCordSoothed(ServerPlayer visitor, BlockPos pos) {
        ServerLevel bodyLevel = (ServerLevel) visitor.level();
        MinecraftServer server = bodyLevel.getServer();
        BodyRegionData.Visit visit = BodyRegionData.get(server).visit(visitor.getUUID());
        if (visit == null || !visit.isAscaris()) return;

        long now = bodyLevel.getGameTime();
        Long prev = LAST_SOOTHE_TICK.get(visitor.getUUID());
        if (prev != null && now - prev < 25) {
            return;
        }
        LAST_SOOTHE_TICK.put(visitor.getUUID(), now);

        int dx = pos.getX() - BodyRegionData.centerX(visit.slot());
        boolean atNerveRing = Math.abs(dx - AscarisAnatomyBuilder.NERVE_RING_X) <= 3;
        int obsessionDrop = atNerveRing ? 20 : 10;
        int affectionGain = atNerveRing ? 6 : 2;

        calmCoelomocytesInSlot(bodyLevel, visit.slot());
        AscarisPhysiologyState phys = BodyRegionData.get(server).physiologyFor(visit.slot());
        int rx = pos.getX() - BodyRegionData.centerX(visit.slot());
        if (!phys.isParalyzedByPiperazine()) {
            phys.launchImpulse(rx, atNerveRing ? +1 : (rx > AscarisAnatomyBuilder.NERVE_RING_X ? -1 : +1),
                    AscarisPhysiologyState.NerveImpulse.KIND_SENSORY, "");
        }

        AscarisEntity ascaris = BodyWorld.findAscarisHost(server, visit);
        int prevObs = BodyWorld.getEffectiveObsession(ascaris, visit);
        int newObs = BodyWorld.adjustHostStats(server, visitor, visit, -obsessionDrop, 0, affectionGain);
        if (ascaris != null && ascaris.isAlive()) {
            ascaris.heal(atNerveRing ? 4.0f : 1.5f);
            ascaris.playVoice(atNerveRing ? JRSounds.ASCARIS_VOICE_HAPPY : JRSounds.ASCARIS_VOICE_SLEEPY, 0.85f, 35, false);
        }

        bodyLevel.sendParticles(ParticleTypes.HEART, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                atNerveRing ? 6 : 3, 0.35, 0.3, 0.35, 0.02);
        bodyLevel.playSound(null, pos, JRSounds.ASCARIS_VOICE_SLEEPY, SoundSource.VOICE, 0.7f, 1.05f);
        silenceZoneHud(visitor, 60);
        String obsSuffix = JRConfig.isAscarisYandereEnabled() ? (" §7（当前执念 " + newObs + "%）") : "";
        String baseMsg = phys.isParalyzedByPiperazine()
                ? "§b围咽神经环正受哌嗪阻滞（电信号微弱），你轻柔抚摸安抚了她"
                : (atNerveRing
                        ? "§d你轻抚了她的围咽神经环，柔和的生物电波顺着神经干传向全身~♡"
                        : "§d你轻触了神经索，动作电位沿神经干传向围咽神经环~");
        visitor.sendOverlayMessage(Component.literal(baseMsg + obsSuffix));
        announceIfCalmedBelowLock(visitor, prevObs, newObs);
    }

    /** 在咽部 / 神经环处手持甜食右键：直接从内部投喂她，生成对应风味的流动食糜团并温和自愈！ */
    public static boolean tryFeedSweetInsidePharynx(ServerPlayer visitor, BlockPos pos, ItemStack stack) {
        ServerLevel bodyLevel = (ServerLevel) visitor.level();
        MinecraftServer server = bodyLevel.getServer();
        BodyRegionData.Visit visit = BodyRegionData.get(server).visit(visitor.getUUID());
        if (visit == null || !visit.isAscaris()) return false;

        int dx = pos.getX() - BodyRegionData.centerX(visit.slot());
        int dy = pos.getY() - AscarisAnatomyBuilder.FLOOR_Y;
        if (dx > -22 || dy < 5) return false;

        int flavor = AscarisChymeBlock.flavorForItem(stack);
        if (!visitor.isCreative()) {
            stack.shrink(1);
        }
        AscarisEntity ascaris = BodyWorld.findAscarisHost(server, visit);
        int prevObs = BodyWorld.getEffectiveObsession(ascaris, visit);
        int newObs = BodyWorld.adjustHostStats(server, visitor, visit, -18, 25, 8);
        if (ascaris != null && ascaris.isAlive()) {
            ascaris.heal(5.0f);
            ascaris.playVoice(JRSounds.ASCARIS_VOICE_HAPPY, 0.9f, 35, true);
        }
        calmCoelomocytesInSlot(bodyLevel, visit.slot());
        replenishSweetChyme(bodyLevel, visit.slot(), flavor);
        AscarisPhysiologyEngine.onFoodSwallowed(bodyLevel, visit.slot(), ascaris, flavor);
        AscarisAnatomyBuilder.healDamagedAnatomy(bodyLevel, visit.slot(), 32);
        AscarisPhysiologyEngine.syncBreachesAfterHeal(bodyLevel, visit.slot());

        bodyLevel.playSound(null, pos, SoundEvents.HONEY_DRINK.value(), SoundSource.PLAYERS, 0.55f, 1.15f);
        bodyLevel.sendParticles(ParticleTypes.HEART, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 6, 0.35, 0.3, 0.35, 0.02);
        silenceZoneHud(visitor, 65);
        visitor.sendOverlayMessage(Component.literal(
                "§d肌肉咽负压抽吸吞下了甜食，一枚「" + AscarisChymeBlock.flavorLabel(flavor) + "」食糜团正随蠕动波滚入肠道~♡"));
        announceIfCalmedBelowLock(visitor, prevObs, newObs);
        return true;
    }

    /** 在体内向咽部/神经环/肠道直接给药（宝塔糖=哌嗪神经肌肉阻滞；阿苯达唑=微管解聚与糖摄取阻断！） */
    public static boolean tryAdministerDrugInside(ServerPlayer visitor, BlockPos pos, ItemStack stack) {
        if (!AscarisEntity.isDewormer(stack)) return false;
        ServerLevel bodyLevel = (ServerLevel) visitor.level();
        MinecraftServer server = bodyLevel.getServer();
        BodyRegionData.Visit visit = BodyRegionData.get(server).visit(visitor.getUUID());
        if (visit == null || !visit.isAscaris()) return false;

        boolean harsh = stack.is(org.cneko.justarod.item.JRItems.Companion.getALBENDAZOLE());
        if (!visitor.isCreative()) {
            stack.shrink(1);
        }
        AscarisEntity ascaris = BodyWorld.findAscarisHost(server, visit);
        int drugType = harsh ? AscarisPhysiologyState.DRUG_ALBENDAZOLE : AscarisPhysiologyState.DRUG_PIPERAZINE;
        AscarisPhysiologyEngine.administerInternalDrug(bodyLevel, visit.slot(), ascaris, drugType, harsh ? 1200 : 900);

        bodyLevel.playSound(null, pos, JRSounds.ASCARIS_RUMBLE, SoundSource.PLAYERS, 0.85f, harsh ? 0.85f : 1.05f);
        bodyLevel.sendParticles(harsh ? YANDERE_DUST : AQUA_DUST,
                pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 20, 0.7, 0.5, 0.7, 0.04);
        silenceZoneHud(visitor, 90);
        if (harsh) {
            visitor.sendSystemMessage(Component.literal(
                    "§c[阿苯达唑药理发作] §f微管聚合受阻！肠绒毛停止摄取葡萄糖、糖原断崖枯竭并引发高热——可前往 H 形排泄管横桥强力冲洗排毒并灌注甜食抢救！"));
        } else {
            visitor.sendSystemMessage(Component.literal(
                    "§b[枸橼酸哌嗪药理发作] §f宝塔糖阻断了神经肌肉接头！背腹神经脉冲熄灭、静水压骤降、肠道蠕动停摆，全部出口括约肌已弛缓松开！"));
        }
        AscarisPhysiologyEngine.sendPhysiologySync(visitor, BodyRegionData.get(server).physiologyFor(visit.slot()));
        return true;
    }

    /** 空手轻挠 Y 形三辐射肌肉咽（dx ∈ [-39, -25]）触发机械吞咽/干呕反射弧 */
    public static boolean tryStimulatePharynx(ServerPlayer visitor, BlockPos pos) {
        ServerLevel bodyLevel = (ServerLevel) visitor.level();
        MinecraftServer server = bodyLevel.getServer();
        BodyRegionData.Visit visit = BodyRegionData.get(server).visit(visitor.getUUID());
        if (visit == null || !visit.isAscaris()) return false;
        int dx = pos.getX() - BodyRegionData.centerX(visit.slot());
        int dy = pos.getY() - AscarisAnatomyBuilder.FLOOR_Y;
        if (dy < 5 || dx < -39 || dx > -25) return false;
        AscarisPhysiologyEngine.onPharynxStimulated(bodyLevel, visit.slot(), BodyWorld.findAscarisHost(server, visit), visitor, pos);
        return true;
    }

    /** 空手轻抚盘绕丝状卵巢组织 -> 刺激排卵推进 + 神经反射弧 */
    public static boolean tryStimulateOvary(ServerPlayer visitor, BlockPos pos) {
        ServerLevel bodyLevel = (ServerLevel) visitor.level();
        MinecraftServer server = bodyLevel.getServer();
        BodyRegionData.Visit visit = BodyRegionData.get(server).visit(visitor.getUUID());
        if (visit == null || !visit.isAscaris()) return false;
        AscarisPhysiologyEngine.onOvaryStimulated(bodyLevel, visit.slot(), BodyWorld.findAscarisHost(server, visit), visitor, pos);
        return true;
    }

    /** 空手探查/刺激柔粉肠绒毛 */
    public static void onMicrovilliInteracted(ServerPlayer visitor, BlockPos pos) {
        ServerLevel bodyLevel = (ServerLevel) visitor.level();
        MinecraftServer server = bodyLevel.getServer();
        BodyRegionData.Visit visit = BodyRegionData.get(server).visit(visitor.getUUID());
        if (visit == null || !visit.isAscaris()) return;
        AscarisPhysiologyEngine.onMicrovilliStimulated(bodyLevel, visit.slot(), visitor, pos);
    }

    /** 体内右键 H 形排泄管膜：同步疏通宿主排泄孔与代谢废物/药毒 */
    public static void onExcretoryFlushedInside(ServerPlayer visitor, BlockPos pos) {
        ServerLevel bodyLevel = (ServerLevel) visitor.level();
        MinecraftServer server = bodyLevel.getServer();
        BodyRegionData.Visit visit = BodyRegionData.get(server).visit(visitor.getUUID());
        if (visit == null || !visit.isAscaris()) return;
        AscarisPhysiologyEngine.onExcretoryFlushed(bodyLevel, visit.slot(), BodyWorld.findAscarisHost(server, visit), visitor);
    }

    /** 三大天然解剖孔道（口腔三唇 dx<=-41 / 尾端肛门括约肌 dx>=40 / 前 1/3 腹侧阴门 dx∈[-18,-15]）处空手右键自然滑出体外 */
    public static boolean tryNaturalExitAtOrifice(ServerPlayer visitor, BlockPos pos) {
        ServerLevel bodyLevel = (ServerLevel) visitor.level();
        MinecraftServer server = bodyLevel.getServer();
        BodyRegionData.Visit visit = BodyRegionData.get(server).visit(visitor.getUUID());
        if (visit == null || !visit.isAscaris()) return false;

        int dx = pos.getX() - BodyRegionData.centerX(visit.slot());
        int dy = pos.getY() - AscarisAnatomyBuilder.FLOOR_Y;
        int dz = pos.getZ() - BodyRegionData.centerZ(visit.slot());
        boolean oralOrAnal = (dy >= 5) && (dx <= -41 || dx >= 40);
        boolean vaginal = (dy <= 4) && (dx >= -18 && dx <= -15) && Math.abs(dz) <= 3;
        if (oralOrAnal || vaginal) {
            BodyWorld.exit(visitor);
            return true;
        }
        return false;
    }

    /** 玩家直接啃食肠道内的甜味食糜团（向后兼容 1 参版本） */
    public static void onChymeEatenInside(ServerPlayer visitor) {
        onChymeEatenInside(visitor, AscarisChymeBlock.FLAVOR_SWEET);
    }

    /** 玩家直接啃食肠道内的多风味甜味食糜团：反向偷吃扣除外界粉白蛔虫娘饱食度 */
    public static void onChymeEatenInside(ServerPlayer visitor, int flavor) {
        ServerLevel bodyLevel = (ServerLevel) visitor.level();
        MinecraftServer server = bodyLevel.getServer();
        BodyRegionData.Visit visit = BodyRegionData.get(server).visit(visitor.getUUID());
        if (visit == null || !visit.isAscaris()) return;

        BodyWorld.adjustHostStats(server, visitor, visit, 0, -6, 0);
        AscarisPhysiologyState phys = BodyRegionData.get(server).physiologyFor(visit.slot());
        phys.glycogen = Math.max(0f, phys.glycogen - 4.0f);
        AscarisEntity ascaris = BodyWorld.findAscarisHost(server, visit);
        if (ascaris != null && ascaris.isAlive() && ascaris.level() instanceof ServerLevel hostLevel) {
            hostLevel.sendParticles(PINK_DUST, ascaris.getX(), ascaris.getY() + 0.8, ascaris.getZ(), 8, 0.3, 0.3, 0.3, 0.03);
        }
        silenceZoneHud(visitor, 60);
        String msg = switch (flavor) {
            case AscarisChymeBlock.FLAVOR_GLOW -> "§e你偷吃了一口发光浆果食糜，眼前顿时明亮通透了起来~（获得夜视）";
            case AscarisChymeBlock.FLAVOR_HONEY_GOLD -> "§6你偷吃了一口蜂蜜金苹食糜，香甜暖流化作了守护微光~（获得伤害吸收）";
            default -> "§d你偷吃了一口肠道里的甜味食糜，温润香甜~（外面的她肚子轻轻动了一下）";
        };
        visitor.sendOverlayMessage(Component.literal(msg));
    }

    /** 玩家在体内空手剥离采集卵囊时的温柔反应 */
    public static void onEggSacHarvestedInside(ServerPlayer visitor) {
        ServerLevel bodyLevel = (ServerLevel) visitor.level();
        MinecraftServer server = bodyLevel.getServer();
        BodyRegionData.Visit visit = BodyRegionData.get(server).visit(visitor.getUUID());
        if (visit == null || !visit.isAscaris()) return;

        AscarisEntity ascaris = BodyWorld.findAscarisHost(server, visit);
        AscarisPhysiologyState phys = BodyRegionData.get(server).physiologyFor(visit.slot());
        phys.uterineLoad = Math.max(0f, phys.uterineLoad - 10.0f);
        if (ascaris != null && ascaris.isAlive()) {
            ascaris.playVoice(JRSounds.ASCARIS_VOICE_HOP, 0.8f, 40, false);
        }
        silenceZoneHud(visitor, 60);
    }

    /**
     * 玩家在体内挖掘她的生物组织方块时：
     * - 唤醒体腔免疫小细胞护体；
     * - 病娇模式下：破坏/弄痛她反而会令她更兴奋、病娇执念 +10%，无法靠暴力逃出！
     */
    public static void onAnatomyBlockBroken(ServerPlayer visitor, BlockPos pos, BlockState state) {
        if (!visitor.level().dimension().equals(BodyWorld.DIMENSION)) return;
        ServerLevel bodyLevel = (ServerLevel) visitor.level();
        MinecraftServer server = bodyLevel.getServer();
        BodyRegionData.Visit visit = BodyRegionData.get(server).visit(visitor.getUUID());
        if (visit == null || !visit.isAscaris()) return;

        AscarisEntity ascaris = BodyWorld.findAscarisHost(server, visit);
        boolean yandereOwned = JRConfig.isAscarisYandereEnabled() && BodyWorld.isOwnedByVisitor(ascaris, visit, visitor);

        // 记录体壁/器官破口（导致假体腔静水压微渗漏、炎症发热并触发痛觉神经反射弧）
        AscarisPhysiologyEngine.onTissueBreached(bodyLevel, visit.slot(), pos, state);

        // 唤醒附近体腔免疫小细胞进入护体警戒！
        alertCoelomocytesInSlot(bodyLevel, visit.slot(), yandereOwned ? 220 : 160);

        long now = bodyLevel.getGameTime();
        Long prev = LAST_PAIN_TICK.get(visitor.getUUID());
        if (prev != null && now - prev < 40) {
            return;
        }
        LAST_PAIN_TICK.put(visitor.getUUID(), now);

        int newObs = BodyWorld.getEffectiveObsession(ascaris, visit);
        if (yandereOwned) {
            newObs = BodyWorld.adjustHostStats(server, visitor, visit, +10, 0, 0);
        }

        if (ascaris != null && ascaris.isAlive()) {
            if (ascaris.getHealth() > 5.0f && ascaris.level() instanceof ServerLevel hostLevel) {
                ascaris.setHealth(Math.max(4.0f, ascaris.getHealth() - 1.0f));
                hostLevel.sendParticles(yandereOwned ? YANDERE_DUST : PINK_DUST,
                        ascaris.getX(), ascaris.getY() + 0.8, ascaris.getZ(), 8, 0.35, 0.3, 0.35, 0.04);
            }
            if (!ascaris.isMovementLocked()) {
                ascaris.playTransientPose(yandereOwned ? AscarisPoseState.YANDERE_STARE : AscarisPoseState.CURL_UP, 42);
            }
            ascaris.playVoice(yandereOwned ? JRSounds.ASCARIS_VOICE_YANDERE_GIGGLE : JRSounds.ASCARIS_VOICE_HURT, 0.88f, 45, true);
        }

        visitor.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 0, true, false, false));
        bodyLevel.sendParticles(yandereOwned ? YANDERE_DUST : PINK_DUST,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 14, 0.4, 0.4, 0.4, 0.04);
        if (yandereOwned) {
            bodyLevel.playSound(null, visitor.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.6f, 0.95f);
            bodyLevel.playSound(null, visitor.blockPosition(), JRSounds.ASCARIS_VOICE_YANDERE_GIGGLE, SoundSource.VOICE, 0.78f, 1.0f);
        } else {
            bodyLevel.playSound(null, visitor.blockPosition(), JRSounds.ASCARIS_VOICE_HURT, SoundSource.VOICE, 0.65f, 1.0f);
        }
        ServerPlayNetworking.send(visitor, new AscarisVisualEventPayload(
                AscarisVisualEventPayload.EVENT_BELLY_RUMBLE, visitor.getId(), 2, 0));

        silenceZoneHud(visitor, 70);
        if (yandereOwned) {
            visitor.sendOverlayMessage(Component.literal(
                    "§c§o「啊哈♡ 越在里面折腾弄痛我，我就越兴奋、越舍不得放开你呢♡」§7（执念 +10% → §c" + newObs + "%§7）"));
        } else {
            visitor.sendOverlayMessage(Component.literal("§d「呜咿！好痛……体腔小细胞都急着冲过来保护人家了啦 >~<」"));
        }
    }

    /** 在笔直肠道中段补长 1 团默认甜味食糜 */
    public static boolean replenishSweetChyme(ServerLevel bodyLevel, long slot) {
        return replenishSweetChyme(bodyLevel, slot, AscarisChymeBlock.FLAVOR_SWEET);
    }

    /** 在笔直肠道中段补长 1 团指定风味的新鲜食糜（优先填补空位或咬过的食糜，特殊风味也可升级一处普通食糜） */
    public static boolean replenishSweetChyme(ServerLevel bodyLevel, long slot, int flavor) {
        int cx = BodyRegionData.centerX(slot);
        int cz = BodyRegionData.centerZ(slot);
        int y = AscarisAnatomyBuilder.GUT_FLOOR_Y + 1;
        BlockState fresh = JRBlocks.ASCARIS_CHYME_BLOCK.defaultBlockState()
                .setValue(AscarisChymeBlock.BITES, 0)
                .setValue(AscarisChymeBlock.FLAVOR, flavor);

        // 优先替换空气或被咬过的食糜
        for (int[] anchor : CHYME_ANCHORS) {
            BlockPos pos = new BlockPos(cx + anchor[0], y, cz + anchor[1]);
            BlockState cur = bodyLevel.getBlockState(pos);
            if (cur.isAir() || (cur.is(JRBlocks.ASCARIS_CHYME_BLOCK)
                    && cur.getValue(AscarisChymeBlock.BITES) > 0)) {
                bodyLevel.setBlock(pos, fresh, Block.UPDATE_ALL);
                bodyLevel.sendParticles(PEARL_DUST, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        8, 0.3, 0.25, 0.3, 0.02);
                return true;
            }
        }
        // 若喂的是特殊风味且 4 处食糜都是完好的普通味，升级第 1 处普通食糜为新风味
        if (flavor != AscarisChymeBlock.FLAVOR_SWEET) {
            for (int[] anchor : CHYME_ANCHORS) {
                BlockPos pos = new BlockPos(cx + anchor[0], y, cz + anchor[1]);
                BlockState cur = bodyLevel.getBlockState(pos);
                if (cur.is(JRBlocks.ASCARIS_CHYME_BLOCK)
                        && cur.getValue(AscarisChymeBlock.FLAVOR) == AscarisChymeBlock.FLAVOR_SWEET) {
                    bodyLevel.setBlock(pos, fresh, Block.UPDATE_ALL);
                    bodyLevel.sendParticles(WARM_PEACH, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            10, 0.3, 0.25, 0.3, 0.02);
                    return true;
                }
            }
        }
        return false;
    }

    /** 在左右双管子宫外侧内壁自动再生 1 块新的蛔虫卵囊块 */
    public static boolean tryRegenerateUterineEggSac(ServerLevel bodyLevel, long slot) {
        int cx = BodyRegionData.centerX(slot);
        int cz = BodyRegionData.centerZ(slot);
        int y = AscarisAnatomyBuilder.FLOOR_Y + 1;
        for (int side : new int[] {-1, 1}) {
            int eggZ = cz + side * 11;
            for (int rx : EGG_SAC_XS) {
                BlockPos pos = new BlockPos(cx + rx, y, eggZ);
                if (bodyLevel.getBlockState(pos).isAir()) {
                    bodyLevel.setBlock(pos, JRBlocks.ASCARIS_EGG_SAC.defaultBlockState(), Block.UPDATE_ALL);
                    bodyLevel.sendParticles(PEARL_DUST, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            8, 0.25, 0.25, 0.25, 0.02);
                    return true;
                }
            }
        }
        return false;
    }

    private static void announceIfCalmedBelowLock(ServerPlayer visitor, int prevObs, int newObs) {
        if (JRConfig.isAscarisYandereEnabled()
                && prevObs >= BodyWorld.YANDERE_LOCK_OBSESSION
                && newObs < BodyWorld.YANDERE_LOCK_OBSESSION) {
            visitor.sendSystemMessage(Component.literal(
                    "§a✦ 她的情绪彻底柔和了下来（执念降至 " + newObs + "%）！口腔三唇与尾端肛门出口已重新为你松开~♡"));
        }
    }
}
