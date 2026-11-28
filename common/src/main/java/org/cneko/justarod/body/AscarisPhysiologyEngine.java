package org.cneko.justarod.body;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.cneko.justarod.ascaris.AscarisEntity;
import org.cneko.justarod.ascaris.AscarisInfectionController;
import org.cneko.justarod.ascaris.AscarisPoseState;
import org.cneko.justarod.block.JRBlocks;
import org.cneko.justarod.block.ascaris.AscarisChymeBlock;
import org.cneko.justarod.block.ascaris.AscarisNerveCordBlock;
import org.cneko.justarod.entity.Pregnant;
import org.cneko.justarod.item.JRItems;
import org.cneko.justarod.packet.AscarisVisualEventPayload;
import org.cneko.justarod.sound.JRSounds;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * 硬核体内生理循环引擎：
 * <ul>
 *   <li>粉白蛔虫娘体内：静水骨骼液压、厌氧糖原代谢、活体消化流水线（咽部负压抽吸 → 滚动蠕动波 → 移动食糜团 → 肠绒毛吸收 → 直肠排空）、
 *       H 形排泄管渗透稳态、神经索级联脉冲反射弧、卵巢→输卵管→双管子宫→阴道宫缩产卵周期、宝塔糖(哌嗪) vs 阿苯达唑双药药理发作、
 *       以及体壁破口渗漏与体腔小细胞自主凝血修补；</li>
 *   <li>玩家宿主体内：实时映射 {@link Pregnant} 生理状态（心跳/宫寒体温、月经周期/宫缩、孕期胎动、排泄/膀胱压力与寄生虫联动）。</li>
 * </ul>
 */
public final class AscarisPhysiologyEngine {

    private static final DustParticleOptions PINK_DUST = new DustParticleOptions(0xFF9AC2, 0.95f);
    private static final DustParticleOptions PEARL_DUST = new DustParticleOptions(0xFFF3F8, 0.88f);
    private static final DustParticleOptions PEACH_BOLUS = new DustParticleOptions(0xFFD096, 1.15f);
    private static final DustParticleOptions GLOW_BOLUS = new DustParticleOptions(0xFFEE7A, 1.2f);
    private static final DustParticleOptions HONEY_BOLUS = new DustParticleOptions(0xFFB340, 1.2f);
    private static final DustParticleOptions NERVE_GOLD = new DustParticleOptions(0xFFE574, 0.95f);
    private static final DustParticleOptions PAIN_CRIMSON = new DustParticleOptions(0xE8285A, 1.05f);
    private static final DustParticleOptions AQUA_EXCRETORY = new DustParticleOptions(0x88ECFF, 0.9f);
    private static final DustParticleOptions TOXIN_MIST = new DustParticleOptions(0x7FA89B, 1.0f);
    private static final DustParticleOptions PIPERAZINE_BLUE = new DustParticleOptions(0x9BB8FF, 0.95f);
    private static final DustParticleOptions ALBENDAZOLE_ASH = new DustParticleOptions(0xC97458, 1.0f);
    private static final DustParticleOptions HEMOLYMPH_LEAK = new DustParticleOptions(0xFF7FA8, 0.9f);
    private static final DustParticleOptions FEVER_STEAM = new DustParticleOptions(0xFF8C66, 0.85f);

    /** 双管子宫按真实解剖从后段（靠近输卵管）向阴门前段依次生长卵囊的 X 坐标顺序 */
    private static final int[] UTERINE_EGG_POSTERIOR_TO_ANTERIOR = {12, 8, 4, 0, -4};
    private static final int[][] CHYME_ANCHORS = {{-6, -2}, {-6, 2}, {10, 2}, {10, -2}};

    private AscarisPhysiologyEngine() {
    }

    /** 玩家进入粉白蛔虫娘体内时，从外界本体同步初始糖原、药理状态与活性 */
    public static void initFromExternalHost(MinecraftServer server, long slot, @Nullable AscarisEntity ascaris) {
        BodyRegionData data = BodyRegionData.get(server);
        AscarisPhysiologyState st = data.physiologyFor(slot);
        if (ascaris != null && ascaris.isAlive()) {
            st.glycogen = Mth.clamp(Mth.lerp(0.55f, st.glycogen, ascaris.getHunger()), 5f, 100f);
            if (ascaris.getExpelledStage() == AscarisEntity.EXPELLED_ALBENDAZOLE_CRITICAL || ascaris.getAlbendazoleToxicityTicks() > 0) {
                st.drugType = AscarisPhysiologyState.DRUG_ALBENDAZOLE;
                st.drugTicks = Math.max(st.drugTicks, Math.max(400, ascaris.getAlbendazoleToxicityTicks() * 4));
                st.temperatureC = Math.max(st.temperatureC, 38.8f);
            } else if (ascaris.getExpelledStage() == AscarisEntity.EXPELLED_PAGODA_WEAK) {
                st.drugType = AscarisPhysiologyState.DRUG_PIPERAZINE;
                st.drugTicks = Math.max(st.drugTicks, 400);
                st.hydrostaticPressure = Math.min(st.hydrostaticPressure, 32.0f);
            }
            if (st.boluses.isEmpty() && ascaris.getHunger() >= 45) {
                st.addBolus(-18.0, 0, AscarisChymeBlock.FLAVOR_SWEET, 75.0f);
            }
            data.markPhysiologyDirty();
        }
    }

    /** 单槽位每 Server Tick 生理主驱动（同一 tick 自动去重，支持多位访客共享同一活体生理循环） */
    public static void tickSlotPhysiology(ServerLevel bodyLevel, long slot, @Nullable AscarisEntity ascaris,
                                          ServerPlayer visitor, BodyRegionData.Visit visit) {
        MinecraftServer server = bodyLevel.getServer();
        BodyRegionData data = BodyRegionData.get(server);
        AscarisPhysiologyState st = data.physiologyFor(slot);
        long gameTime = bodyLevel.getGameTime();

        boolean firstThisTick = (st.lastSimTick != gameTime);
        if (firstThisTick) {
            st.lastSimTick = gameTime;
            syncExternalHostState(server, bodyLevel, slot, st, ascaris, visit, gameTime);
            tickPharmacology(bodyLevel, slot, st, ascaris, gameTime);
            tickDigestionPipeline(bodyLevel, slot, st, ascaris, visitor, visit, gameTime);
            tickHydrostaticAndExcretory(bodyLevel, slot, st, ascaris, gameTime);
            tickReproductivePipeline(bodyLevel, slot, st, ascaris, gameTime);
            tickNerveImpulses(bodyLevel, slot, st, ascaris, gameTime);
            st.clampAll();
            if (gameTime % 60 == 0) {
                data.markPhysiologyDirty();
            }
        }

        // 每位访客的局部物理受力与生理环境体验
        applyPhysiologyEffectsToVisitor(bodyLevel, slot, st, visitor, visit, gameTime);

        // 每 10 tick 同步硬核生理体征到客户端 HUD
        if (visitor.tickCount % 10 == 0 && !AscarisNeuralLinkController.isLinked(visitor.getUUID())) {
            sendPhysiologySync(visitor, st);
        }
    }

    public static void sendPhysiologySync(ServerPlayer visitor, AscarisPhysiologyState st) {
        ServerPlayNetworking.send(visitor, new AscarisVisualEventPayload(
                AscarisVisualEventPayload.EVENT_PHYSIOLOGY_SYNC,
                st.packVitalsInt(),
                st.packOrgansInt(),
                st.packCycleFlagsInt()
        ));
    }

    public static void clearClientPhysiology(ServerPlayer visitor) {
        ServerPlayNetworking.send(visitor, new AscarisVisualEventPayload(
                AscarisVisualEventPayload.EVENT_PHYSIOLOGY_SYNC, -1, 0, 0));
    }

