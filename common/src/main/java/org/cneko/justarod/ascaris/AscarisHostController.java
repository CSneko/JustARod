package org.cneko.justarod.ascaris;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.cneko.justarod.config.JRConfig;
import org.cneko.justarod.entity.JREntities;
import org.cneko.justarod.entity.Pregnant;
import org.cneko.justarod.item.JRItems;
import org.cneko.justarod.packet.AscarisVisualEventPayload;
import org.cneko.justarod.sound.JRSounds;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 粉白蛔虫娘「体内共生寄宿 + 自动钻入 + 领地意识强制驱逐」总控器：
 * <ul>
 *   <li>主动：小金铃 Shift+右键收放，完整保留名字/血量/饱食/亲密/执念/体型；</li>
 *   <li>自动：体外残血避险自动钻回主人肚子里回血，或病娇高执念时主动贴身钻入；</li>
 *   <li>领地意识：不静默拦截——若有野生虫卵/寄生虫或第二只蛔虫娘试图进入，体内原住民会立刻触发领地爆发，把入侵者连同粒子与击退强行顶飞赶出体外！</li>
 * </ul>
 */
public final class AscarisHostController {

    private static final DustParticleOptions PINK_DUST = new DustParticleOptions(0xFF9EC4, 0.95f);
    private static final DustParticleOptions WHITE_DUST = new DustParticleOptions(0xFFF2F7, 0.8f);
    private static final DustParticleOptions YANDERE_DUST = new DustParticleOptions(0xC2184E, 1.0f);

    private AscarisHostController() {
    }

    public static boolean hasHostedAscaris(Player player) {
        if (!(player instanceof Pregnant p)) return false;
        CompoundTag tag = p.getHostedAscarisNbt();
        return tag != null && !tag.isEmpty();
    }

    @Nullable
    public static CompoundTag getHostedTag(Player player) {
        if (!(player instanceof Pregnant p)) return null;
        CompoundTag tag = p.getHostedAscarisNbt();
        return (tag == null || tag.isEmpty()) ? null : tag;
    }

