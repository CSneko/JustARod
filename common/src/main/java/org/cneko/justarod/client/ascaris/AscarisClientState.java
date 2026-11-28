package org.cneko.justarod.client.ascaris;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.cneko.justarod.ascaris.AscarisEntity;
import org.cneko.justarod.ascaris.AscarisInfectionController;
import org.cneko.justarod.body.BodyWorld;
import org.cneko.justarod.client.JRKeyBindings;
import org.cneko.justarod.packet.AscarisActionPayload;
import org.cneko.justarod.packet.AscarisVisualEventPayload;
import org.jetbrains.annotations.Nullable;

/**
 * 客户端蛔虫视觉、双人缠绕、体内体温呼吸微晕与神经视窗遥控状态机：
 * - 丝滑淡入/淡出的 wrapVisualWeight 与 bodyWarmthWeight；
 * - 神经视窗连接期间采集鼠标朝向与 WASD/空格/R/G/X 输入发送给服务端遥控粉白蛔虫娘。
 */
public final class AscarisClientState {

    private static int nibbleTicks = 0;
    private static int nibbleDuration = 36;
    private static int rumbleTicks = 0;
    private static int coughTicks = 0;
    private static int crampTicks = 0;

    private static int expulsionTicks = 0;
    private static int expulsionMaxTicks = 60;
    private static int expulsionDrugType = AscarisInfectionController.DRUG_PAGODA;

    private static int wrapEntityId = -1;
    private static int wrapMode = AscarisEntity.WRAP_NONE;
    private static int lastVisualWrapMode = AscarisEntity.WRAP_NONE;
    private static int wrapStrugglePct = 0;
    private static int wrapActiveTicks = 0;
    private static int wrapStaleTicks = 0;
    private static float wrapVisualWeight = 0f;
    private static int tightenPulseTicks = 0;

    private static float lastPlayerYaw = 0f;
    private static float lastPlayerPitch = 0f;
    private static int manualLookCooldown = 0;
    private static float gazeDriftWeight = 0f;
    private static boolean prevJumpDown = false;
    private static boolean prevChestJumpDown = false;

    private static boolean hostedAscaris = false;
    private static int hostedHunger = 80;
    private static int hostedAffection = 25;
    private static int hostedObsession = 15;

    // 体内暖桃色体温呼吸微晕状态
    private static int bodyWarmthMode = 0;
    private static int bodyWarmthLastMode = 1;
    private static int bodyWarmthObsession = 0;
    private static float bodyWarmthWeight = 0f;

    // 粉白蛔虫娘体内硬核生理监护状态
    private static boolean physActive = false;
    private static int physPressure = 64;
    private static int physGlycogen = 76;
    private static int physWaste = 12;
    private static float physTempC = 37.2f;
    private static int physUterus = 46;
    private static int physRectum = 14;
    private static int physWallIntegrity = 100;
    private static int physDrugType = 0;
    private static boolean physPeristalsis = false;
    private static int physHostActivity = 0;
    private static int physOvary = 28;
    private static int physBolusCount = 0;
    private static int physBreachCount = 0;
    private static int physFlags = 0;

    // 跨维度神经视窗 + 体内遥控状态
    private static boolean neuralLinked = false;
    private static int neuralEntityId = -1;
    private static boolean neuralSitting = false;
    private static boolean neuralCrawling = false;
    private static int neuralHunger = 80;
    private static int neuralHpPct = 100;
    private static int neuralSendTick = 0;
    private static float neuralLastYaw = Float.NaN;
    private static float neuralLastPitch = Float.NaN;
    private static boolean prevNeuralJump = false;

    private AscarisClientState() {
    }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(AscarisVisualEventPayload.ID, (payload, context) ->
                context.client().execute(() -> accept(payload)));