    /** 外界本体活动、环境气候与伤势实时映射到体内静水骨骼与运动神经 */
    private static void syncExternalHostState(MinecraftServer server, ServerLevel bodyLevel, long slot,
                                              AscarisPhysiologyState st, @Nullable AscarisEntity ascaris,
                                              BodyRegionData.Visit visit, long gameTime) {
        ServerLevel overworld = server.overworld();
        boolean raining = overworld.isRaining();
        long tod = overworld.getOverworldClockTime() % 24000L;
        boolean night = tod >= 13000L && tod <= 23000L;

        int activity = AscarisPhysiologyState.ACTIVITY_IDLE;
        float envTempDelta = (raining ? -0.45f : 0f) + (night ? -0.25f : 0.1f);

        if (ascaris != null && ascaris.isAlive()) {
            AscarisPoseState pose = ascaris.getPoseState();
            double speedSq = ascaris.getDeltaMovement().horizontalDistanceSqr();
            if (pose == AscarisPoseState.COLLAPSE) {
                activity = AscarisPhysiologyState.ACTIVITY_COLLAPSE;
            } else if (pose == AscarisPoseState.SLEEP || pose == AscarisPoseState.REST) {
                activity = AscarisPhysiologyState.ACTIVITY_SLEEP;
            } else if (pose.isWrapping()) {
                activity = AscarisPhysiologyState.ACTIVITY_WRAP;
            } else if (pose == AscarisPoseState.CURL_UP || ascaris.getHealth() < ascaris.getMaxHealth() * 0.45f) {
                activity = AscarisPhysiologyState.ACTIVITY_HURT_CURL;
            } else if (ascaris.isCrawling()) {
                activity = AscarisPhysiologyState.ACTIVITY_CRAWL;
            } else if (speedSq > 0.018 || !ascaris.onGround()) {
                activity = AscarisPhysiologyState.ACTIVITY_RUN_JUMP;
            } else if (speedSq > 0.0008) {
                activity = AscarisPhysiologyState.ACTIVITY_WALK;
            }

            // 外界阿苯达唑残留自动同步至体内药理状态
            if (ascaris.getAlbendazoleToxicityTicks() > 0 && st.drugType == AscarisPhysiologyState.DRUG_NONE) {
                st.drugType = AscarisPhysiologyState.DRUG_ALBENDAZOLE;
                st.drugTicks = Math.max(300, ascaris.getAlbendazoleToxicityTicks() * 3);
            }
            // 结合外界所在生物群系温度微调体温
            try {
                float biomeTemp = ascaris.level().getBiome(ascaris.blockPosition()).value().getBaseTemperature();
                if (biomeTemp < 0.25f) envTempDelta -= 0.7f;
                else if (biomeTemp > 1.2f) envTempDelta += 0.55f;
            } catch (Throwable ignored) {
            }
        }
        st.hostActivity = activity;

        // 运动传出神经脉冲（围咽神经环 -> 纵肌背腹交替传导）+ 运动糖原消耗
        if (!st.isParalyzedByPiperazine()) {
            if (activity == AscarisPhysiologyState.ACTIVITY_RUN_JUMP || activity == AscarisPhysiologyState.ACTIVITY_WRAP) {
                if (gameTime % 26 == 0) {
                    st.launchImpulse(AscarisAnatomyBuilder.NERVE_RING_X, +1, AscarisPhysiologyState.NerveImpulse.KIND_MOTOR, "");
                }
                if (gameTime % 20 == 0) {
                    st.glycogen -= 0.28f;
                    st.metabolicWaste += 0.22f;
                }
            } else if (activity == AscarisPhysiologyState.ACTIVITY_WALK || activity == AscarisPhysiologyState.ACTIVITY_CRAWL) {
                if (gameTime % 48 == 0) {
                    st.launchImpulse(AscarisAnatomyBuilder.NERVE_RING_X, +1, AscarisPhysiologyState.NerveImpulse.KIND_MOTOR, "");
                }
                if (gameTime % 20 == 0) {
                    st.glycogen -= 0.14f;
                    st.metabolicWaste += 0.10f;
                }
            } else if (gameTime % 40 == 0) {
                // 基础厌氧糖酵解消耗
                st.glycogen -= (activity == AscarisPhysiologyState.ACTIVITY_SLEEP ? 0.05f : 0.09f);
                st.metabolicWaste += 0.08f;
            }
        }

        // 核心体温动态趋近目标值（正常 37.2°C + 糖原产热 + 炎症/阿苯达唑发热 + 病娇燥热 + 环境冷暖）
        float targetTemp = 37.2f + envTempDelta;
        if (st.glycogen < 20f) targetTemp -= 0.85f;
        if (st.glycogen > 75f) targetTemp += 0.25f;
        if (!st.breaches.isEmpty()) targetTemp += Math.min(1.6f, st.breaches.size() * 0.28f);
        if (st.isBlockedByAlbendazole()) targetTemp += 1.55f;
        if (st.isParalyzedByPiperazine()) targetTemp -= 0.75f;
        if (BodyWorld.getEffectiveObsession(ascaris, visit) >= BodyWorld.YANDERE_LOCK_OBSESSION) targetTemp += 0.45f;
        if (activity == AscarisPhysiologyState.ACTIVITY_RUN_JUMP || activity == AscarisPhysiologyState.ACTIVITY_WRAP) {
            targetTemp += 0.4f;
        }
        st.temperatureC = Mth.lerp(0.015f, st.temperatureC, Mth.clamp(targetTemp, 34.5f, 40.2f));
    }

    /** 双药硬核药理模拟：枸橼酸哌嗪（神经肌肉接头阻滞） vs 阿苯达唑（微管解聚/糖摄取阻断） */
    private static void tickPharmacology(ServerLevel bodyLevel, long slot, AscarisPhysiologyState st,
                                         @Nullable AscarisEntity ascaris, long gameTime) {
        if (st.drugTicks <= 0 || st.drugType == AscarisPhysiologyState.DRUG_NONE) {
            st.drugType = AscarisPhysiologyState.DRUG_NONE;
            st.drugTicks = 0;
            return;
        }
        st.drugTicks--;
        int cx = BodyRegionData.centerX(slot);
        int cz = BodyRegionData.centerZ(slot);

        if (st.drugType == AscarisPhysiologyState.DRUG_PIPERAZINE) {
            // 宝塔糖（枸橼酸哌嗪）：超极化阻断神经肌肉接头 -> 清除传导中的神经脉冲、静水压暴跌、蠕动完全停摆
            st.impulses.clear();
            st.peristalsisActive = false;
            st.pharynxSuctionTicks = 0;
            if (gameTime % 12 == 0) {
                st.hydrostaticPressure = Mth.lerp(0.18f, st.hydrostaticPressure, 22.0f);
                st.metabolicWaste += 0.12f;
                bodyLevel.sendParticles(PIPERAZINE_BLUE,
                        cx + AscarisAnatomyBuilder.NERVE_RING_X + 0.5,
                        AscarisAnatomyBuilder.GUT_FLOOR_Y + 2.5,
                        cz + 0.5, 6, 2.2, 1.5, 2.2, 0.01);
            }
        } else if (st.drugType == AscarisPhysiologyState.DRUG_ALBENDAZOLE) {
            // 阿苯达唑：抑制肠上皮细胞微管聚合 -> 阻断葡萄糖摄取、糖原断崖枯竭、代谢毒物与炎症高热飙升！
            if (gameTime % 10 == 0) {
                st.glycogen = Math.max(0f, st.glycogen - 0.55f);
                st.metabolicWaste = Math.min(100f, st.metabolicWaste + 0.42f);
                bodyLevel.sendParticles(ALBENDAZOLE_ASH,
                        cx + (bodyLevel.getRandom().nextDouble() - 0.5) * 32.0,
                        AscarisAnatomyBuilder.GUT_FLOOR_Y + 2.0,
                        cz + (bodyLevel.getRandom().nextDouble() - 0.5) * 6.0,
                        6, 1.2, 0.6, 1.2, 0.015);
            }
            // 每 140 tick 肠道锚点食糜干枯萎缩 1 口，直观体现吸收障碍与营养枯竭！
            if (gameTime % 140 == 0) {
                int y = AscarisAnatomyBuilder.GUT_FLOOR_Y + 1;
                for (int[] anchor : CHYME_ANCHORS) {
                    BlockPos pos = new BlockPos(cx + anchor[0], y, cz + anchor[1]);
                    BlockState bs = bodyLevel.getBlockState(pos);
                    if (bs.is(JRBlocks.ASCARIS_CHYME_BLOCK)) {
                        int bites = bs.getValue(AscarisChymeBlock.BITES);
                        if (bites < 3) {
                            bodyLevel.setBlock(pos, bs.setValue(AscarisChymeBlock.BITES, bites + 1), Block.UPDATE_ALL);
                            bodyLevel.sendParticles(ALBENDAZOLE_ASH, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                    8, 0.25, 0.2, 0.25, 0.02);
                            break;
                        }
                    }
                }
            }
            if (ascaris != null && ascaris.isAlive() && gameTime % 60 == 0 && ascaris.getHunger() > 5) {
                ascaris.setHunger(ascaris.getHunger() - 2);
            }
        }

        if (st.drugTicks <= 0) {
            st.drugType = AscarisPhysiologyState.DRUG_NONE;
            if (ascaris != null && ascaris.isAlive() && st.glycogen >= 35f) {
                ascaris.recoverFromInternalRescue();
            }
        }
    }