    /** 将一只已驯服的粉白蛔虫娘收进主人的体内；若体内已有原住民，则触发领地意识强制把新来的顶飞赶走！ */
    public static boolean enterHostBody(AscarisEntity ascaris, ServerPlayer host, String reason) {
        if (ascaris == null || host == null || !ascaris.isAlive() || !host.isAlive() || host.isSpectator()) {
            return false;
        }
        if (!ascaris.isTame() || !ascaris.isOwnedBy(host)) {
            return false;
        }

        ServerLevel level = (ServerLevel) host.level();

        // ===== 领地意识：体内已经住着原住民，强行把第二只新来的顶飞赶出去！ =====
        if (hasHostedAscaris(host)) {
            triggerTerritorialEvictionOfEntity(host, ascaris);
            return false;
        }

        // 先解除任何缠绕和胸口骑乘
        if (ascaris.getPoseState().isWrapping()) {
            ascaris.releaseTailWrap(false);
        }
        if (ascaris.hasChestPassenger()) {
            ascaris.ejectPassengers();
        }

        // 若主人体内原本有野生感染，钻入的同时把野生寄生虫一口气赶出去！
        Pregnant p = (Pregnant) (Object) host;
        int prevWild = p.getAscaris();

        // 完整序列化保存个体状态
        CompoundTag saved = serializeAscaris(ascaris);
        p.setHostedAscarisNbt(saved);

        // 粉白螺旋钻入特效
        for (int i = 0; i < 16; i++) {
            double ang = i * 0.45;
            double r = 0.75 * (1.0 - i / 16.0);
            level.sendParticles((i % 2 == 0) ? PINK_DUST : WHITE_DUST,
                    host.getX() + Math.cos(ang) * r,
                    host.getY() + 0.55 + (i / 16.0) * 0.55,
                    host.getZ() + Math.sin(ang) * r,
                    1, 0.02, 0.02, 0.02, 0.01);
        }
        level.playSound(null, host.blockPosition(), JRSounds.ASCARIS_SLITHER, SoundSource.PLAYERS, 0.9f, 1.08f);
        level.playSound(null, host.blockPosition(), JRSounds.ASCARIS_BELL, SoundSource.PLAYERS, 0.7f, 1.18f);
        float voicePitch = 0.97f + level.getRandom().nextFloat() * 0.06f;
        switch (reason) {
            case "low_hp_flee" ->
                    level.playSound(null, host.blockPosition(), JRSounds.ASCARIS_VOICE_WHIMPER, SoundSource.VOICE, 0.85f, voicePitch);
            case "yandere_snuggle" ->
                    level.playSound(null, host.blockPosition(),
                            JRConfig.isAscarisYandereEnabled() ? JRSounds.ASCARIS_VOICE_YANDERE_GIGGLE : JRSounds.ASCARIS_VOICE_SLEEPY,
                            SoundSource.VOICE, 0.85f, voicePitch);
            default ->
                    level.playSound(null, host.blockPosition(), JRSounds.ASCARIS_VOICE_SLEEPY, SoundSource.VOICE, 0.78f, voicePitch);
        }
        ServerPlayNetworking.send(host, new AscarisVisualEventPayload(
                AscarisVisualEventPayload.EVENT_BELLY_RUMBLE, host.getId(), 3, 0));

        ascaris.remove(Entity.RemovalReason.DISCARDED);
        syncHostSummaryToClient(host);

        if (prevWild > 0) {
            p.setAscaris(0);
            host.removeEffect(org.cneko.justarod.effect.JREffects.Companion.getASCARIS_INFECTION_EFFECT());
            BodyAscarisBridge.despawnForHost(level.getServer(), host.getUUID());
            evictWildIntruderVisual(host, prevWild >= AscarisInfectionController.STAGE_2_TICKS);
        }

        String displayName = saved.getStringOr("CustomName", "粉白蛔虫娘");
        switch (reason) {
            case "low_hp_flee" -> host.sendSystemMessage(Component.literal(
                    "§d" + displayName + " 受伤受惊了，缩成一团飞快地钻回你的肚子里避难疗伤……"));
            case "yandere_snuggle" -> host.sendSystemMessage(Component.literal(
                    "§c§o「外面太吵啦……还是钻回你的身体里、贴着你的心跳最安心了♡」§7（" + displayName + " 自己钻进了你的体内）"));
            default -> host.sendSystemMessage(Component.literal(
                    "§d「叮铃~」" + displayName + " 卷起粉白长尾，暖呼呼地钻回了你的体内！§7（小金铃 Shift+右键可唤出）"));
        }
        return true;
    }