        ClientTickEvents.END_CLIENT_TICK.register(AscarisClientState::tick);
    }

    private static void accept(AscarisVisualEventPayload p) {
        switch (p.eventType()) {
            case AscarisVisualEventPayload.EVENT_HUNGER_NIBBLE -> {
                nibbleDuration = 36;
                nibbleTicks = nibbleDuration;
            }
            case AscarisVisualEventPayload.EVENT_BELLY_RUMBLE -> rumbleTicks = 50;
            case AscarisVisualEventPayload.EVENT_LOFFLER_COUGH -> coughTicks = 32;
            case AscarisVisualEventPayload.EVENT_OBSTRUCTION_CRAMP -> crampTicks = 48;
            case AscarisVisualEventPayload.EVENT_EXPULSION_START -> {
                expulsionDrugType = p.param();
                expulsionMaxTicks = Math.max(20, p.value());
                expulsionTicks = expulsionMaxTicks;
            }
            case AscarisVisualEventPayload.EVENT_EXPULSION_FINISH -> {
                expulsionTicks = 0;
                rumbleTicks = 24;
            }
            case AscarisVisualEventPayload.EVENT_WRAP_SYNC -> {
                if (p.param() == AscarisEntity.WRAP_NONE || p.entityId() < 0) {
                    clearWrapSmooth();
                } else {
                    if (wrapEntityId != p.entityId()) {
                        wrapActiveTicks = 0;
                        manualLookCooldown = 0;
                        Minecraft mc = Minecraft.getInstance();
                        prevJumpDown = mc.options.keyJump.isDown();
                        if (mc.player != null) {
                            lastPlayerYaw = mc.player.getYRot();
                            lastPlayerPitch = mc.player.getXRot();
                        }
                        while (JRKeyBindings.PET_CANCEL_KEY.consumeClick()) { }
                        while (JRKeyBindings.SLIME_STRUGGLE_KEY.consumeClick()) { }
                    }
                    wrapEntityId = p.entityId();
                    wrapMode = p.param();
                    lastVisualWrapMode = p.param();
                    wrapStrugglePct = Mth.clamp(p.value(), 0, 100);
                    wrapStaleTicks = 0;
                }
            }
            case AscarisVisualEventPayload.EVENT_YANDERE_TIGHTEN -> {
                tightenPulseTicks = Math.max(24, p.value());
                manualLookCooldown = Math.min(manualLookCooldown, 8);
            }
            case AscarisVisualEventPayload.EVENT_HOST_SYNC -> {
                hostedAscaris = p.param() > 0;
                if (hostedAscaris) {
                    int packed = p.value();
                    hostedHunger = packed & 0x7F;
                    hostedAffection = (packed >> 7) & 0x7F;
                    hostedObsession = (packed >> 14) & 0x7F;
                }
            }
            case AscarisVisualEventPayload.EVENT_NEURAL_LINK -> {
                boolean active = p.param() > 0 && p.entityId() >= 0;
                if (active) {
                    if (!neuralLinked || neuralEntityId != p.entityId()) {
                        Minecraft mc = Minecraft.getInstance();
                        neuralLastYaw = Float.NaN;
                        neuralLastPitch = Float.NaN;
                        neuralSendTick = 0;
                        prevNeuralJump = mc.options.keyJump.isDown();
                        while (JRKeyBindings.PET_CANCEL_KEY.consumeClick()) { }
                        while (JRKeyBindings.SLIME_STRUGGLE_KEY.consumeClick()) { }
                        while (JRKeyBindings.SLIME_DEVOUR_KEY.consumeClick()) { }
                    }
                    neuralLinked = true;
                    neuralEntityId = p.entityId();
                    int packed = p.value();
                    neuralSitting = (packed & 1) != 0;
                    neuralCrawling = (packed & 2) != 0;
                    neuralHunger = Mth.clamp((packed >> 2) & 0x7F, 0, 100);
                    neuralHpPct = Mth.clamp((packed >> 9) & 0x7F, 0, 100);
                } else {
                    neuralLinked = false;
                    neuralEntityId = -1;
                    neuralLastYaw = Float.NaN;
                    neuralLastPitch = Float.NaN;
                }
            }
            case AscarisVisualEventPayload.EVENT_BODY_WARMTH -> {
                bodyWarmthMode = Math.max(0, p.param());
                bodyWarmthObsession = Mth.clamp(p.value(), 0, 100);
                if (bodyWarmthMode > 0) {
                    bodyWarmthLastMode = bodyWarmthMode;
                }
            }
            case AscarisVisualEventPayload.EVENT_PHYSIOLOGY_SYNC -> {
                if (p.entityId() < 0) {
                    physActive = false;
                } else {
                    physActive = true;
                    int vitals = p.entityId();
                    physPressure = Mth.clamp(vitals & 0x7F, 0, 100);
                    physGlycogen = Mth.clamp((vitals >> 7) & 0x7F, 0, 100);
                    physWaste = Mth.clamp((vitals >> 14) & 0x7F, 0, 100);
                    physTempC = (300 + ((vitals >> 21) & 0x7F)) / 10.0f;

                    int organs = p.param();
                    physUterus = Mth.clamp(organs & 0x7F, 0, 100);
                    physRectum = Mth.clamp((organs >> 7) & 0x7F, 0, 100);
                    physWallIntegrity = Mth.clamp((organs >> 14) & 0x7F, 0, 100);
                    physDrugType = (organs >> 21) & 0x3;
                    physPeristalsis = ((organs >> 23) & 1) != 0;
                    physHostActivity = (organs >> 24) & 0x7;

                    int cyc = p.value();
                    physOvary = Mth.clamp(cyc & 0x7F, 0, 100);
                    physBolusCount = (cyc >> 7) & 0xF;
                    physBreachCount = (cyc >> 11) & 0x1F;
                    physFlags = (cyc >> 16) & 0xFF;
                }
            }
        }
    }

    private static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            resetAll();
            return;
        }
        if (nibbleTicks > 0) nibbleTicks--;
        if (rumbleTicks > 0) rumbleTicks--;
        if (coughTicks > 0) coughTicks--;
        if (crampTicks > 0) crampTicks--;
        if (expulsionTicks > 0) expulsionTicks--;
        if (tightenPulseTicks > 0) tightenPulseTicks--;
        if (manualLookCooldown > 0) manualLookCooldown--;

        // 0. 体内暖桃色体温呼吸微晕平滑淡入/淡出
        boolean insideBodyDim = mc.level.dimension().equals(BodyWorld.DIMENSION);
        if (!insideBodyDim) {
            bodyWarmthMode = 0;
            physActive = false;
        }
        if (insideBodyDim && !neuralLinked && bodyWarmthMode > 0) {
            bodyWarmthWeight = Math.min(1.0f, bodyWarmthWeight + 0.045f);
        } else if (bodyWarmthWeight > 0f) {
            bodyWarmthWeight = Math.max(0f, bodyWarmthWeight - 0.055f);
        }

        // 0.5 神经视窗连接中：将鼠标朝向与 WASD / 空格 / R / G / X 实时传给服务端遥控粉白蛔虫娘！
        if (neuralLinked) {
            tickNeuralControls(mc);
        } else {
            prevNeuralJump = false;
        }

        // 缩小玩家坐在粉白蛔虫娘胸口上时：按空格键可以让她开心地原地轻跳，感受柔软弹性晃动！
        if (!neuralLinked && mc.player.getVehicle() instanceof AscarisEntity carrier && mc.screen == null) {
            boolean jumpDown = mc.options.keyJump.isDown();
            if (jumpDown && !prevChestJumpDown) {
                ClientPlayNetworking.send(new AscarisActionPayload(AscarisActionPayload.ACTION_CHEST_HOP, carrier.getId()));
            }
            prevChestJumpDown = jumpDown;
        } else {
            prevChestJumpDown = false;
        }

        if (wrapMode != AscarisEntity.WRAP_NONE) {
            wrapActiveTicks++;
            wrapStaleTicks++;
            wrapVisualWeight = Math.min(1.0f, wrapVisualWeight + 0.055f);

            AscarisEntity entity = getWrappedAscaris(mc);
            if (entity == null || !entity.isAlive() || !entity.getPoseState().isWrapping() || wrapStaleTicks > 40) {
                clearWrapSmooth();
                return;
            }

            var player = mc.player;

            // 1. 丝滑贴合到缠绕锚点（带方块碰撞保护，不穿墙、不被互推挤开）
            var anchor = entity.computeWrapAnchor();
            double dx = anchor.x - player.getX();
            double dz = anchor.z - player.getZ();
            double distSq = dx * dx + dz * dz;
            if (distSq > 0.0004 && distSq < 12.0) {
                double stepX = dx * 0.25;
                double stepZ = dz * 0.25;
                if (mc.level.noCollision(player, player.getBoundingBox().move(stepX, 0, stepZ))) {
                    player.setPos(player.getX() + stepX, player.getY(), player.getZ() + stepZ);
                }
            }
            var vel = player.getDeltaMovement();
            player.setDeltaMovement(0, vel.y, 0);

            // 2. 柔和视角引导
            float curYaw = player.getYRot();
            float curPitch = player.getXRot();
            float userDeltaYaw = Math.abs(Mth.wrapDegrees(curYaw - lastPlayerYaw));
            float userDeltaPitch = Math.abs(curPitch - lastPlayerPitch);
            if (userDeltaYaw > 0.35f || userDeltaPitch > 0.35f) {
                manualLookCooldown = 24;
                gazeDriftWeight = 0f;
            }

            if (mc.screen == null && manualLookCooldown == 0 && !mc.options.getCameraType().isMirrored()) {
                gazeDriftWeight = Math.min(1.0f, gazeDriftWeight + 0.06f);
                float ease = gazeDriftWeight * wrapVisualWeight;
                Vec3 toFace = entity.getEyePosition().subtract(player.getEyePosition());
                double horiz = Math.hypot(toFace.x, toFace.z);
                if (horiz > 0.08 && ease > 0.01f) {
                    float targetYaw = (float) (Mth.atan2(toFace.z, toFace.x) * Mth.RAD_TO_DEG) - 90.0f;
                    float targetPitch = (float) -(Mth.atan2(toFace.y, horiz) * Mth.RAD_TO_DEG);
                    float diffYaw = Mth.wrapDegrees(targetYaw - curYaw);
                    float diffPitch = targetPitch - curPitch;
                    boolean yandere = wrapMode == AscarisEntity.WRAP_YANDERE;
                    float maxStep = (tightenPulseTicks > 0 ? 3.8f : (yandere ? 2.2f : 1.6f)) * ease;
                    float factor = (tightenPulseTicks > 0 ? 0.12f : (yandere ? 0.065f : 0.048f)) * ease;
                    float nextYaw = curYaw + Mth.clamp(diffYaw * factor, -maxStep, maxStep);
                    float nextPitch = Mth.clamp(curPitch + Mth.clamp(diffPitch * factor, -maxStep * 0.7f, maxStep * 0.7f), -85f, 85f);
                    player.setYRot(nextYaw);
                    player.setXRot(nextPitch);
                    curYaw = nextYaw;
                    curPitch = nextPitch;
                }
            } else {
                gazeDriftWeight = Math.max(0f, gazeDriftWeight - 0.12f);
            }
            lastPlayerYaw = curYaw;
            lastPlayerPitch = curPitch;

            // 3. 挣脱/松开按键监听
            if (mc.screen == null && wrapActiveTicks >= 12) {
                boolean jumpNow = mc.options.keyJump.isDown();
                boolean cancelKey = JRKeyBindings.PET_CANCEL_KEY.consumeClick();
                boolean struggleKey = JRKeyBindings.SLIME_STRUGGLE_KEY.consumeClick();

                if (wrapMode == AscarisEntity.WRAP_AFFECTIONATE) {
                    if (cancelKey || struggleKey) {
                        ClientPlayNetworking.send(new AscarisActionPayload(AscarisActionPayload.ACTION_RELEASE, wrapEntityId));
                    }
                } else if (wrapMode == AscarisEntity.WRAP_PARASITIC || wrapMode == AscarisEntity.WRAP_YANDERE) {
                    if ((jumpNow && !prevJumpDown) || struggleKey) {
                        ClientPlayNetworking.send(new AscarisActionPayload(AscarisActionPayload.ACTION_STRUGGLE, wrapEntityId));
                    }
                }
                prevJumpDown = jumpNow;
            } else {
                prevJumpDown = mc.options.keyJump.isDown();
                while (JRKeyBindings.PET_CANCEL_KEY.consumeClick()) { }
                while (JRKeyBindings.SLIME_STRUGGLE_KEY.consumeClick()) { }
            }
        } else {
            if (wrapVisualWeight > 0f) {
                wrapVisualWeight = Math.max(0f, wrapVisualWeight - 0.055f);
                if (wrapVisualWeight == 0f) {
                    lastVisualWrapMode = AscarisEntity.WRAP_NONE;
                }
            }
            prevJumpDown = false;
        }
    }

    private static void tickNeuralControls(Minecraft mc) {
        if (mc.screen != null || mc.player == null) {
            prevNeuralJump = mc.options.keyJump.isDown();
            return;
        }
        // X 断开神经视窗返回体内
        if (JRKeyBindings.PET_CANCEL_KEY.consumeClick()) {
            ClientPlayNetworking.send(new AscarisActionPayload(AscarisActionPayload.ACTION_NEURAL_EXIT, 0));
            return;
        }
        // R 坐下 / 起身
        if (JRKeyBindings.SLIME_STRUGGLE_KEY.consumeClick()) {
            ClientPlayNetworking.send(new AscarisActionPayload(AscarisActionPayload.ACTION_NEURAL_SIT, 0));
        }
        // G 钻 1 格缝爬行 / 恢复站立
        if (JRKeyBindings.SLIME_DEVOUR_KEY.consumeClick()) {
            ClientPlayNetworking.send(new AscarisActionPayload(AscarisActionPayload.ACTION_NEURAL_CRAWL, 0));
        }
        // 空格 轻跳
        boolean jumpDown = mc.options.keyJump.isDown();
        if (jumpDown && !prevNeuralJump) {
            ClientPlayNetworking.send(new AscarisActionPayload(AscarisActionPayload.ACTION_NEURAL_HOP, 0));
        }
        prevNeuralJump = jumpDown;

        // 每 2 tick 同步鼠标视角与 WASD 挪步方向
        neuralSendTick++;
        if (neuralSendTick % 2 == 0) {
            float yaw = Mth.wrapDegrees(mc.player.getYRot());
            float pitch = Mth.clamp(mc.player.getXRot(), -65f, 65f);
            if (Float.isNaN(neuralLastYaw)
                    || Math.abs(Mth.wrapDegrees(yaw - neuralLastYaw)) > 0.6f
                    || Math.abs(pitch - neuralLastPitch) > 0.6f) {
                neuralLastYaw = yaw;
                neuralLastPitch = pitch;
                int yawFixed = Math.round(yaw * 10.0f) & 0xFFFF;
                int pitchFixed = Math.round(pitch * 10.0f) & 0xFFFF;
                int packed = yawFixed | (pitchFixed << 16);
                ClientPlayNetworking.send(new AscarisActionPayload(AscarisActionPayload.ACTION_NEURAL_LOOK, packed));
            }

            int moveBits = (mc.options.keyUp.isDown() ? 1 : 0)
                    | (mc.options.keyDown.isDown() ? 2 : 0)
                    | (mc.options.keyLeft.isDown() ? 4 : 0)
                    | (mc.options.keyRight.isDown() ? 8 : 0);
            if (moveBits != 0) {
                int yawFixed = Math.round(yaw * 10.0f) & 0xFFFF;
                int packed = (moveBits & 0xF) | (yawFixed << 4);
                ClientPlayNetworking.send(new AscarisActionPayload(AscarisActionPayload.ACTION_NEURAL_MOVE, packed));
            }
        }
    }

    /** 结束缠绕逻辑，但保留 wrapVisualWeight 让 HUD/柔焦/镜头呼吸平滑退场 */
    public static void clearWrapSmooth() {
        wrapEntityId = -1;
        wrapMode = AscarisEntity.WRAP_NONE;
        wrapStrugglePct = 0;
        wrapActiveTicks = 0;
        wrapStaleTicks = 0;
        gazeDriftWeight = 0f;
    }

    public static void resetAll() {
        nibbleTicks = 0;
        rumbleTicks = 0;
        coughTicks = 0;
        crampTicks = 0;
        expulsionTicks = 0;
        tightenPulseTicks = 0;
        wrapVisualWeight = 0f;
        lastVisualWrapMode = AscarisEntity.WRAP_NONE;
        hostedAscaris = false;
        prevChestJumpDown = false;
        bodyWarmthMode = 0;
        bodyWarmthWeight = 0f;
        bodyWarmthObsession = 0;
        physActive = false;
        physDrugType = 0;
        physFlags = 0;
        neuralLinked = false;
        neuralEntityId = -1;
        neuralLastYaw = Float.NaN;
        neuralLastPitch = Float.NaN;
        prevNeuralJump = false;
        clearWrapSmooth();
    }

    @Nullable
    public static AscarisEntity getWrappedAscaris(Minecraft mc) {
        if (wrapEntityId < 0 || mc.level == null) return null;
        Entity e = mc.level.getEntity(wrapEntityId);
        return (e instanceof AscarisEntity a) ? a : null;
    }

    public static float nibbleProgress() {
        return nibbleTicks <= 0 ? 0f : 1.0f - (nibbleTicks / (float) nibbleDuration);
    }

    public static boolean isNibbling() { return nibbleTicks > 0; }
    public static float rumbleIntensity() { return rumbleTicks / 50.0f; }
    public static float coughIntensity() { return coughTicks / 32.0f; }
    public static float crampIntensity() { return crampTicks / 48.0f; }

    public static boolean isExpelling() { return expulsionTicks > 0; }
    public static float expulsionProgress() {
        if (expulsionTicks <= 0) return 0f;
        return Mth.clamp(1.0f - (expulsionTicks / (float) Math.max(1, expulsionMaxTicks)), 0f, 1f);
    }
    public static int expulsionDrugType() { return expulsionDrugType; }

    public static boolean isWrapped() { return wrapMode != AscarisEntity.WRAP_NONE && wrapEntityId >= 0; }
    public static boolean hasWrapVisual() { return wrapVisualWeight > 0.005f && lastVisualWrapMode != AscarisEntity.WRAP_NONE; }
    public static float wrapVisualWeight() { return wrapVisualWeight; }
    public static int wrapMode() { return wrapMode; }
    public static int visualWrapMode() { return wrapMode != AscarisEntity.WRAP_NONE ? wrapMode : lastVisualWrapMode; }
    public static float wrapStruggle() { return wrapStrugglePct / 100.0f; }
    public static int wrapActiveTicks() { return wrapActiveTicks; }
    public static float tightenIntensity() { return Mth.clamp(tightenPulseTicks / 28.0f, 0f, 1f); }

    public static boolean hasHostedAscaris() { return hostedAscaris; }
    public static int hostedHunger() { return hostedHunger; }
    public static int hostedAffection() { return hostedAffection; }
    public static int hostedObsession() { return hostedObsession; }

    public static boolean hasBodyWarmth() { return bodyWarmthWeight > 0.005f && !neuralLinked; }
    public static float bodyWarmthWeight() { return bodyWarmthWeight; }
    public static int bodyWarmthMode() { return bodyWarmthMode > 0 ? bodyWarmthMode : bodyWarmthLastMode; }
    public static int bodyWarmthObsession() { return bodyWarmthObsession; }

    public static boolean hasPhysiology() { return physActive && !neuralLinked; }
    public static int physPressure() { return physPressure; }
    public static int physGlycogen() { return physGlycogen; }
    public static int physWaste() { return physWaste; }
    public static float physTempC() { return physTempC; }
    public static int physUterus() { return physUterus; }
    public static int physRectum() { return physRectum; }
    public static int physWallIntegrity() { return physWallIntegrity; }
    public static int physDrugType() { return physDrugType; }
    public static boolean physPeristalsis() { return physPeristalsis; }
    public static int physHostActivity() { return physHostActivity; }
    public static int physOvary() { return physOvary; }
    public static int physBolusCount() { return physBolusCount; }
    public static int physBreachCount() { return physBreachCount; }
    public static int physFlags() { return physFlags; }

    public static boolean isNeuralLinked() { return neuralLinked && neuralEntityId >= 0; }
    public static boolean isNeuralSitting() { return neuralSitting; }
    public static boolean isNeuralCrawling() { return neuralCrawling; }
    public static int neuralHunger() { return neuralHunger; }
    public static int neuralHpPct() { return neuralHpPct; }

    /** 供 SlimeDigestionBlurMixin 合并调用：缠绕入场与退场均随 wrapVisualWeight 丝滑过渡 */
    public static float blur() {
        float b = 0f;
        if (hasWrapVisual()) {
            int mode = visualWrapMode();
            float base = switch (mode) {
                case AscarisEntity.WRAP_YANDERE -> 0.34f + tightenIntensity() * 0.20f;
                case AscarisEntity.WRAP_PARASITIC -> 0.26f;
                default -> 0.16f;
            };
            b = Math.max(b, wrapVisualWeight * base);
        }
        if (isExpelling()) {
            b = Math.max(b, expulsionProgress() * 0.42f);
        }
        if (crampTicks > 0) {
            b = Math.max(b, crampIntensity() * 0.25f);
        }
        return b;
    }
}