    /** 完整活体消化流水线：咽部抽吸泵 → 滚动蠕动推进波 → 移动食糜团 → 微绒毛真实吸收 → 直肠积聚与排空 */
    private static void tickDigestionPipeline(ServerLevel bodyLevel, long slot, AscarisPhysiologyState st,
                                              @Nullable AscarisEntity ascaris, ServerPlayer visitor,
                                              BodyRegionData.Visit visit, long gameTime) {
        int cx = BodyRegionData.centerX(slot);
        int cz = BodyRegionData.centerZ(slot);

        // 1. 咽部负压抽吸泵节律（x = -42 .. -25）
        if (st.pharynxSuctionTicks > 0) {
            st.pharynxSuctionTicks--;
            if (st.pharynxSuctionTicks % 5 == 0) {
                double progress = 1.0 - (st.pharynxSuctionTicks / 40.0);
                double px = cx - 42.0 + progress * 17.0;
                bodyLevel.sendParticles(PINK_DUST, px, AscarisAnatomyBuilder.GUT_FLOOR_Y + 2.0, cz + 0.5,
                        6, 0.4, 0.6, 0.8, 0.03);
            }
        }
        if (st.microvilliBoostTicks > 0) {
            st.microvilliBoostTicks--;
        }

        // 2. 滚动蠕动波（哌嗪麻痹时完全停摆！）
        if (!st.isParalyzedByPiperazine()) {
            if (!st.peristalsisActive) {
                if (st.peristalsisCooldown > 0) {
                    st.peristalsisCooldown--;
                } else {
                    // 有食糜团时强消化蠕动；空腹低糖原时触发空腹移行复合波（MMC）并伴随肠鸣音
                    boolean hasBolus = !st.boluses.isEmpty();
                    boolean hungryMmc = !hasBolus && st.isHypoglycemic();
                    st.peristalsisActive = true;
                    st.hungerMmcWave = hungryMmc;
                    st.peristalsisX = -36.0;
                    st.peristalsisCooldown = hasBolus ? 45 : (hungryMmc ? 85 : 110);
                    if (hungryMmc) {
                        bodyLevel.playSound(null, new BlockPos(cx, AscarisAnatomyBuilder.GUT_FLOOR_Y + 2, cz),
                                JRSounds.ASCARIS_RUMBLE, SoundSource.AMBIENT, 0.72f, 0.92f);
                    }
                }
            } else {
                double step = st.hungerMmcWave ? 0.58 : 0.78;
                double prevX = st.peristalsisX;
                st.peristalsisX += step;

                // 仅在有访客靠近时绘制环形蠕动收缩粒子浪（性能极佳）
                double waveWorldX = cx + st.peristalsisX;
                if (gameTime % 2 == 0 && Math.abs(visitor.getX() - waveWorldX) <= 18.0) {
                    double cy = AscarisAnatomyBuilder.GUT_FLOOR_Y + 2.1;
                    DustParticleOptions ringDust = st.hungerMmcWave ? PAIN_CRIMSON : PINK_DUST;
                    for (int k = 0; k < 8; k++) {
                        double ang = k * (Math.PI / 4.0) + st.peristalsisX * 0.25;
                        bodyLevel.sendParticles((k % 2 == 0) ? ringDust : PEARL_DUST,
                                waveWorldX, cy + Math.sin(ang) * 1.35, cz + 0.5 + Math.cos(ang) * 2.75,
                                1, 0.06, 0.06, 0.06, 0.01);
                    }
                }

                // 蠕动波扫过每一枚食糜团 -> 真实推动食糜沿肠道向肛门前进！
                for (AscarisPhysiologyState.ChymeBolus b : st.boluses) {
                    if (b.rx >= prevX - 1.5 && b.rx <= st.peristalsisX + 1.5) {
                        b.rx += st.hungerMmcWave ? 4.2 : 3.2;
                    }
                }

                if (st.peristalsisX >= 41.0) {
                    st.peristalsisActive = false;
                    st.hungerMmcWave = false;
                }
            }
        }

        // 3. 移动食糜团渲染 + 肠绒毛刷状缘真实吸收 + 到达直肠转残渣
        Iterator<AscarisPhysiologyState.ChymeBolus> it = st.boluses.iterator();
        while (it.hasNext()) {
            AscarisPhysiologyState.ChymeBolus b = it.next();
            if (!st.isParalyzedByPiperazine()) {
                // 基础微流漂移
                b.rx += 0.045;
            }
            double bx = cx + b.rx;
            double by = AscarisAnatomyBuilder.GUT_FLOOR_Y + 1.45;
            double bz = cz + 0.5 + b.rz * 1.1;

            // 绘制食糜团实体感核心微粒（按风味呈现粉蜜 / 荧光 / 金蜜团块）
            if (gameTime % 4 == 0 && Math.abs(visitor.getX() - bx) <= 28.0) {
                DustParticleOptions bolusDust = switch (b.flavor) {
                    case AscarisChymeBlock.FLAVOR_GLOW -> GLOW_BOLUS;
                    case AscarisChymeBlock.FLAVOR_HONEY_GOLD -> HONEY_BOLUS;
                    default -> PEACH_BOLUS;
                };
                int count = Math.max(2, Math.round(b.nutrients / 18.0f));
                bodyLevel.sendParticles(bolusDust, bx, by, bz, count, 0.35, 0.25, 0.35, 0.01);
                if (gameTime % 16 == 0) {
                    bodyLevel.sendParticles(ParticleTypes.BUBBLE_POP, bx, by + 0.35, bz, 1, 0.2, 0.15, 0.2, 0.01);
                }
            }

            // 在笔直肠道吸收段（x ∈ [-22, +30]）：按局部完好肠绒毛数量真实吸收营养！
            if (gameTime % 20 == 0 && b.rx >= -22.0 && b.rx <= 30.0 && b.nutrients > 0f) {
                float villiEff = measureLocalMicrovilliEfficiency(bodyLevel, slot, (int) Math.round(b.rx));
                if (st.microvilliBoostTicks > 0) villiEff = Math.min(1.4f, villiEff * 1.35f);
                if (st.isBlockedByAlbendazole()) {
                    // 阿苯达唑微管解聚：阻断 90% 肠上皮葡萄糖摄取！
                    villiEff *= 0.10f;
                    bodyLevel.sendParticles(ALBENDAZOLE_ASH, bx, by + 0.3, bz, 4, 0.5, 0.2, 1.4, 0.01);
                }
                float absorbed = Math.min(b.nutrients, 6.5f * villiEff);
                if (absorbed > 0.2f) {
                    b.nutrients -= absorbed;
                    float glycogenMult = (b.flavor == AscarisChymeBlock.FLAVOR_HONEY_GOLD) ? 1.55f : 1.0f;
                    st.glycogen = Math.min(100f, st.glycogen + absorbed * 0.52f * glycogenMult);
                    st.metabolicWaste = Math.min(100f, st.metabolicWaste + absorbed * 0.16f);

                    // 实时同步吸收后的营养给外界蛔虫娘！
                    int hungerGain = Math.max(1, Math.round(absorbed * 0.35f));
                    BodyWorld.adjustHostStats(bodyLevel.getServer(), visitor, visit, 0, hungerGain, 0);
                    if (ascaris != null && ascaris.isAlive()) {
                        ascaris.heal(b.flavor == AscarisChymeBlock.FLAVOR_HONEY_GOLD ? 1.2f : 0.45f);
                    }
                    if (b.flavor == AscarisChymeBlock.FLAVOR_HONEY_GOLD && st.temperatureC > 37.5f) {
                        st.temperatureC -= 0.12f;
                    }
                    // 肠壁微绒毛发光吸收反馈粒子
                    for (int side : new int[] {-1, 1}) {
                        bodyLevel.sendParticles(b.flavor == AscarisChymeBlock.FLAVOR_GLOW ? GLOW_BOLUS : PEARL_DUST,
                                bx, by + 0.15, cz + 0.5 + side * 2.8, 3, 0.3, 0.25, 0.2, 0.015);
                    }
                }
            }

            // 食糜团抵达直肠（x >= 33）：残余未吸收物质转化为直肠充盈度！
            if (b.rx >= 33.0) {
                float residue = 10.0f + b.nutrients * 0.35f;
                st.rectalLoad = Math.min(100f, st.rectalLoad + residue);
                it.remove();
            }
        }

        // 4. 直肠充盈满载 -> 生理性排空反射波（Rectal Evacuation Reflex）
        if (st.rectalLoad >= 85.0f && !st.isParalyzedByPiperazine() && gameTime % 20 == 0) {
            st.rectalLoad = 12.0f;
            st.metabolicWaste = Math.max(0f, st.metabolicWaste - 8.0f);
            BlockPos rectumPos = new BlockPos(cx + 38, AscarisAnatomyBuilder.GUT_FLOOR_Y + 2, cz);
            bodyLevel.playSound(null, rectumPos, JRSounds.SLIME_SWALLOW, SoundSource.AMBIENT, 0.75f, 0.85f);
            bodyLevel.sendParticles(PEACH_BOLUS, cx + 38.5, AscarisAnatomyBuilder.GUT_FLOOR_Y + 1.8, cz + 0.5,
                    18, 2.5, 0.6, 1.0, 0.04);
            st.launchImpulse(38.0, -1, AscarisPhysiologyState.NerveImpulse.KIND_REFLEX, "rectum_twitch");
            if (ascaris != null && ascaris.isAlive() && ascaris.level() instanceof ServerLevel hostLevel) {
                // 外界伴随柔和尾尖抖动与极少量粉白代谢碎屑
                ascaris.triggerInternalOrganReflex("rectum_twitch");
                hostLevel.sendParticles(PEARL_DUST, ascaris.getX(), ascaris.getY() + 0.3, ascaris.getZ(),
                        10, 0.3, 0.15, 0.3, 0.03);
            }
            // 若访客正站在直肠里且没有潜行抓紧肠壁，会被排空波强力推向肛门括约肌！
            if (AscarisBodyInteractions.detectZone(slot, visitor.getX(), visitor.getY(), visitor.getZ())
                    == AscarisBodyInteractions.OrganZone.RECTUM && !visitor.isShiftKeyDown()) {
                visitor.push(0.68, 0.08, 0.0);
                visitor.hurtMarked = true;
                AscarisBodyInteractions.silenceZoneHudPublic(visitor, 65);
                visitor.sendOverlayMessage(Component.literal("§d尾端直肠触发了排空反射波，一股滑溜的推力把你推向了肛门括约肌！"));
            }
        }
    }