    /** 将体内寄宿的粉白蛔虫娘唤出体外（可为温柔摇铃唤出，或驱虫药误服逼出） */
    @Nullable
    public static AscarisEntity summonFromHostBody(ServerPlayer host, boolean forcedByDewormer, boolean harshDewormer) {
        CompoundTag saved = getHostedTag(host);
        if (saved == null) return null;

        Pregnant p = (Pregnant) (Object) host;
        p.setHostedAscarisNbt(null);

        ServerLevel level = (ServerLevel) host.level();
        BodyAscarisBridge.despawnForHost(level.getServer(), host.getUUID());

        AscarisEntity ascaris = JREntities.ASCARIS.create(level, EntitySpawnReason.EVENT);
        if (ascaris == null) return null;

        Vec3 forward = Vec3.directionFromRotation(0, host.getYRot()).scale(1.15);
        ascaris.setPos(host.getX() + forward.x, host.getY() + 0.05, host.getZ() + forward.z);
        float faceYaw = host.getYRot() + 180f;
        ascaris.setYRot(faceYaw);
        ascaris.setYBodyRot(faceYaw);
        ascaris.setYHeadRot(faceYaw);

        applySerializedState(ascaris, saved, host);
        ascaris.setAutoEnterCooldown(forcedByDewormer ? 360 : 240);

        if (forcedByDewormer) {
            ascaris.setAffection(Math.max(0, ascaris.getAffection() - (harshDewormer ? 22 : 12)));
            if (JRConfig.isAscarisYandereEnabled()) {
                ascaris.setObsession(Math.min(100, ascaris.getObsession() + (harshDewormer ? 28 : 18)));
            }
            ascaris.setPoseState(harshDewormer ? AscarisPoseState.COLLAPSE : AscarisPoseState.CURL_UP);
            ascaris.setHealth(Math.max(4.0f, ascaris.getHealth() - (harshDewormer ? 8.0f : 3.0f)));
            level.sendParticles(WHITE_DUST, ascaris.getX(), ascaris.getY() + 0.7, ascaris.getZ(), 18, 0.4, 0.4, 0.4, 0.05);
            level.playSound(null, host.blockPosition(), JRSounds.ASCARIS_SLITHER, SoundSource.PLAYERS, 0.95f, 0.85f);
            level.playSound(null, host.blockPosition(), JRSounds.ASCARIS_BELL, SoundSource.PLAYERS, 0.75f, 0.78f);
            ascaris.playVoice(JRSounds.ASCARIS_VOICE_WHIMPER, 0.9f, 50, true);
            if (JRConfig.isAscarisYandereEnabled()) {
                host.sendSystemMessage(Component.literal(
                        "§c§o「咳……居然吃驱虫药想把我赶出去？！太过分了啦……下次绝对不会乖乖松开了♡」"));
            } else {
                host.sendSystemMessage(Component.literal(
                        "§d她被驱虫药熏得眼泪汪汪地滑了出来，委屈地 >~< 缩成一团发抖……"));
            }
        } else {
            ascaris.playTransientPose(AscarisPoseState.WAVE, 52);
            level.sendParticles(PINK_DUST, ascaris.getX(), ascaris.getY() + 0.8, ascaris.getZ(), 16, 0.4, 0.4, 0.4, 0.04);
            level.sendParticles(ParticleTypes.HEART, ascaris.getX(), ascaris.getY() + 1.2, ascaris.getZ(), 6, 0.3, 0.25, 0.3, 0.02);
            level.playSound(null, host.blockPosition(), JRSounds.ASCARIS_BELL, SoundSource.PLAYERS, 0.75f, 1.2f);
            ascaris.playVoice(JRSounds.ASCARIS_VOICE_GREET, 0.9f, 36, true);
            String name = saved.getStringOr("CustomName", "粉白蛔虫娘");
            host.sendSystemMessage(Component.literal("§d「叮铃~」" + name + " 暖呼呼地钻出来向你挥手啦！"));
        }

        level.addFreshEntity(ascaris);
        syncHostSummaryToClient(host);
        return ascaris;
    }

    /**
     * 领地意识触发（外来野生感染 / 虫卵胶囊）：
     * 不是静默无事发生，而是先引起一阵剧烈腹鸣与甩尾爆发，把刚试图入侵的外来蛔虫强行赶出体外！
     *
     * @return true 表示体内原住民已拦截并强制赶走入侵者（不再累计野生感染）
     */
    public static boolean handleWildIntrusion(ServerPlayer host, boolean spawnExpelledWorm, String sourceLabel) {
        CompoundTag saved = getHostedTag(host);
        if (saved == null) return false;

        if (JRConfig.isAscarisYandereEnabled()) {
            int obs = Math.min(100, saved.getIntOr("Obsession", 15) + 6);
            saved.putInt("Obsession", obs);
        }
        evictWildIntruderVisual(host, spawnExpelledWorm);
        syncHostSummaryToClient(host);

        String name = saved.getStringOr("CustomName", "粉白蛔虫娘");
        boolean yandere = JRConfig.isAscarisYandereEnabled();
        if (yandere) {
            host.sendSystemMessage(Component.literal(
                    "§c§o「哈？！居然有外来的" + sourceLabel + "敢钻进我的领地？给我滚出去啦！」§7（体内的 "
                            + name + " 触发领地意识，一尾巴把入侵者抽飞出了体外！）"));
        } else {
            host.sendSystemMessage(Component.literal(
                    "§d「呜咿！这里明明是我先住下的专属小窝，外来的" + sourceLabel + "不许进来！」§7（体内的 "
                            + name + " 触发领地意识，把入侵者强行顶出了体外！）"));
        }
        return true;
    }

