package org.cneko.justarod.ascaris;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.cneko.justarod.damage.JRDamageTypes;
import org.cneko.justarod.effect.JREffects;
import org.cneko.justarod.entity.JREntities;
import org.cneko.justarod.entity.Pregnant;
import org.cneko.justarod.item.JRItems;
import org.cneko.justarod.packet.AscarisActionPayload;
import org.cneko.justarod.packet.AscarisVisualEventPayload;
import org.cneko.justarod.sound.JRSounds;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 蛔虫四阶段硬核感染 + 进食偷吃 + 视觉演出 + 双药驱虫排出总控制器。
 */
public final class AscarisInfectionController {

    /** Ⅰ 潜伏期: 1 ~ 11,999 (约 0~10 分钟) */
    public static final int STAGE_1_START = 1;
    /** Ⅱ 肺部移行期 (Löffler 期): 12,000 ~ 23,999 (约 10~20 分钟) */
    public static final int STAGE_2_TICKS = 12_000;
    /** Ⅲ 肠道成虫期 (可活体排出蛔虫娘): 24,000 ~ 47,999 (约 20~40 分钟) */
    public static final int STAGE_3_TICKS = 24_000;
    /** Ⅳ 重度肠梗阻危机: >= 48,000 (40 分钟+) */
    public static final int STAGE_4_TICKS = 48_000;

    public static final int DRUG_PAGODA = 1;
    public static final int DRUG_ALBENDAZOLE = 2;

    private static final int EXPULSION_DURATION_TICKS = 60; // 3 秒高潮演出

    private static final DustParticleOptions PINK_DUST = new DustParticleOptions(0xFF9AC2, 1.0f);
    private static final DustParticleOptions WHITE_DUST = new DustParticleOptions(0xFFF2F7, 0.85f);

    private static final Map<UUID, ExpulsionTask> ACTIVE_EXPULSIONS = new ConcurrentHashMap<>();

    private AscarisInfectionController() {
    }

    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(AscarisActionPayload.ID, (payload, context) ->
                context.server().execute(() -> {
                    ServerPlayer player = context.player();
                    Entity entity = player.level().getEntity(payload.entityId());
                    if (entity instanceof AscarisEntity ascaris) {
                        if (payload.action() == AscarisActionPayload.ACTION_CHEST_HOP) {
                            ascaris.handleChestHopFromPassenger(player);
                        } else {
                            ascaris.handleWrapActionFromPlayer(player, payload.action());
                        }
                    }
                }));