    /** 静水骨骼液压、体壁破口微渗漏与 H 形排泄管渗透压稳态 */
    private static void tickHydrostaticAndExcretory(ServerLevel bodyLevel, long slot, AscarisPhysiologyState st,
                                                    @Nullable AscarisEntity ascaris, long gameTime) {
        int cx = BodyRegionData.centerX(slot);
        int cz = BodyRegionData.centerZ(slot);

        // 1. 定期检查 H 形排泄管横桥方块完好度（挖断横桥会真实降低代谢排毒速率！）
        if (gameTime % 40 == 0) {
            st.lastExcretoryEfficiency = measureExcretoryBridgeEfficiency(bodyLevel, slot);
        }

        // 2. 自动清理已被玩家修复/填平的体壁破口，并对仍敞开的破口产生体腔液渗漏
        if (gameTime % 20 == 0 && !st.breaches.isEmpty()) {
            Iterator<AscarisPhysiologyState.TissueBreach> it = st.breaches.iterator();
            while (it.hasNext()) {
                AscarisPhysiologyState.TissueBreach br = it.next();
                BlockPos pos = br.worldPos(slot);
                if (!bodyLevel.getBlockState(pos).isAir()) {
                    it.remove();
                    continue;
                }
                // 破口渗出高液压假体腔液珠！
                bodyLevel.sendParticles(HEMOLYMPH_LEAK,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        3, 0.25, 0.25, 0.25, 0.04);
            }
        }

        // 3. 静水压动态平衡（正常 ~64%，疾跑/缠绕飙升至 ~88%，体壁破口渗漏或哌嗪麻痹暴跌！）
        if (gameTime % 10 == 0) {
            float targetPressure = switch (st.hostActivity) {
                case AscarisPhysiologyState.ACTIVITY_WRAP -> 90.0f;
                case AscarisPhysiologyState.ACTIVITY_RUN_JUMP -> 82.0f;
                case AscarisPhysiologyState.ACTIVITY_CRAWL -> 74.0f;
                case AscarisPhysiologyState.ACTIVITY_HURT_CURL -> 78.0f;
                case AscarisPhysiologyState.ACTIVITY_WALK -> 70.0f;
                case AscarisPhysiologyState.ACTIVITY_SLEEP -> 54.0f;
                case AscarisPhysiologyState.ACTIVITY_COLLAPSE -> 34.0f;
                default -> 64.0f;
            };
            if (st.isParalyzedByPiperazine()) {
                targetPressure = 22.0f;
            } else {
                // 体壁破口导致静水骨骼漏压
                targetPressure -= Math.min(36.0f, st.breaches.size() * 4.5f);
                if (st.glycogen < 18.0f) targetPressure -= 10.0f;
            }
            st.hydrostaticPressure = Mth.lerp(0.10f, st.hydrostaticPressure, Mth.clamp(targetPressure, 12f, 98f));

            // 4. H 形排泄管持续渗透清除代谢废物
            float clearRate = 0.22f * st.lastExcretoryEfficiency;
            if (st.isParalyzedByPiperazine()) clearRate *= 0.45f;
            st.metabolicWaste = Math.max(0f, st.metabolicWaste - clearRate);
        }

        // 5. 高代谢负荷（酸中毒）对外部实体与排泄横桥的直观反馈
        if (gameTime % 80 == 0 && st.hasAcidosis()) {
            bodyLevel.sendParticles(TOXIN_MIST,
                    cx + AscarisAnatomyBuilder.EXCRETORY_BRIDGE_X + 0.5,
                    AscarisAnatomyBuilder.FLOOR_Y + 1.2,
                    cz + 0.5, 10, 1.5, 0.5, 6.0, 0.02);
            if (ascaris != null && ascaris.isAlive()) {
                ascaris.triggerInternalOrganReflex("acidosis_fatigue");
            }
        }
    }

    /** 双管雌性生殖生理链：盘绕卵巢造卵 → 输卵管/受精囊粒子流 → 双子宫从后向前生长卵囊 → 阴道宫缩产卵 */
    private static void tickReproductivePipeline(ServerLevel bodyLevel, long slot, AscarisPhysiologyState st,
                                                 @Nullable AscarisEntity ascaris, long gameTime) {
        int cx = BodyRegionData.centerX(slot);
        int cz = BodyRegionData.centerZ(slot);

        if (st.ovipositionCooldown > 0) {
            st.ovipositionCooldown--;
        }

        // 1. 盘绕卵巢阶段（消耗少量糖原合成卵母细胞；阿苯达唑毒发或低血糖时暂停）
        if (gameTime % 20 == 0 && !st.isBlockedByAlbendazole() && st.glycogen >= 35.0f && !st.ovipositing) {
            float gain = st.glycogen >= 70.0f ? 2.4f : 1.3f;
            st.ovaryProgress += gain;
            st.glycogen -= 0.15f;

            // 2. 卵巢满 100% -> 沿左右输卵管向受精囊与双管子宫输送成熟受精卵！
            if (st.ovaryProgress >= 100.0f) {
                st.ovaryProgress = 0.0f;
                triggerOviductTransfer(bodyLevel, slot, st);
            }
        }

        // 3. 双子宫满载 (>= 85%) -> 启动产道宫缩产卵波！
        if (!st.ovipositing && st.uterineLoad >= 85.0f && st.ovipositionCooldown == 0 && !st.isParalyzedByPiperazine()) {
            st.ovipositing = true;
            st.ovipositionX = 12.0;
            st.ovipositionCooldown = 600;
            bodyLevel.playSound(null, new BlockPos(cx - 10, AscarisAnatomyBuilder.FLOOR_Y + 2, cz),
                    JRSounds.ASCARIS_SLITHER, SoundSource.AMBIENT, 0.65f, 1.15f);
        }

        // 4. 执行阴道/子宫产卵宫缩推进（x: +12 → -17）
        if (st.ovipositing) {
            if (st.isParalyzedByPiperazine()) {
                st.ovipositing = false;
                return;
            }
            st.ovipositionX -= 0.45;
            if (gameTime % 3 == 0) {
                double wx = cx + st.ovipositionX;
                if (st.ovipositionX >= -6.0) {
                    // 左右双管子宫同步蠕动推进
                    for (int side : new int[] {-1, 1}) {
                        bodyLevel.sendParticles(PEARL_DUST, wx, AscarisAnatomyBuilder.FLOOR_Y + 2.2,
                                cz + side * 9.0, 5, 0.4, 0.6, 1.0, 0.015);
                    }
                } else {
                    // 汇入 Y 形分叉与肌质阴道
                    bodyLevel.sendParticles(PEARL_DUST, wx, AscarisAnatomyBuilder.FLOOR_Y + 2.0,
                            cz + 0.5, 7, 0.4, 0.6, 1.1, 0.02);
                    bodyLevel.sendParticles(PINK_DUST, wx, AscarisAnatomyBuilder.FLOOR_Y + 1.8,
                            cz + 0.5, 4, 0.4, 0.4, 0.8, 0.02);
                }
            }

            if (st.ovipositionX <= -17.0) {
                st.ovipositing = false;
                st.uterineLoad = Math.max(15.0f, st.uterineLoad - 35.0f);
                // 消耗最前端 1 枚成熟卵囊并在阴门产出温热的蛔虫卵胶囊
                consumeAnteriorEggSac(bodyLevel, slot);
                ItemEntity capsule = new ItemEntity(bodyLevel,
                        cx - 16.5, AscarisAnatomyBuilder.FLOOR_Y + 1.3, cz + 0.5,
                        new ItemStack(JRItems.Companion.getASCARIS_EGG_CAPSULE(), 1));
                capsule.setDeltaMovement(-0.08, 0.14, 0.0);
                bodyLevel.addFreshEntity(capsule);
                bodyLevel.playSound(null, new BlockPos(cx - 16, AscarisAnatomyBuilder.FLOOR_Y + 2, cz),
                        SoundEvents.SLIME_SQUISH_SMALL, SoundSource.BLOCKS, 0.75f, 1.25f);
                bodyLevel.sendParticles(PEARL_DUST, cx - 16.5, AscarisAnatomyBuilder.FLOOR_Y + 1.6, cz + 0.5,
                        16, 0.5, 0.4, 0.5, 0.03);
                if (ascaris != null && ascaris.isAlive()) {
                    ascaris.triggerInternalOrganReflex("oviposition");
                }
            }
        }
    }

    /** 卵母细胞沿左右输卵管从卵巢(x=21)输送至受精囊与双子宫，并按从后到前解剖顺序生成卵囊块 */
    public static void triggerOviductTransfer(ServerLevel bodyLevel, long slot, AscarisPhysiologyState st) {
        int cx = BodyRegionData.centerX(slot);
        int cz = BodyRegionData.centerZ(slot);
        for (int side : new int[] {-1, 1}) {
            for (int rx = 21; rx >= 13; rx -= 2) {
                bodyLevel.sendParticles(PEARL_DUST, cx + rx + 0.5, AscarisAnatomyBuilder.FLOOR_Y + 2.0,
                        cz + side * 9 + 0.5, 4, 0.3, 0.3, 0.3, 0.02);
            }
        }
        st.uterineLoad = Math.min(100.0f, st.uterineLoad + 22.0f);
        growUterineEggSacPosteriorToAnterior(bodyLevel, slot);
    }

    /** 按真实解剖流向（后段输卵管端 x=+12 → 前段阴门端 x=-4）在左右双子宫内壁生长 1 块卵囊 */
    public static boolean growUterineEggSacPosteriorToAnterior(ServerLevel bodyLevel, long slot) {
        int cx = BodyRegionData.centerX(slot);
        int cz = BodyRegionData.centerZ(slot);
        int y = AscarisAnatomyBuilder.FLOOR_Y + 1;
        for (int rx : UTERINE_EGG_POSTERIOR_TO_ANTERIOR) {
            for (int side : new int[] {-1, 1}) {
                BlockPos pos = new BlockPos(cx + rx, y, cz + side * 11);
                if (bodyLevel.getBlockState(pos).isAir()) {
                    bodyLevel.setBlock(pos, JRBlocks.ASCARIS_EGG_SAC.defaultBlockState(), Block.UPDATE_ALL);
                    bodyLevel.sendParticles(PEARL_DUST, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            10, 0.25, 0.25, 0.25, 0.02);
                    return true;
                }
            }
        }
        return false;
    }