    /** 领地意识触发（试图把第 2 只蛔虫娘塞进已有原住民的体内） */
    public static void triggerTerritorialEvictionOfEntity(ServerPlayer host, AscarisEntity newcomer) {
        ServerLevel level = (ServerLevel) host.level();
        CompoundTag saved = getHostedTag(host);
        boolean yandere = JRConfig.isAscarisYandereEnabled();
        if (saved != null && yandere) {
            saved.putInt("Obsession", Math.min(100, saved.getIntOr("Obsession", 15) + 10));
        }

        Vec3 away = newcomer.position().subtract(host.position());
        double len = Math.max(0.1, away.horizontalDistance());
        newcomer.setDeltaMovement(away.x / len * 0.92, 0.36, away.z / len * 0.92);
        newcomer.hurtMarked = true;
        newcomer.playTransientPose(AscarisPoseState.CURL_UP, 65);
        newcomer.setAutoEnterCooldown(200);

        level.playSound(null, host.blockPosition(), JRSounds.ASCARIS_RUMBLE, SoundSource.PLAYERS, 0.9f, 0.92f);
        level.playSound(null, host.blockPosition(), JRSounds.ASCARIS_SLITHER, SoundSource.PLAYERS, 0.95f, 0.85f);
        level.playSound(null, host.blockPosition(), JRSounds.ASCARIS_BELL, SoundSource.PLAYERS, 0.75f, 0.72f);
        level.playSound(null, host.blockPosition(),
                yandere ? JRSounds.ASCARIS_VOICE_JEALOUS : JRSounds.ASCARIS_VOICE_HISS,
                SoundSource.VOICE, 0.92f, 0.97f + level.getRandom().nextFloat() * 0.06f);
        level.sendParticles(yandere ? YANDERE_DUST : PINK_DUST,
                host.getX(), host.getY() + 0.9, host.getZ(), 20, 0.45, 0.35, 0.45, 0.08);
        ServerPlayNetworking.send(host, new AscarisVisualEventPayload(
                AscarisVisualEventPayload.EVENT_BELLY_RUMBLE, host.getId(), 4, 0));

        String residentName = saved != null ? saved.getStringOr("CustomName", "粉白蛔虫娘") : "粉白蛔虫娘";
        if (yandere) {
            host.sendSystemMessage(Component.literal(
                    "§c§o「还想把别的家伙塞进你的身体里？！这副身体只能住我一个——滚开啦！」§7（体内的 "
                            + residentName + " 爆发领地意识，把新来的强行顶飞了出去！）"));
        } else {
            host.sendSystemMessage(Component.literal(
                    "§d「唔咿！肚子里已经住满我啦，挤不下第二只了啦！」§7（体内的 "
                            + residentName + " 触发领地意识，把新来的顶飞了出去！）"));
        }
        syncHostSummaryToClient(host);
    }

    private static void evictWildIntruderVisual(ServerPlayer host, boolean spawnExpelledMiniWorm) {
        ServerLevel level = (ServerLevel) host.level();
        Vec3 fwd = Vec3.directionFromRotation(0, host.getYRot());
        double ex = host.getX() + fwd.x * 1.1;
        double ey = host.getY() + 0.45;
        double ez = host.getZ() + fwd.z * 1.1;

        boolean yandere = JRConfig.isAscarisYandereEnabled();
        level.playSound(null, host.blockPosition(), JRSounds.ASCARIS_RUMBLE, SoundSource.PLAYERS, 0.85f, 0.95f);
        level.playSound(null, host.blockPosition(), JRSounds.ASCARIS_SLITHER, SoundSource.PLAYERS, 0.9f, 1.05f);
        level.playSound(null, host.blockPosition(), JRSounds.ASCARIS_BELL, SoundSource.PLAYERS, 0.7f, yandere ? 0.72f : 1.25f);
        level.playSound(null, host.blockPosition(), JRSounds.ASCARIS_VOICE_HISS,
                SoundSource.VOICE, 0.9f, 0.97f + level.getRandom().nextFloat() * 0.06f);
        level.sendParticles(yandere ? YANDERE_DUST : PINK_DUST, ex, ey + 0.3, ez, 18, 0.35, 0.3, 0.35, 0.08);
        level.sendParticles(WHITE_DUST, ex, ey + 0.25, ez, 12, 0.3, 0.25, 0.3, 0.05);
        ServerPlayNetworking.send(host, new AscarisVisualEventPayload(
                AscarisVisualEventPayload.EVENT_BELLY_RUMBLE, host.getId(), 3, 0));

        if (spawnExpelledMiniWorm) {
            // 直接把一条发懵的野生小蛔虫娘抽飞甩到地面！
            AscarisEntity intruder = JREntities.ASCARIS.create(level, EntitySpawnReason.EVENT);
            if (intruder != null) {
                intruder.setPos(ex, ey, ez);
                intruder.setYRot(host.getYRot());
                intruder.setYBodyRot(host.getYRot());
                intruder.setVisualScale(0.55f);
                intruder.setExpelledStage(AscarisEntity.EXPELLED_CAUTIOUS_CURLED);
                intruder.playTransientPose(AscarisPoseState.CURL_UP, 90);
                intruder.setDeltaMovement(fwd.x * 0.78, 0.32, fwd.z * 0.78);
                intruder.hurtMarked = true;
                level.addFreshEntity(intruder);
            }
        } else {
            // 早期生食虫卵被强行顶出：吐出一粒失活虫卵/粉白粒子团
            var drop = host.spawnAtLocation(level, new ItemStack(JRItems.Companion.getASCARIS_EGG_CAPSULE()), 0.5f);
            if (drop != null) {
                drop.setDeltaMovement(fwd.x * 0.45, 0.25, fwd.z * 0.45);
                drop.setPickUpDelay(35);
            }
        }
    }

