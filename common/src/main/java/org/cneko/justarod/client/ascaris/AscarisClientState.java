package org.cneko.justarod.client.ascaris;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.cneko.justarod.ascaris.AscarisEntity;
import org.cneko.justarod.ascaris.AscarisInfectionController;
import org.cneko.justarod.client.JRKeyBindings;
import org.cneko.justarod.packet.AscarisActionPayload;
import org.cneko.justarod.packet.AscarisVisualEventPayload;
import org.jetbrains.annotations.Nullable;

/**
 * 客户端蛔虫视觉与双人缠绕状态机：
 * - 丝滑淡入/淡出的 wrapVisualWeight（挣脱松开时 0.9s 平滑退场，不再瞬间跳切）；
 * - 尊重玩家手动转鼠标的柔和视角牵引（转动鼠标时立即让步不弹回，停止转动后再缓缓望向她）。
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

        // 缩小玩家坐在粉白蛔虫娘胸口上时：按空格键可以让她开心地原地轻跳，感受柔软弹性晃动！
        if (mc.player.getVehicle() instanceof AscarisEntity carrier && mc.screen == null) {
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

            // 2. 柔和视角引导：检测玩家是否主动动鼠标，动鼠标时立刻礼让不弹回；松手停顿后再由 0 平滑渐入地望向她的脸
            float curYaw = player.getYRot();
            float curPitch = player.getXRot();
            float userDeltaYaw = Math.abs(Mth.wrapDegrees(curYaw - lastPlayerYaw));
            float userDeltaPitch = Math.abs(curPitch - lastPlayerPitch);
            if (userDeltaYaw > 0.35f || userDeltaPitch > 0.35f) {
                manualLookCooldown = 24; // 玩家正在主动转视角 -> 停止自动拉回，绝不突兀弹回！
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

            // 3. 挣脱/松开按键监听（避开 Shift 冲突与 12 tick 起手保护）
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
            // 挣脱/松开后的 0.9 秒平滑淡出过渡，避免画面和柔焦瞬间跳切
            if (wrapVisualWeight > 0f) {
                wrapVisualWeight = Math.max(0f, wrapVisualWeight - 0.055f);
                if (wrapVisualWeight == 0f) {
                    lastVisualWrapMode = AscarisEntity.WRAP_NONE;
                }
            }
            prevJumpDown = false;
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