    private static void consumeAnteriorEggSac(ServerLevel bodyLevel, long slot) {
        int cx = BodyRegionData.centerX(slot);
        int cz = BodyRegionData.centerZ(slot);
        int y = AscarisAnatomyBuilder.FLOOR_Y + 1;
        // 优先消耗靠阴门的前段成熟卵囊
        for (int i = UTERINE_EGG_POSTERIOR_TO_ANTERIOR.length - 1; i >= 0; i--) {
            int rx = UTERINE_EGG_POSTERIOR_TO_ANTERIOR[i];
            for (int side : new int[] {-1, 1}) {
                BlockPos pos = new BlockPos(cx + rx, y, cz + side * 11);
                if (bodyLevel.getBlockState(pos).is(JRBlocks.ASCARIS_EGG_SAC)) {
                    bodyLevel.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    return;
                }
            }
        }
    }

    /** 生物电动作电位逐格沿背/腹/肠道神经索传导，并在传入围咽神经环时触发外界生理反射 */
    private static void tickNerveImpulses(ServerLevel bodyLevel, long slot, AscarisPhysiologyState st,
                                          @Nullable AscarisEntity ascaris, long gameTime) {
        if (st.impulses.isEmpty()) return;
        if (st.isParalyzedByPiperazine()) {
            st.impulses.clear();
            return;
        }
        int cx = BodyRegionData.centerX(slot);
        int cz = BodyRegionData.centerZ(slot);
        int dorsalY = AscarisAnatomyBuilder.CY + 14;
        int ventralY = AscarisAnatomyBuilder.FLOOR_Y;
        int gutY = AscarisAnatomyBuilder.GUT_FLOOR_Y;

        Iterator<AscarisPhysiologyState.NerveImpulse> it = st.impulses.iterator();
        while (it.hasNext()) {
            AscarisPhysiologyState.NerveImpulse imp = it.next();
            double prevRx = imp.rx;
            imp.rx += imp.direction * 2.5;
            imp.ttl--;

            int curRx = (int) Math.round(imp.rx);
            DustParticleOptions spark = (imp.kind == AscarisPhysiologyState.NerveImpulse.KIND_PAIN) ? PAIN_CRIMSON : NERVE_GOLD;

            // 点亮沿途真实神经索方块（背神经线 / 腹神经线 / 肠道神经线）
            int[] ys = {ventralY, gutY, dorsalY};
            for (int y : ys) {
                for (int dx = -1; dx <= 1; dx++) {
                    BlockPos p = new BlockPos(cx + curRx + dx, y, cz);
                    BlockState bs = bodyLevel.getBlockState(p);
                    if (bs.is(JRBlocks.ASCARIS_NERVE_CORD)) {
                        if (!bs.getValue(AscarisNerveCordBlock.EXCITED)) {
                            bodyLevel.setBlock(p, bs.setValue(AscarisNerveCordBlock.EXCITED, true), Block.UPDATE_CLIENTS);
                            bodyLevel.scheduleTick(p, JRBlocks.ASCARIS_NERVE_CORD, 8);
                        }
                        bodyLevel.sendParticles(spark, p.getX() + 0.5, p.getY() + 0.6, p.getZ() + 0.5,
                                2, 0.15, 0.15, 0.15, 0.01);
                    }
                }
            }

            // 传入神经抵达围咽神经环（x = -31）-> 触发神经中枢反射弧！
            boolean crossedRing = (prevRx > AscarisAnatomyBuilder.NERVE_RING_X && imp.rx <= AscarisAnatomyBuilder.NERVE_RING_X)
                    || (prevRx < AscarisAnatomyBuilder.NERVE_RING_X && imp.rx >= AscarisAnatomyBuilder.NERVE_RING_X);
            if (crossedRing && imp.direction < 0) {
                BlockPos ringTop = new BlockPos(cx + AscarisAnatomyBuilder.NERVE_RING_X, AscarisAnatomyBuilder.GUT_FLOOR_Y + 5, cz);
                BlockState ringState = bodyLevel.getBlockState(ringTop);
                if (ringState.is(JRBlocks.ASCARIS_NERVE_CORD)) {
                    bodyLevel.setBlock(ringTop, ringState.setValue(AscarisNerveCordBlock.EXCITED, true), Block.UPDATE_CLIENTS);
                    bodyLevel.scheduleTick(ringTop, JRBlocks.ASCARIS_NERVE_CORD, 14);
                }
                bodyLevel.sendParticles(spark, ringTop.getX() + 0.5, ringTop.getY() - 0.5, ringTop.getZ() + 0.5,
                        10, 1.2, 1.2, 1.2, 0.03);
                if (ascaris != null && ascaris.isAlive() && !imp.reflexTag.isEmpty()) {
                    ascaris.triggerInternalOrganReflex(imp.reflexTag);
                }
                it.remove();
                continue;
            }

            if (imp.ttl <= 0 || imp.rx < -45.0 || imp.rx > 44.0) {
                it.remove();
            }
        }
    }