    /** 宿主体内寄宿期间每 Tick 同步与暖腹共生恢复 + 病娇危急自动冲出护主 */
    public static void tickHostedPlayer(ServerPlayer host) {
        CompoundTag saved = getHostedTag(host);
        if (saved == null) return;

        ServerLevel level = (ServerLevel) host.level();
        int ticks = host.tickCount;

        // 1. 体内持续安全疗伤回血
        if (ticks % 60 == 0) {
            float hp = saved.getFloatOr("Health", 30f);
            float maxHp = Math.max(20f, saved.getFloatOr("MaxHealth", 30f));
            if (hp < maxHp) {
                saved.putFloat("Health", Math.min(maxHp, hp + 1.5f));
            }
        }

        // 2. 在主人心跳旁安心平复执念
        if (ticks % 240 == 0 && JRConfig.isAscarisYandereEnabled()) {
            int obs = saved.getIntOr("Obsession", 15);
            if (obs > 20) {
                saved.putInt("Obsession", obs - 1);
            }
        }

        // 3. 高亲密暖腹共生：主人受伤时轻柔恢复
        if (ticks % 180 == 0) {
            int aff = saved.getIntOr("Affection", 20);
            if (aff >= 35 && host.getHealth() < host.getMaxHealth()) {
                host.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false));
                level.sendParticles(ParticleTypes.HEART, host.getX(), host.getY() + 1.1, host.getZ(), 2, 0.25, 0.2, 0.25, 0.01);
            }
        }

        // 4. 定期同步寄宿摘要给客户端（用于活体饥饿条 & B超共生界面）
        if (ticks % 40 == 0) {
            syncHostSummaryToClient(host);
        }

        // 5. 病娇护主机制：主人残血且身边有敌对怪物时，自动破体冲出甩尾哈气震飞全部敌人！
        if (ticks % 25 == 0 && JRConfig.isAscarisYandereEnabled()
                && host.getHealth() <= host.getMaxHealth() * 0.35f) {
            int obs = saved.getIntOr("Obsession", 15);
            if (obs >= 45) {
                List<Monster> threats = level.getEntitiesOfClass(Monster.class, host.getBoundingBox().inflate(4.2), LivingEntity::isAlive);
                if (!threats.isEmpty()) {
                    AscarisEntity guard = summonFromHostBody(host, false, false);
                    if (guard != null) {
                        guard.playTransientPose(AscarisPoseState.THREATEN, 44);
                        guard.triggerThreatenStrike(false);
                        host.sendSystemMessage(Component.literal(
                                "§c§o「不准伤害我最重要的人——统统给我滚开！」§7（体内的她冲出来甩尾哈气震飞了周围怪物！）"));
                    }
                }
            }
        }
    }

    /** 宿主进食时：体内寄宿的她开心共享营养（吃甜食触发可爱尾尖轻啄，绝不恶性掠夺！） */
    public static void onHostFinishedEating(ServerPlayer host, ItemStack stack) {
        CompoundTag saved = getHostedTag(host);
        if (saved == null) return;
        if (!stack.has(net.minecraft.core.component.DataComponents.FOOD)) return;

        ServerLevel level = (ServerLevel) host.level();
        boolean sweet = AscarisEntity.isSweetTreat(stack);
        int hunger = Math.min(100, saved.getIntOr("Hunger", 80) + (sweet ? 28 : 14));
        int aff = Math.min(100, saved.getIntOr("Affection", 20) + (sweet ? 5 : 1));
        int obs = Math.max(0, saved.getIntOr("Obsession", 15) - (sweet ? 8 : 2));
        float hp = Math.min(saved.getFloatOr("MaxHealth", 30f), saved.getFloatOr("Health", 30f) + (sweet ? 5f : 2f));
        saved.putInt("Hunger", hunger);
        saved.putInt("Affection", aff);
        saved.putInt("Obsession", obs);
        saved.putFloat("Health", hp);

        if (sweet) {
            level.playSound(null, host.blockPosition(), JRSounds.ASCARIS_BELL, SoundSource.PLAYERS, 0.5f, 1.35f);
            level.playSound(null, host.blockPosition(), JRSounds.ASCARIS_VOICE_HAPPY,
                    SoundSource.VOICE, 0.65f, 0.97f + level.getRandom().nextFloat() * 0.06f);
            level.sendParticles(ParticleTypes.HEART, host.getX(), host.getY() + 1.1, host.getZ(), 4, 0.3, 0.25, 0.3, 0.02);
            ServerPlayNetworking.send(host, new AscarisVisualEventPayload(
                    AscarisVisualEventPayload.EVENT_HUNGER_NIBBLE, host.getId(), 3, 1));
        }
        syncHostSummaryToClient(host);
    }

    /** 将宿主体内寄宿状态同步给客户端（param = 1 寄宿 / 0 无寄宿；value 打包 饱食/亲密/执念） */
    public static void syncHostSummaryToClient(ServerPlayer host) {
        CompoundTag saved = getHostedTag(host);
        if (saved == null) {
            ServerPlayNetworking.send(host, new AscarisVisualEventPayload(
                    AscarisVisualEventPayload.EVENT_HOST_SYNC, host.getId(), 0, 0));
            return;
        }
        int hunger = Mth.clamp(saved.getIntOr("Hunger", 80), 0, 100);
        int aff = Mth.clamp(saved.getIntOr("Affection", 20), 0, 100);
        int obs = Mth.clamp(saved.getIntOr("Obsession", 15), 0, 100);
        int packed = (hunger & 0x7F) | ((aff & 0x7F) << 7) | ((obs & 0x7F) << 14);
        ServerPlayNetworking.send(host, new AscarisVisualEventPayload(
                AscarisVisualEventPayload.EVENT_HOST_SYNC, host.getId(), 1, packed));
    }

    public static CompoundTag serializeAscaris(AscarisEntity a) {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("Health", a.getHealth());
        tag.putFloat("MaxHealth", a.getMaxHealth());
        tag.putInt("Hunger", a.getHunger());
        tag.putInt("Affection", a.getAffection());
        tag.putInt("Obsession", a.getObsession());
        tag.putFloat("VisualScale", a.getVisualScale());
        if (a.hasCustomName() && a.getCustomName() != null) {
            tag.putString("CustomName", a.getCustomName().getString());
        }
        return tag;
    }

    public static void applySerializedState(AscarisEntity a, CompoundTag tag, Player owner) {
        a.setTame(true, true);
        a.tame(owner);
        a.setOrderedToSit(false);
        a.setHunger(tag.getIntOr("Hunger", 80));
        a.setAffection(tag.getIntOr("Affection", 25));
        a.setObsession(tag.getIntOr("Obsession", 15));
        a.setVisualScale(tag.getFloatOr("VisualScale", 1.0f));
        float hp = tag.getFloatOr("Health", a.getMaxHealth());
        a.setHealth(Mth.clamp(hp, 2.0f, a.getMaxHealth()));
        String customName = tag.getStringOr("CustomName", "");
        if (!customName.isEmpty()) {
            a.setCustomName(Component.literal(customName));
        }
    }
}