        ServerTickEvents.END_SERVER_TICK.register(AscarisInfectionController::tickExpulsions);
    }

    /** 根据感染值计算阶段 0..4 */
    public static int stageOf(int ticks) {
        if (ticks <= 0) return 0;
        if (ticks < STAGE_2_TICKS) return 1;
        if (ticks < STAGE_3_TICKS) return 2;
        if (ticks < STAGE_4_TICKS) return 3;
        return 4;
    }

    public static String stageLabel(int stage) {
        return switch (stage) {
            case 1 -> "Ⅰ期 · 虫卵潜伏";
            case 2 -> "Ⅱ期 · 肺部移行";
            case 3 -> "Ⅲ期 · 肠道成虫";
            case 4 -> "Ⅳ期 · 肠梗阻危机";
            default -> "健康";
        };
    }

    public static int minTicksForStage(int stage) {
        return switch (stage) {
            case 1 -> STAGE_1_START;
            case 2 -> STAGE_2_TICKS;
            case 3 -> STAGE_3_TICKS;
            case 4 -> STAGE_4_TICKS;
            default -> 0;
        };
    }

    /** 主动感染入口（若体内已住着已驯服原住民，不静默拒绝，而是触发领地意识把入侵者赶出体外！） */
    public static void infectPlayer(ServerPlayer player, int targetStage, String message) {
        if (AscarisHostController.hasHostedAscaris(player)) {
            AscarisHostController.handleWildIntrusion(player, targetStage >= 2, "野生蛔虫");
            return;
        }
        Pregnant p = (Pregnant) (Object) player;
        int minTicks = minTicksForStage(Math.max(1, Math.min(4, targetStage)));
        if (p.getAscaris() < minTicks) {
            p.setAscaris(minTicks);
        }
        ensureEffectSynced(player, stageOf(p.getAscaris()));
        if (message != null && !message.isEmpty()) {
            player.sendSystemMessage(Component.literal(message));
        }
        ServerLevel level = (ServerLevel) player.level();
        level.sendParticles(PINK_DUST, player.getX(), player.getY() + 1.0, player.getZ(), 10, 0.35, 0.4, 0.35, 0.03);
    }

    /** 进食钩子（由 ItemStackMixin 调用）：处理生食感染 + 成虫期偷吃饥饿值 */
    public static void onFinishConsuming(Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer sp)) return;
        Pregnant p = (Pregnant) (Object) sp;
        ServerLevel level = (ServerLevel) sp.level();

        // 1. 吞服蛔虫卵胶囊：若体内已有原住民则触发领地意识直接抽飞一条野生小虫！
        if (stack.is(JRItems.Companion.getASCARIS_EGG_CAPSULE())) {
            if (AscarisHostController.hasHostedAscaris(sp)) {
                AscarisHostController.handleWildIntrusion(sp, true, "外来虫卵");
                return;
            }
            int curStage = stageOf(p.getAscaris());
            int nextStage = Math.min(4, curStage + 1);
            infectPlayer(sp, nextStage,
                    curStage == 0
                            ? "§d你吞下了一粒温热的粉白虫卵胶囊……肚子里传来一丝微妙的悸动。"
                            : "§d虫卵胶囊加速了体内寄生！当前已进入 §c" + stageLabel(nextStage) + "§d。");
            level.playSound(null, sp.blockPosition(), JRSounds.ASCARIS_RUMBLE, SoundSource.PLAYERS, 0.7f, 1.05f);
            return;
        }

        // 2. 生肉/生鱼/腐肉/排泄物 概率引发蛔虫感染
        float infectChance = rawFoodInfectionChance(stack);
        if (infectChance > 0f) {
            // 憋排泄卫生差时感染风险翻倍
            if (p.getExcretion() > 20 * 60 * 10) {
                infectChance = Math.min(0.85f, infectChance * 1.8f);
            }
            if (sp.getRandom().nextFloat() < infectChance) {
                if (AscarisHostController.hasHostedAscaris(sp)) {
                    AscarisHostController.handleWildIntrusion(sp, false, "生食虫卵");
                } else if (p.getAscaris() <= 0) {
                    infectPlayer(sp, 1, "§e吃下去的东西好像不太干净……总觉得肚子里微微发痒。");
                } else {
                    // 重复吃脏东西加快病程推进
                    p.setAscaris(p.getAscaris() + 2400);
                }
            }
        }

        // 2.5 体内共生寄宿期间进食：同步滋养并弹出可爱尾尖轻啄，不走恶性掠夺
        if (AscarisHostController.hasHostedAscaris(sp)) {
            AscarisHostController.onHostFinishedEating(sp, stack);
            return;
        }

        // 3. Ⅲ~Ⅳ期进食营养掠夺：蛔虫娘从饱食度里“偷吃”一口！
        int stage = stageOf(p.getAscaris());
        if (stage >= 3 && stack.has(net.minecraft.core.component.DataComponents.FOOD)) {
            var foodProp = stack.get(net.minecraft.core.component.DataComponents.FOOD);
            if (foodProp != null && foodProp.nutrition() >= 2) {
                int stolen = Math.max(1, (int) Math.round(foodProp.nutrition() * (stage == 4 ? 0.55 : 0.40)));
                int after = Math.max(1, sp.getFoodData().getFoodLevel() - stolen);
                sp.getFoodData().setFoodLevel(after);
                level.playSound(null, sp.blockPosition(), JRSounds.ASCARIS_BELL, SoundSource.PLAYERS, 0.45f, 1.35f);
                level.sendParticles(PINK_DUST, sp.getX(), sp.getY() + 1.1, sp.getZ(), 8, 0.3, 0.25, 0.3, 0.03);
                ServerPlayNetworking.send(sp, new AscarisVisualEventPayload(
                        AscarisVisualEventPayload.EVENT_HUNGER_NIBBLE, sp.getId(), stage, stolen));
                if (sp.getRandom().nextInt(3) == 0) {
                    sp.sendOverlayMessage(Component.literal("§d刚吃下去的食物好像被肚子里的小家伙偷走了一半……（叮铃~）"));
                }
            }
        } else if (stage >= 1 && stack.has(net.minecraft.core.component.DataComponents.FOOD) && sp.getRandom().nextInt(3) == 0) {
            level.sendParticles(WHITE_DUST, sp.getX(), sp.getY() + 1.35, sp.getZ(), 4, 0.2, 0.15, 0.2, 0.02);
        }
    }

    private static float rawFoodInfectionChance(ItemStack stack) {
        if (stack.is(JRItems.Companion.getEXCREMENT())) return 0.50f;
        if (stack.is(Items.ROTTEN_FLESH) || stack.is(Items.SPIDER_EYE)) return 0.30f;
        if (stack.is(Items.BEEF) || stack.is(Items.PORKCHOP) || stack.is(Items.MUTTON)
                || stack.is(Items.CHICKEN) || stack.is(Items.RABBIT)
                || stack.is(Items.COD) || stack.is(Items.SALMON) || stack.is(Items.TROPICAL_FISH)) {
            return 0.14f;
        }
        if (stack.is(Items.SUSPICIOUS_STEW)) return 0.12f;
        return 0f;
    }

    /** 玩家每 Tick 感染推进（在 PlayerMixin#tick 服务端分支中调用） */
    public static void tickPlayer(LivingEntity living) {
        if (!(living instanceof ServerPlayer player)) return;
        if (AscarisHostController.hasHostedAscaris(player)) {
            AscarisHostController.tickHostedPlayer(player);
        }
        Pregnant p = (Pregnant) (Object) player;
        int ticks = p.getAscaris();
        if (ticks <= 0) {
            if (player.hasEffect(JREffects.Companion.getASCARIS_INFECTION_EFFECT())) {
                player.removeEffect(JREffects.Companion.getASCARIS_INFECTION_EFFECT());
            }
            return;
        }

        // 推进病程（熬夜疲劳或免疫低下时增速）
        int inc = (p.getFatigue() > Pregnant.FATIGUE_STAGE_3 || p.getAids() > 0) ? 2 : 1;
        int prevStage = stageOf(ticks);
        ticks = Math.min(ticks + inc, 20 * 60 * 20 * 10);
        p.setAscaris(ticks);
        int stage = stageOf(ticks);

        // 阶段晋升提示
        if (stage > prevStage) {
            onStageAdvance(player, stage);
        }

        // 同步药水图标（每 2 秒刷新）
        if (player.tickCount % 40 == 0) {
            ensureEffectSynced(player, stage);
        }

        ServerLevel level = (ServerLevel) player.level();

        // ===== 阶段专属硬核表现 + 视觉听觉反差 =====
        if (stage == 1) {
            // 潜伏期：极轻微的饥饿消耗增加
            if (player.tickCount % 300 == 0) {
                player.causeFoodExhaustion(0.4f);
            }
        } else if (stage == 2) {
            // Ⅱ 肺部移行期 (Löffler 综合征)：阵发性咳嗽、轻微掉氧、低热泛红 + 极轻腹中金铃声
            if (player.tickCount % 260 == 0 && player.getRandom().nextFloat() < 0.45f) {
                player.setAirSupply(Math.max(40, player.getAirSupply() - 55));
                player.setSprinting(false);
                level.playSound(null, player.blockPosition(), JRSounds.ASCARIS_RUMBLE, SoundSource.PLAYERS, 0.55f, 1.15f);
                level.sendParticles(WHITE_DUST, player.getX(), player.getY() + 1.4, player.getZ(), 6, 0.25, 0.2, 0.25, 0.03);
                ServerPlayNetworking.send(player, new AscarisVisualEventPayload(
                        AscarisVisualEventPayload.EVENT_LOFFLER_COUGH, player.getId(), stage, 0));
                if (player.getRandom().nextInt(3) == 0) {
                    player.sendOverlayMessage(Component.literal("§e一阵干咳袭来……恍惚间好像听见肚子里传来极轻的「叮铃」声？"));
                }
            }
        } else if (stage == 3) {
            // Ⅲ 肠道成虫期：周期偷吃饱食度 + 腹中铃声 + 第三人称脚边粉白尾影闪现粒子
            if (player.tickCount % 200 == 0) {
                player.causeFoodExhaustion(1.3f);
            }
            if (player.tickCount % 320 == 0 && player.getRandom().nextFloat() < 0.6f) {
                level.playSound(null, player.blockPosition(), JRSounds.ASCARIS_RUMBLE, SoundSource.PLAYERS, 0.65f, 1.0f);
                ServerPlayNetworking.send(player, new AscarisVisualEventPayload(
                        AscarisVisualEventPayload.EVENT_BELLY_RUMBLE, player.getId(), stage, 0));
                if (player.getFoodData().getFoodLevel() > 4 && player.getRandom().nextFloat() < 0.45f) {
                    player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 1);
                    ServerPlayNetworking.send(player, new AscarisVisualEventPayload(
                            AscarisVisualEventPayload.EVENT_HUNGER_NIBBLE, player.getId(), stage, 1));
                }
            }
            // 第三人称外显异象：脚边/腰际盘绕闪现的粉白微光弧线 + 音符粒子
            if (player.tickCount % 35 == 0) {
                spawnThirdPersonTailGlint(level, player, false);
            }
        } else if (stage == 4) {
            // Ⅳ 重度肠梗阻危机：硬核绞痛扣血 + 强视觉脉动 + 近距离传染风险
            if (player.tickCount % 20 == 0) {
                spawnThirdPersonTailGlint(level, player, true);
            }
            if (player.tickCount % 220 == 0 && !ACTIVE_EXPULSIONS.containsKey(player.getUUID())) {
                player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 70, 1, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 80, 0, false, false));
                player.hurtServer(level, JRDamageTypes.of(level, JRDamageTypes.ASCARIS_OBSTRUCTION), 1.5f);
                level.playSound(null, player.blockPosition(), JRSounds.ASCARIS_RUMBLE, SoundSource.PLAYERS, 0.85f, 0.9f);
                level.playSound(null, player.blockPosition(), JRSounds.ASCARIS_BELL, SoundSource.PLAYERS, 0.7f, 1.25f);
                ServerPlayNetworking.send(player, new AscarisVisualEventPayload(
                        AscarisVisualEventPayload.EVENT_OBSTRUCTION_CRAMP, player.getId(), stage, 0));
                player.sendOverlayMessage(Component.literal("§c§l剧烈腹绞痛袭来！§r§d里面已经完全长大了……快服用宝塔糖或阿苯达唑驱虫！"));
            }
            // 硬核传染：重度感染且排泄卫生差时，贴身接触可能传染给其他玩家
            if (player.tickCount % 400 == 0 && p.getExcretion() > 20 * 60 * 5) {
                List<ServerPlayer> nearby = level.getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(2.2),
                        other -> other != player && other.isAlive() && ((Pregnant) (Object) other).getAscaris() <= 0);
                for (ServerPlayer other : nearby) {
                    if (player.getRandom().nextFloat() < 0.15f) {
                        infectPlayer(other, 1, "§e与重度蛔虫感染者贴身接触后，你总觉得肚子有点不舒服……");
                    }
                }
            }
        }
    }

    private static void onStageAdvance(ServerPlayer player, int stage) {
        ServerLevel level = (ServerLevel) player.level();
        switch (stage) {
            case 2 -> {
                player.sendSystemMessage(Component.literal("§e[蛔虫感染 · Ⅱ期肺部移行] §f你突然剧烈咳嗽起来，胸腹间似乎有细小的生命正在游走……"));
                level.playSound(null, player.blockPosition(), JRSounds.ASCARIS_RUMBLE, SoundSource.PLAYERS, 0.7f, 1.15f);
            }
            case 3 -> {
                player.sendSystemMessage(Component.literal("§d[蛔虫感染 · Ⅲ期肠道成虫] §f肚子深处传来一声清脆的「叮铃~」——她已经长成成虫了！§7（用宝塔糖可温和麻痹活体排出）"));
                level.playSound(null, player.blockPosition(), JRSounds.ASCARIS_BELL, SoundSource.PLAYERS, 0.85f, 1.1f);
                ServerPlayNetworking.send(player, new AscarisVisualEventPayload(
                        AscarisVisualEventPayload.EVENT_BELLY_RUMBLE, player.getId(), 3, 0));
            }
            case 4 -> {
                player.sendSystemMessage(Component.literal("§c[蛔虫感染 · Ⅳ期肠梗阻危机] §f腹部绞痛难忍，里面已经挤不下了！不及时驱虫将危及生命！"));
                level.playSound(null, player.blockPosition(), JRSounds.ASCARIS_RUMBLE, SoundSource.PLAYERS, 0.9f, 0.88f);
                ServerPlayNetworking.send(player, new AscarisVisualEventPayload(
                        AscarisVisualEventPayload.EVENT_OBSTRUCTION_CRAMP, player.getId(), 4, 0));
            }
        }
    }

    /** 第三人称可见异象：在重度感染宿主腰际/脚边划出一小截粉白交替的弧形尾影粒子 + 铃铛音符 */
    private static void spawnThirdPersonTailGlint(ServerLevel level, ServerPlayer player, boolean intense) {
        double baseAngle = (player.tickCount * 0.25) % (Math.PI * 2);
        int segs = intense ? 10 : 6;
        double radius = 0.65;
        for (int i = 0; i < segs; i++) {
            double ang = baseAngle + i * 0.32;
            double px = player.getX() + Math.cos(ang) * radius;
            double pz = player.getZ() + Math.sin(ang) * radius;
            double py = player.getY() + 0.22 + (i / (double) segs) * 0.45;
            level.sendParticles((i % 2 == 0) ? PINK_DUST : WHITE_DUST, px, py, pz, 1, 0.02, 0.02, 0.02, 0.0);
        }
        if (intense && player.getRandom().nextInt(3) == 0) {
            level.sendParticles(ParticleTypes.NOTE, player.getX(), player.getY() + 1.0, player.getZ(), 1, 0.3, 0.2, 0.3, 0.0);
        }
    }

    private static void ensureEffectSynced(ServerPlayer player, int stage) {
        if (stage <= 0) return;
        player.addEffect(new MobEffectInstance(
                JREffects.Companion.getASCARIS_INFECTION_EFFECT(),
                100,
                Math.max(0, stage - 1),
                false,
                false,
                true
        ));
    }

    // ===== 双药驱虫入口（宝塔糖哌嗪温和麻痹 vs 阿苯达唑硬核强杀） =====

    public static boolean administerDewormer(LivingEntity target, int drugType, @Nullable Player giver) {
        if (!(target instanceof ServerPlayer host)) return false;
        Pregnant p = (Pregnant) (Object) host;
        int stage = stageOf(p.getAscaris());
        ServerLevel level = (ServerLevel) host.level();

        if (stage <= 0) {
            if (AscarisHostController.hasHostedAscaris(host)) {
                AscarisHostController.summonFromHostBody(host, true, drugType == DRUG_ALBENDAZOLE);
                return true;
            }
            if (giver != null) {
                giver.sendSystemMessage(Component.literal(drugType == DRUG_PAGODA
                        ? "§d甜甜的宝塔糖入口即化——不过体内并没有蛔虫呢。"
                        : "§7未检测到蛔虫感染，服下阿苯达唑后只觉得胃里微微发苦。"));
            }
            if (drugType == DRUG_PAGODA) {
                host.getFoodData().eat(2, 0.2f);
            }
            return true;
        }

        if (ACTIVE_EXPULSIONS.containsKey(host.getUUID())) {
            if (giver != null) {
                giver.sendSystemMessage(Component.literal("§e药效正在剧烈发作中，请稍候片刻！"));
            }
            return false;
        }

        // Ⅰ~Ⅱ期（尚未长成成虫）：直接清除幼虫，不产出成体蛔虫娘
        if (stage < 3) {
            p.setAscaris(0);
            host.removeEffect(JREffects.Companion.getASCARIS_INFECTION_EFFECT());
            BodyAscarisBridge.despawnForHost(level.getServer(), host.getUUID());
            level.playSound(null, host.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7f, 1.3f);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, host.getX(), host.getY() + 1.0, host.getZ(), 10, 0.4, 0.5, 0.4, 0.04);
            host.sendSystemMessage(Component.literal(drugType == DRUG_PAGODA
                    ? "§a宝塔糖温和麻痹并清除了体内的早期蛔虫幼虫，肚子瞬间轻松了！"
                    : "§a阿苯达唑迅速杀灭了体内的蛔虫卵与幼虫，感染已彻底清除。"));
            return true;
        }

        // Ⅲ~Ⅳ期（已长成成体）：启动 3 秒高潮排出演出 -> 实体粉白蛔虫娘滑出倒地！
        ACTIVE_EXPULSIONS.put(host.getUUID(), new ExpulsionTask(host.getUUID(), drugType, stage, EXPULSION_DURATION_TICKS));
        host.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, EXPULSION_DURATION_TICKS + 10, 2, false, false));
        level.playSound(null, host.blockPosition(), JRSounds.ASCARIS_RUMBLE, SoundSource.PLAYERS, 0.9f, 0.95f);
        ServerPlayNetworking.send(host, new AscarisVisualEventPayload(
                AscarisVisualEventPayload.EVENT_EXPULSION_START, host.getId(), drugType, EXPULSION_DURATION_TICKS));
        host.sendSystemMessage(Component.literal(drugType == DRUG_PAGODA
                ? "§d[枸橼酸哌嗪生效] §f温甜的药力缓缓麻痹了体内的长尾……她正在失去抓力滑出来！"
                : "§c[阿苯达唑生效] §f强烈的驱虫药效席卷肠道，里面的家伙剧烈挣扎起来！"));
        return true;
    }

    private static void tickExpulsions(MinecraftServer server) {
        if (ACTIVE_EXPULSIONS.isEmpty()) return;
        Iterator<Map.Entry<UUID, ExpulsionTask>> it = ACTIVE_EXPULSIONS.entrySet().iterator();
        while (it.hasNext()) {
            ExpulsionTask task = it.next().getValue();
            ServerPlayer host = server.getPlayerList().getPlayer(task.hostUuid);
            if (host == null || !host.isAlive()) {
                it.remove();
                continue;
            }
            ServerLevel level = (ServerLevel) host.level();
            task.ticksLeft--;

            // 上升的粉白双色螺旋粒子漩涡 + 越来越急促的铃铛声
            int elapsed = EXPULSION_DURATION_TICKS - task.ticksLeft;
            double progress = elapsed / (double) EXPULSION_DURATION_TICKS;
            double ang = elapsed * 0.55;
            double rad = 1.15 - progress * 0.45;
            for (int arm = 0; arm < 2; arm++) {
                double a = ang + arm * Math.PI;
                double px = host.getX() + Math.cos(a) * rad;
                double pz = host.getZ() + Math.sin(a) * rad;
                double py = host.getY() + 0.25 + progress * 1.3;
                level.sendParticles(arm == 0 ? PINK_DUST : WHITE_DUST, px, py, pz, 2, 0.05, 0.05, 0.05, 0.01);
            }
            if (elapsed == 18 || elapsed == 36 || elapsed == 48) {
                level.playSound(null, host.blockPosition(), JRSounds.ASCARIS_BELL, SoundSource.PLAYERS,
                        0.65f + (float) progress * 0.3f, 1.0f + (float) progress * 0.35f);
            }

            if (task.ticksLeft <= 0) {
                it.remove();
                completeExpulsion(level, host, task);
            }
        }
    }

    private static void completeExpulsion(ServerLevel level, ServerPlayer host, ExpulsionTask task) {
        Pregnant p = (Pregnant) (Object) host;
        p.setAscaris(0);
        host.removeEffect(JREffects.Companion.getASCARIS_INFECTION_EFFECT());
        BodyAscarisBridge.despawnForHost(level.getServer(), host.getUUID());

        ServerPlayNetworking.send(host, new AscarisVisualEventPayload(
                AscarisVisualEventPayload.EVENT_EXPULSION_FINISH, host.getId(), task.drugType, 0));

        // 炸出粉白花瓣/微光粒子 + 软滑声 + 清脆铃声
        Vec3 forward = Vec3.directionFromRotation(0, host.getYRot()).scale(1.15);
        double sx = host.getX() + forward.x;
        double sy = host.getY() + 0.05;
        double sz = host.getZ() + forward.z;

        level.sendParticles(PINK_DUST, sx, sy + 0.65, sz, 28, 0.45, 0.45, 0.45, 0.08);
        level.sendParticles(WHITE_DUST, sx, sy + 0.65, sz, 20, 0.45, 0.45, 0.45, 0.06);
        level.sendParticles(ParticleTypes.CHERRY_LEAVES, sx, sy + 0.9, sz, 12, 0.5, 0.4, 0.5, 0.04);
        level.playSound(null, host.blockPosition(), JRSounds.ASCARIS_SLITHER, SoundSource.PLAYERS, 1.0f, 0.95f);
        level.playSound(null, host.blockPosition(), JRSounds.ASCARIS_BELL, SoundSource.PLAYERS, 0.95f, 1.15f);

        AscarisEntity ascaris = JREntities.ASCARIS.create(level, EntitySpawnReason.EVENT);
        if (ascaris != null) {
            ascaris.setPos(sx, sy, sz);
            float faceHostYaw = host.getYRot() + 180f;
            ascaris.setYRot(faceHostYaw);
            ascaris.setYBodyRot(faceHostYaw);
            ascaris.setYHeadRot(faceHostYaw);
            ascaris.setVisualScale(task.stage >= 4 ? 1.0f : 0.88f);
            boolean harsh = task.drugType == DRUG_ALBENDAZOLE;
            ascaris.configureAsFreshlyExpelled(harsh);
            level.addFreshEntity(ascaris);

            if (harsh) {
                host.sendSystemMessage(Component.literal(
                        "§c一条粉白蛔虫娘在阿苯达唑药力下踉跄侧倒在地上，生命垂危！§7（立刻喂甜食可勉强救活，否则片刻后将化作虫蜕掉落）"));
            } else {
                host.sendSystemMessage(Component.literal(
                        "§d§l「叮铃~」一条粉白蛔虫娘从你体内软绵绵地滑了出来，虚弱地侧倒在地发抖！§r§7（喂她甜食可安抚收服，也可趁虚清除）"));
            }
        }
    }

    public static boolean isExpelling(LivingEntity entity) {
        return entity != null && ACTIVE_EXPULSIONS.containsKey(entity.getUUID());
    }

    public static void cureCleanly(ServerPlayer player) {
        ACTIVE_EXPULSIONS.remove(player.getUUID());
        ((Pregnant) (Object) player).setAscaris(0);
        player.removeEffect(JREffects.Companion.getASCARIS_INFECTION_EFFECT());
        if (player.level() instanceof ServerLevel sl) {
            BodyAscarisBridge.despawnForHost(sl.getServer(), player.getUUID());
        }
    }

    private static final class ExpulsionTask {
        final UUID hostUuid;
        final int drugType;
        final int stage;
        int ticksLeft;

        ExpulsionTask(UUID hostUuid, int drugType, int stage, int ticksLeft) {
            this.hostUuid = hostUuid;
            this.drugType = drugType;
            this.stage = stage;
            this.ticksLeft = ticksLeft;
        }
    }
}