    /** 将体内实时生理波、咽部抽吸、静水压、酸中毒与发烧反馈作用到访客身上 */
    private static void applyPhysiologyEffectsToVisitor(ServerLevel bodyLevel, long slot, AscarisPhysiologyState st,
                                                        ServerPlayer visitor, BodyRegionData.Visit visit, long gameTime) {
        AscarisBodyInteractions.OrganZone zone = AscarisBodyInteractions.detectZone(slot, visitor.getX(), visitor.getY(), visitor.getZ());
        int cx = BodyRegionData.centerX(slot);

        // 1. 咽部抽吸泵物理吸力（向肠道深处 +X 吸入，潜行可抓壁抵抗）
        if (st.pharynxSuctionTicks > 0 && (zone == AscarisBodyInteractions.OrganZone.LIPS || zone == AscarisBodyInteractions.OrganZone.PHARYNX)
                && !visitor.isShiftKeyDown() && gameTime % 5 == 0) {
            visitor.push(0.22, 0.0, 0.0);
            visitor.hurtMarked = true;
        }

        // 2. 真实滚动蠕动波扫过玩家坐标时的推送（按住潜行可抓紧肠壁抵抗！）
        if (st.peristalsisActive && (zone == AscarisBodyInteractions.OrganZone.INTESTINE || zone == AscarisBodyInteractions.OrganZone.PHARYNX)
                && !visitor.isShiftKeyDown()) {
            double playerRx = visitor.getX() - cx;
            if (Math.abs(playerRx - st.peristalsisX) <= 1.2) {
                visitor.push(st.hungerMmcWave ? 0.34 : 0.24, 0.0, 0.0);
                visitor.hurtMarked = true;
            }
        }

        // 3. 产道宫缩波扫过子宫/阴道时的柔和推进（推向阴门 -X）
        if (st.ovipositing && (zone == AscarisBodyInteractions.OrganZone.UTERUS || zone == AscarisBodyInteractions.OrganZone.VAGINA)
                && !visitor.isShiftKeyDown()) {
            double playerRx = visitor.getX() - cx;
            if (Math.abs(playerRx - st.ovipositionX) <= 1.5) {
                visitor.push(-0.22, 0.0, 0.0);
                visitor.hurtMarked = true;
            }
        }

        // 4. 发光浆果食糜团经过玩家身边 -> 短暂微光夜视滋养
        if (gameTime % 20 == 0 && zone == AscarisBodyInteractions.OrganZone.INTESTINE) {
            double playerRx = visitor.getX() - cx;
            for (AscarisPhysiologyState.ChymeBolus b : st.boluses) {
                if (Math.abs(playerRx - b.rx) <= 2.5 && b.flavor == AscarisChymeBlock.FLAVOR_GLOW) {
                    visitor.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, true, false, false));
                    break;
                }
            }
        }

        // 5. 静水压失压（体壁破损漏液或哌嗪麻痹） -> 假体腔失弹变黏滞；酸中毒 -> 轻度虚弱雾气；炎症发热 -> 蒸气微粒
        if (gameTime % 40 == 0) {
            if (st.hydrostaticPressure <= 34.0f && zone == AscarisBodyInteractions.OrganZone.PSEUDOCOELOM) {
                visitor.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 50, 0, true, false, false));
            }
            if (st.hasAcidosis()) {
                visitor.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, true, false, false));
                bodyLevel.sendParticles(TOXIN_MIST,
                        visitor.getX() + (visitor.getRandom().nextDouble() - 0.5) * 4.0,
                        visitor.getY() + 1.0,
                        visitor.getZ() + (visitor.getRandom().nextDouble() - 0.5) * 4.0,
                        4, 0.4, 0.3, 0.4, 0.01);
            }
            if (st.hasFever()) {
                bodyLevel.sendParticles(FEVER_STEAM,
                        visitor.getX() + (visitor.getRandom().nextDouble() - 0.5) * 4.5,
                        visitor.getY() + 1.2,
                        visitor.getZ() + (visitor.getRandom().nextDouble() - 0.5) * 4.5,
                        4, 0.4, 0.3, 0.4, 0.02);
            }
        }
    }

    // ===== 器官级事件入口（供方块、互动、命令与外界实体调用） =====

    /** 外界或咽部吞入甜食 -> 启动咽部负压抽吸泵 + 生成真正沿肠道推进的食糜团 + 发出吞咽神经脉冲！ */
    public static void onFoodSwallowed(ServerLevel bodyLevel, long slot, @Nullable AscarisEntity ascaris, int flavor) {
        BodyRegionData data = BodyRegionData.get(bodyLevel.getServer());
        AscarisPhysiologyState st = data.physiologyFor(slot);
        int lane = bodyLevel.getRandom().nextInt(3) - 1;
        st.addBolus(-36.0, lane, flavor, flavor == AscarisChymeBlock.FLAVOR_HONEY_GOLD ? 100f : 85f);
        st.pharynxSuctionTicks = 40;
        st.peristalsisCooldown = Math.min(st.peristalsisCooldown, 12);
        st.glycogen = Math.min(100f, st.glycogen + (flavor == AscarisChymeBlock.FLAVOR_HONEY_GOLD ? 14f : 8f));
        st.launchImpulse(AscarisAnatomyBuilder.NERVE_RING_X, +1, AscarisPhysiologyState.NerveImpulse.KIND_MOTOR, "pharynx_swallow");
        if (ascaris != null && ascaris.isAlive()) {
            ascaris.triggerInternalOrganReflex("pharynx_swallow");
        }
        int cx = BodyRegionData.centerX(slot);
        int cz = BodyRegionData.centerZ(slot);
        bodyLevel.playSound(null, new BlockPos(cx - 35, AscarisAnatomyBuilder.GUT_FLOOR_Y + 2, cz),
                JRSounds.SLIME_SWALLOW, SoundSource.AMBIENT, 0.8f, 1.1f);
        data.markPhysiologyDirty();
    }

    /** 触发体内双药药理发作（宝塔糖=哌嗪神经肌肉麻痹；阿苯达唑=微管解聚与糖摄取阻断） */
    public static void administerInternalDrug(ServerLevel bodyLevel, long slot, @Nullable AscarisEntity ascaris,
                                              int drugType, int durationTicks) {
        BodyRegionData data = BodyRegionData.get(bodyLevel.getServer());
        AscarisPhysiologyState st = data.physiologyFor(slot);
        st.drugType = drugType;
        st.drugTicks = Math.max(0, durationTicks);
        if (drugType == AscarisPhysiologyState.DRUG_PIPERAZINE) {
            st.impulses.clear();
            st.peristalsisActive = false;
            st.hydrostaticPressure = Math.min(st.hydrostaticPressure, 26.0f);
            if (ascaris != null && ascaris.isAlive()) {
                ascaris.setExpelledStage(AscarisEntity.EXPELLED_PAGODA_WEAK);
                if (!ascaris.getPoseState().isWrapping()) {
                    ascaris.setPoseState(AscarisPoseState.COLLAPSE);
                }
            }
        } else if (drugType == AscarisPhysiologyState.DRUG_ALBENDAZOLE) {
            st.metabolicWaste = Math.min(100f, st.metabolicWaste + 25f);
            st.temperatureC = Math.max(st.temperatureC, 38.7f);
            if (ascaris != null && ascaris.isAlive()) {
                ascaris.setDormantDowned(false);
                ascaris.setExpelledStage(AscarisEntity.EXPELLED_ALBENDAZOLE_CRITICAL);
                ascaris.setAlbendazoleToxicityTicks(Math.max(ascaris.getAlbendazoleToxicityTicks(), 160));
            }
        }
        data.markPhysiologyDirty();
    }

    /** 右键 H 形排泄管膜：不仅净化访客，也真正疏通宿主排泄孔、清代谢废物与药毒，甚至可从体内救活阿苯达唑危重宿主！ */
    public static void onExcretoryFlushed(ServerLevel bodyLevel, long slot, @Nullable AscarisEntity ascaris, ServerPlayer visitor) {
        BodyRegionData data = BodyRegionData.get(bodyLevel.getServer());
        AscarisPhysiologyState st = data.physiologyFor(slot);

        float prevWaste = st.metabolicWaste;
        int prevDrug = st.drugType;
        st.metabolicWaste = Math.max(0f, st.metabolicWaste - 32.0f);
        if (st.temperatureC > 37.3f) {
            st.temperatureC = Math.max(37.2f, st.temperatureC - 0.85f);
        }
        boolean drugCleared = false;
        if (st.drugType != AscarisPhysiologyState.DRUG_NONE && st.drugTicks > 0) {
            st.drugTicks = Math.max(0, st.drugTicks - 480);
            if (st.drugTicks == 0) {
                st.drugType = AscarisPhysiologyState.DRUG_NONE;
                drugCleared = true;
            }
        }

        boolean rescued = false;
        if (ascaris != null && ascaris.isAlive()) {
            ascaris.triggerInternalOrganReflex("excretory_relief");
            if (ascaris.getAlbendazoleToxicityTicks() > 0) {
                ascaris.setAlbendazoleToxicityTicks(Math.max(0, ascaris.getAlbendazoleToxicityTicks() - 90));
            }
            if (drugCleared || (st.drugType == AscarisPhysiologyState.DRUG_NONE && st.glycogen >= 30f)) {
                rescued = ascaris.recoverFromInternalRescue();
            }
        }

        int cx = BodyRegionData.centerX(slot);
        int cz = BodyRegionData.centerZ(slot);
        bodyLevel.sendParticles(AQUA_EXCRETORY,
                cx + AscarisAnatomyBuilder.EXCRETORY_BRIDGE_X + 0.5,
                AscarisAnatomyBuilder.FLOOR_Y + 1.0,
                cz + 0.5, 18, 0.8, 0.5, 6.0, 0.04);
        data.markPhysiologyDirty();
        sendPhysiologySync(visitor, st);

        AscarisBodyInteractions.silenceZoneHudPublic(visitor, 75);
        if (rescued) {
            visitor.sendSystemMessage(Component.literal(
                    "§a✦ 你疏通了 H 形排泄管横桥、排净了残留药毒，外界垂危的粉白蛔虫娘恢复了意识~♡"));
        } else if (drugCleared) {
            visitor.sendOverlayMessage(Component.literal(
                    "§bH 形排泄管强力冲洗排净了" + (prevDrug == AscarisPhysiologyState.DRUG_ALBENDAZOLE ? "阿苯达唑" : "哌嗪")
                            + "药毒，内环境稳态恢复！"));
        } else if (prevDrug != AscarisPhysiologyState.DRUG_NONE) {
            visitor.sendOverlayMessage(Component.literal(
                    "§b借助 H 形排泄管加速了药毒渗透排出（剩余药效 " + (st.drugTicks / 20) + " 秒 · 代谢负荷 " + Math.round(st.metabolicWaste) + "%）"));
        } else if (prevWaste >= 20f) {
            visitor.sendOverlayMessage(Component.literal(
                    "§b疏通了 H 形排泄管横桥，宿主代谢负荷降至 §f" + Math.round(st.metabolicWaste)
                            + "% §7（排泄管完整度 " + Math.round(st.lastExcretoryEfficiency * 100) + "%）"));
        }
    }

    /** 破坏体内组织方块：记录体壁破口、漏压、发射红色痛觉神经脉冲冲向围咽神经环！ */
    public static void onTissueBreached(ServerLevel bodyLevel, long slot, BlockPos pos, BlockState brokenState) {
        BodyRegionData data = BodyRegionData.get(bodyLevel.getServer());
        AscarisPhysiologyState st = data.physiologyFor(slot);
        int rx = pos.getX() - BodyRegionData.centerX(slot);
        int rz = pos.getZ() - BodyRegionData.centerZ(slot);
        int organKind = classifyOrgan(brokenState);
        String blockId = BuiltInRegistries.BLOCK.getKey(brokenState.getBlock()).toString();
        st.recordBreach(rx, pos.getY(), rz, organKind, blockId);
        st.hydrostaticPressure = Math.max(12.0f, st.hydrostaticPressure - 3.5f);
        st.temperatureC = Math.min(40.2f, st.temperatureC + 0.18f);
        st.launchImpulse(rx, rx > AscarisAnatomyBuilder.NERVE_RING_X ? -1 : +1,
                AscarisPhysiologyState.NerveImpulse.KIND_PAIN, "");
        data.markPhysiologyDirty();
    }

    /** 器官大面积自愈完成后清空已修复的破口记录 */
    public static void syncBreachesAfterHeal(ServerLevel bodyLevel, long slot) {
        BodyRegionData data = BodyRegionData.get(bodyLevel.getServer());
        AscarisPhysiologyState st = data.physiologyFor(slot);
        st.breaches.removeIf(br -> !bodyLevel.getBlockState(br.worldPos(slot)).isAir());
        data.markPhysiologyDirty();
    }

    /** 体腔免疫小细胞（Coelomocyte）自主凝集修补体内微小组织破口（只填补空气，绝不碰玩家箱子与装饰） */
    public static boolean advanceCoelomocyteRepair(ServerLevel bodyLevel, long slot,
                                                   AscarisPhysiologyState.TissueBreach breach, int delta) {
        BodyRegionData data = BodyRegionData.get(bodyLevel.getServer());
        AscarisPhysiologyState st = data.physiologyFor(slot);
        if (st.glycogen < 15.0f || st.isBlockedByAlbendazole()) {
            return false;
        }
        BlockPos pos = breach.worldPos(slot);
        if (!bodyLevel.getBlockState(pos).isAir()) {
            st.breaches.remove(breach);
            data.markPhysiologyDirty();
            return true;
        }
        breach.repairProgress += delta;
        st.glycogen = Math.max(0f, st.glycogen - 0.35f);
        bodyLevel.sendParticles(PEARL_DUST, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                5, 0.25, 0.25, 0.25, 0.02);
        if (breach.repairProgress >= 100) {
            BlockState restored = resolveRestoredBlockState(breach.blockId);
            bodyLevel.setBlock(pos, restored, Block.UPDATE_ALL);
            st.breaches.remove(breach);
            bodyLevel.playSound(null, pos, SoundEvents.SLIME_SQUISH_SMALL, SoundSource.BLOCKS, 0.55f, 1.2f);
            bodyLevel.sendParticles(ParticleTypes.HEART, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5,
                    3, 0.25, 0.25, 0.25, 0.01);
            data.markPhysiologyDirty();
            return true;
        }
        return false;
    }

    /** 空手轻抚/刺激盘绕卵巢组织 -> 沿神经索传回神经环（外界害羞轻颤）+ 推进排卵周期 */
    public static void onOvaryStimulated(ServerLevel bodyLevel, long slot, @Nullable AscarisEntity ascaris,
                                         ServerPlayer visitor, BlockPos pos) {
        BodyRegionData data = BodyRegionData.get(bodyLevel.getServer());
        AscarisPhysiologyState st = data.physiologyFor(slot);
        int rx = pos.getX() - BodyRegionData.centerX(slot);

        st.launchImpulse(rx, -1, AscarisPhysiologyState.NerveImpulse.KIND_REFLEX, "ovary_blush");
        bodyLevel.playSound(null, pos, SoundEvents.SLIME_SQUISH_SMALL, SoundSource.BLOCKS, 0.55f, 1.3f);
        bodyLevel.sendParticles(PEARL_DUST, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5,
                10, 0.35, 0.35, 0.35, 0.02);

        AscarisBodyInteractions.silenceZoneHudPublic(visitor, 65);
        if (st.isBlockedByAlbendazole()) {
            visitor.sendOverlayMessage(Component.literal("§c阿苯达唑阻断了细胞分裂，盘绕卵巢正处于应激停滞状态……"));
            return;
        }
        if (st.glycogen < 30.0f) {
            visitor.sendOverlayMessage(Component.literal("§e宿主糖原不足（" + Math.round(st.glycogen) + "%），卵巢暂时无力合成新卵母细胞，先去咽部喂点甜食吧~"));
            return;
        }
        st.glycogen -= 4.0f;
        st.ovaryProgress += 28.0f;
        if (st.ovaryProgress >= 100.0f) {
            st.ovaryProgress = 0.0f;
            triggerOviductTransfer(bodyLevel, slot, st);
            visitor.sendOverlayMessage(Component.literal(
                    "§d盘绕卵巢一阵轻颤！成熟卵母细胞正沿着输卵管涌入左右双子宫（子宫充盈 " + Math.round(st.uterineLoad) + "%）~♡"));
        } else {
            visitor.sendOverlayMessage(Component.literal(
                    "§d温热的丝状卵巢微微蠕动，卵母细胞成熟度推进至 §f" + Math.round(st.ovaryProgress) + "% §7（双子宫充盈 " + Math.round(st.uterineLoad) + "%）"));
        }
        data.markPhysiologyDirty();
        sendPhysiologySync(visitor, st);
    }

    /** 空手轻挠 Y 形三辐射肌肉咽 -> 机械反射弧触发抽吸或外界干呕打嗝！ */
    public static void onPharynxStimulated(ServerLevel bodyLevel, long slot, @Nullable AscarisEntity ascaris,
                                           ServerPlayer visitor, BlockPos pos) {
        BodyRegionData data = BodyRegionData.get(bodyLevel.getServer());
        AscarisPhysiologyState st = data.physiologyFor(slot);
        st.pharynxSuctionTicks = Math.max(st.pharynxSuctionTicks, 24);
        st.launchImpulse(pos.getX() - BodyRegionData.centerX(slot), -1,
                AscarisPhysiologyState.NerveImpulse.KIND_REFLEX, "pharynx_gag");
        bodyLevel.playSound(null, pos, JRSounds.SLIME_SWALLOW, SoundSource.BLOCKS, 0.65f, 1.25f);
        bodyLevel.sendParticles(PINK_DUST, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                10, 0.35, 0.35, 0.35, 0.02);
        AscarisBodyInteractions.silenceZoneHudPublic(visitor, 60);
        visitor.sendOverlayMessage(Component.literal(
                st.isParalyzedByPiperazine()
                        ? "§b咽部肌肉正处于哌嗪弛缓麻痹状态，毫无收缩反应……"
                        : "§d你轻挠了 Y 形三辐射肌肉咽，触发了吞咽反射弧（外界的她忍不住轻颤打了个小嗝~）"));
        data.markPhysiologyDirty();
    }

    /** 空手探查/轻抚肠绒毛：显示局部吸收效率并短暂激活刷状缘微绒毛吸收加速 */
    public static void onMicrovilliStimulated(ServerLevel bodyLevel, long slot, ServerPlayer visitor, BlockPos pos) {
        BodyRegionData data = BodyRegionData.get(bodyLevel.getServer());
        AscarisPhysiologyState st = data.physiologyFor(slot);
        int relX = pos.getX() - BodyRegionData.centerX(slot);
        float eff = measureLocalMicrovilliEfficiency(bodyLevel, slot, relX);
        st.microvilliBoostTicks = 160;
        bodyLevel.playSound(null, pos, SoundEvents.SLIME_SQUISH_SMALL, SoundSource.BLOCKS, 0.45f, 1.35f);
        bodyLevel.sendParticles(PEARL_DUST, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                8, 0.3, 0.3, 0.3, 0.02);
        AscarisBodyInteractions.silenceZoneHudPublic(visitor, 65);
        String blockHint = st.isBlockedByAlbendazole() ? " §c[阿苯达唑阻断中 -90%]" : " §a[刷状缘吸收已激活]";
        visitor.sendOverlayMessage(Component.literal(
                "§d局部肠绒毛完整度 §f" + Math.round(eff * 100) + "% §7| 活跃食糜团 §e" + st.boluses.size() + blockHint));
    }

    /** 外界粉白蛔虫娘受击 -> 体内即刻爆发痛觉神经脉冲、静水压骤升与短促警报！ */
    public static void onExternalHostHurt(AscarisEntity ascaris, float amount) {
        if (ascaris == null || !(ascaris.level() instanceof ServerLevel hostLevel)) return;
        MinecraftServer server = hostLevel.getServer();
        BodyRegionData data = BodyRegionData.get(server);
        UUID ownerUuid = ascaris.getBodyRegionOwnerUuid();
        Long slot = data.currentRegion(ownerUuid);
        if (slot == null || !data.isInitialized(slot)) return;
        ServerLevel bodyLevel = server.getLevel(BodyWorld.DIMENSION);
        if (bodyLevel == null) return;

        AscarisPhysiologyState st = data.physiologyFor(slot);
        st.hydrostaticPressure = Math.min(98.0f, st.hydrostaticPressure + Math.min(18f, amount * 4.5f));
        st.temperatureC = Math.min(40.0f, st.temperatureC + 0.25f);
        st.launchImpulse(AscarisAnatomyBuilder.NERVE_RING_X, +1, AscarisPhysiologyState.NerveImpulse.KIND_PAIN, "");
        data.markPhysiologyDirty();

        for (var entry : data.visits().entrySet()) {
            BodyRegionData.Visit v = entry.getValue();
            if (!v.isAscaris() || !v.host().equals(ownerUuid)) continue;
            ServerPlayer visitor = server.getPlayerList().getPlayer(entry.getKey());
            if (visitor == null || !visitor.level().dimension().equals(BodyWorld.DIMENSION)) continue;
            bodyLevel.playSound(null, visitor.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.6f, 1.05f);
            ServerPlayNetworking.send(visitor, new AscarisVisualEventPayload(
                    AscarisVisualEventPayload.EVENT_BELLY_RUMBLE, visitor.getId(), 3, 0));
            sendPhysiologySync(visitor, st);
            AscarisBodyInteractions.silenceZoneHudPublic(visitor, 55);
            visitor.sendOverlayMessage(Component.literal("§c外界的粉白蛔虫娘受到攻击！体壁纵肌猛地绷紧、静水压飙升至 " + st.pressureKpa() + " kPa！"));
        }
    }

    /** 外界粉白蛔虫娘触发安全重伤休眠保护 -> 通知正在体内世界的访客（体内世界安全无虞，且可直接从体内帮她疗伤唤醒） */
    public static void onExternalHostDormant(AscarisEntity ascaris) {
        if (ascaris == null || !(ascaris.level() instanceof ServerLevel hostLevel)) return;
        MinecraftServer server = hostLevel.getServer();
        BodyRegionData data = BodyRegionData.get(server);
        UUID ownerUuid = ascaris.getBodyRegionOwnerUuid();
        Long slot = data.currentRegion(ownerUuid);
        ServerLevel bodyLevel = server.getLevel(BodyWorld.DIMENSION);
        AscarisPhysiologyState st = (slot != null && data.isInitialized(slot)) ? data.physiologyFor(slot) : null;
        if (st != null) {
            st.hydrostaticPressure = Math.min(st.hydrostaticPressure, 42.0f);
            data.markPhysiologyDirty();
        }
        for (var entry : data.visits().entrySet()) {
            BodyRegionData.Visit v = entry.getValue();
            if (!v.isAscaris() || !v.host().equals(ownerUuid)) continue;
            ServerPlayer visitor = server.getPlayerList().getPlayer(entry.getKey());
            if (visitor == null || !visitor.isAlive()) continue;
            if (bodyLevel != null && visitor.level().dimension().equals(BodyWorld.DIMENSION)) {
                bodyLevel.playSound(null, visitor.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.55f, 0.85f);
                ServerPlayNetworking.send(visitor, new AscarisVisualEventPayload(
                        AscarisVisualEventPayload.EVENT_BELLY_RUMBLE, visitor.getId(), 2, 0));
                if (st != null) {
                    sendPhysiologySync(visitor, st);
                }
                AscarisBodyInteractions.silenceZoneHudPublic(visitor, 85);
            }
            visitor.sendSystemMessage(Component.literal(
                    "§d外界的粉白蛔虫娘遭遇重创，已收缩角质层进入安全休眠保护（免疫常规伤害）！§7（体内世界安全无虞，在咽部喂甜食、轻抚神经环或软垫小憩皆可帮她苏醒~）"));
        }
    }

    /** 外界粉白蛔虫娘从安全休眠中苏醒 -> 通知正在体内世界的访客 */
    public static void onExternalHostWoke(AscarisEntity ascaris) {
        if (ascaris == null || !(ascaris.level() instanceof ServerLevel hostLevel)) return;
        MinecraftServer server = hostLevel.getServer();
        BodyRegionData data = BodyRegionData.get(server);
        UUID ownerUuid = ascaris.getBodyRegionOwnerUuid();
        for (var entry : data.visits().entrySet()) {
            BodyRegionData.Visit v = entry.getValue();
            if (!v.isAscaris() || !v.host().equals(ownerUuid)) continue;
            ServerPlayer visitor = server.getPlayerList().getPlayer(entry.getKey());
            if (visitor == null || !visitor.isAlive()) continue;
            if (visitor.level().dimension().equals(BodyWorld.DIMENSION)) {
                AscarisBodyInteractions.silenceZoneHudPublic(visitor, 70);
            }
            visitor.sendSystemMessage(Component.literal(
                    "§a✦ 外界休眠中的粉白蛔虫娘已恢复意识，暖呼呼地苏醒过来了~♡"));
        }
    }

    /** 测量某肠道 X 区段周边（±6 格）肠绒毛完好率（影响真实吸收效率！） */
    public static float measureLocalMicrovilliEfficiency(ServerLevel bodyLevel, long slot, int relX) {
        int cx = BodyRegionData.centerX(slot);
        int cz = BodyRegionData.centerZ(slot);
        int intact = 0;
        int sampled = 0;
        for (int x = -20; x <= 26; x += 5) {
            if (Math.abs(x - relX) > 8) continue;
            for (int side : new int[] {-1, 1}) {
                sampled += 2;
                if (bodyLevel.getBlockState(new BlockPos(cx + x, AscarisAnatomyBuilder.GUT_FLOOR_Y + 1, cz + side * 3)).is(JRBlocks.ASCARIS_MICROVILLI)) {
                    intact++;
                }
                if (bodyLevel.getBlockState(new BlockPos(cx + x + 2, AscarisAnatomyBuilder.GUT_FLOOR_Y + 4, cz + side * 3)).is(JRBlocks.ASCARIS_MICROVILLI)) {
                    intact++;
                }
            }
        }
        if (sampled <= 0) return 0.65f;
        return Mth.clamp(0.20f + 0.80f * (intact / (float) sampled), 0.20f, 1.0f);
    }

    /** 测量 H 形排泄管横桥方块完好率（挖断排泄管会直接导致代谢废物排毒变慢！） */
    public static float measureExcretoryBridgeEfficiency(ServerLevel bodyLevel, long slot) {
        int cx = BodyRegionData.centerX(slot);
        int cz = BodyRegionData.centerZ(slot);
        int wx = cx + AscarisAnatomyBuilder.EXCRETORY_BRIDGE_X;
        int intact = 0;
        int total = 0;
        for (int z = -12; z <= 12; z += 2) {
            total++;
            if (bodyLevel.getBlockState(new BlockPos(wx, AscarisAnatomyBuilder.FLOOR_Y, cz + z)).is(JRBlocks.ASCARIS_EXCRETORY_MEMBRANE)) {
                intact++;
            }
        }
        return Mth.clamp(0.15f + 0.85f * (intact / (float) Math.max(1, total)), 0.15f, 1.0f);
    }

    /** 玩家宿主体内的实时生理映射（直接读取宿主 {@link Pregnant} 生理状态，让玩家体内也“活”起来） */
    public static void tickPlayerHostPhysiology(ServerPlayer visitor, ServerPlayer host, BodyRegionData.Visit visit) {
        ServerLevel bodyLevel = (ServerLevel) visitor.level();
        Pregnant p = (Pregnant) (Object) host;
        int ticks = visitor.tickCount;

        // 1. 每秒更新宿主实时生理监护 ActionBar
        if (ticks % 20 == 0) {
            StringBuilder sb = new StringBuilder("§d[").append(host.getName().getString()).append(" 体内生理] ");
            if (p.isPregnant()) {
                int daysLeft = Math.max(0, p.getPregnant() / (20 * 60 * 20));
                sb.append("§e孕期(余").append(daysLeft).append("天/").append(Math.max(1, p.getBabyCount())).append("胎) §7| ");
            } else if (p.hasUterus()) {
                sb.append("§d").append(p.getCurrentCycle().text).append(" §7| ");
            }
            if (p.isUterineCold()) {
                sb.append("§b宫寒偏冷 §7| ");
            }
            int excPct = Mth.clamp(p.getExcretion() / 240, 0, 100);
            int uriPct = Mth.clamp(p.getUrination() / 240, 0, 100);
            sb.append("§6肠压 ").append(excPct).append("% §7| §b膀胱 ").append(uriPct).append("%");
            int ascStage = AscarisInfectionController.stageOf(p.getAscaris());
            if (ascStage > 0) {
                sb.append(" §7| §c蛔虫").append(ascStage).append("期");
            }
            visitor.sendOverlayMessage(Component.literal(sb.toString()));
        }

        // 2. 宿主生理体征的环境物理反应（心跳 / 经期宫缩 / 胎动 / 宫寒寒雾 / 肠道排泄蠕动）
        if (ticks % 70 == 0) {
            bodyLevel.playSound(null, visitor.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.AMBIENT,
                    p.getFatigue() > 12000 ? 0.5f : 0.3f, p.isPregnant() ? 1.08f : 0.96f);
        }
        if (ticks % 30 == 0) {
            if (p.isUterineCold()) {
                bodyLevel.sendParticles(ParticleTypes.SNOWFLAKE,
                        visitor.getX() + (visitor.getRandom().nextDouble() - 0.5) * 5.0,
                        visitor.getY() + 1.0,
                        visitor.getZ() + (visitor.getRandom().nextDouble() - 0.5) * 5.0,
                        4, 0.4, 0.3, 0.4, 0.01);
            }
            if (p.hasUterus() && p.getCurrentCycle() == Pregnant.MenstruationCycle.MENSTRUATION) {
                bodyLevel.sendParticles(PAIN_CRIMSON,
                        visitor.getX() + (visitor.getRandom().nextDouble() - 0.5) * 5.0,
                        visitor.getY() + 0.8,
                        visitor.getZ() + (visitor.getRandom().nextDouble() - 0.5) * 5.0,
                        5, 0.5, 0.3, 0.5, 0.02);
            }
        }
        // 孕期柔和胎动微推 / 经期与高排泄压蠕动推力
        if (ticks % 120 == 0 && !visitor.isShiftKeyDown()) {
            if (p.isPregnant()) {
                visitor.push(0.0, 0.14, 0.0);
                visitor.hurtMarked = true;
                bodyLevel.sendParticles(ParticleTypes.HEART, visitor.getX(), visitor.getY() + 1.2, visitor.getZ(),
                        4, 0.4, 0.3, 0.4, 0.01);
            } else if (p.getExcretion() > 20 * 60 * 6) {
                visitor.push(0.24, 0.0, 0.0);
                visitor.hurtMarked = true;
                bodyLevel.playSound(null, visitor.blockPosition(), JRSounds.ASCARIS_RUMBLE, SoundSource.AMBIENT, 0.45f, 0.95f);
            }
        }
    }

    private static int classifyOrgan(BlockState state) {
        if (state.is(JRBlocks.ASCARIS_EXCRETORY_MEMBRANE)) return AscarisPhysiologyState.ORGAN_EXCRETORY;
        if (state.is(JRBlocks.ASCARIS_NERVE_CORD)) return AscarisPhysiologyState.ORGAN_NERVE;
        if (state.is(JRBlocks.ASCARIS_INTESTINAL_MEMBRANE) || state.is(JRBlocks.ASCARIS_MICROVILLI)) return AscarisPhysiologyState.ORGAN_GUT;
        if (state.is(JRBlocks.ASCARIS_UTERINE_WALL) || state.is(JRBlocks.ASCARIS_OVARY_TISSUE)) return AscarisPhysiologyState.ORGAN_REPRO;
        if (state.is(JRBlocks.ASCARIS_MUSCLE)) return AscarisPhysiologyState.ORGAN_MUSCLE;
        return AscarisPhysiologyState.ORGAN_CUTICLE;
    }

    private static BlockState resolveRestoredBlockState(String blockId) {
        try {
            Identifier id = Identifier.tryParse(blockId);
            if (id != null) {
                Block b = BuiltInRegistries.BLOCK.getValue(id);
                if (b != null && b != net.minecraft.world.level.block.Blocks.AIR) {
                    BlockState def = b.defaultBlockState();
                    if (def.hasProperty(RotatedPillarBlock.AXIS)) {
                        return def.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
                    }
                    return def;
                }
            }
        } catch (Throwable ignored) {
        }
        return JRBlocks.ASCARIS_MUCOSA.defaultBlockState();
    }
}
