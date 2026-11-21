package org.cneko.justarod.scale;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.cneko.justarod.JRAttributes;
import org.cneko.justarod.Justarod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩家「萌态体型」(justarod:player.cute_scale) 统一物理控制器：
 * - 不修改头身比例（尊重前置模组自带体型模型），仅做丝滑缩放插值与符合直觉的物理曲线联动；
 * - 解决原版直接改 minecraft:scale 时视角狂晃、速度过快/过慢、跳跃失衡、卡半砖、第三人称相机过远等痛点；
 * - 所有衍生修改均为瞬态修饰符（TRANSIENT），恢复 1.0 后自动完全卸载、零残留。
 */
public final class CuteScaleController {

    public static final float MIN_SCALE = 0.25f;
    public static final float MAX_SCALE = 4.0f;
    /** 体型 <= 此阈值时，可被粉白蛔虫娘捧起安放在胸口上 */
    public static final float CHEST_RIDE_MAX_SCALE = 0.65f;
    /** 骑在胸口上时若长大超过此阈值则自动温柔放回地面 */
    public static final float CHEST_RIDE_DISMOUNT_SCALE = 0.72f;

    private static final Identifier MOD_SCALE = id("cute_scale_size");
    private static final Identifier MOD_SPEED = id("cute_scale_speed");
    private static final Identifier MOD_JUMP = id("cute_scale_jump");
    private static final Identifier MOD_GRAVITY = id("cute_scale_gravity");
    private static final Identifier MOD_STEP = id("cute_scale_step");
    private static final Identifier MOD_FALL_DMG = id("cute_scale_fall_dmg");
    private static final Identifier MOD_SAFE_FALL = id("cute_scale_safe_fall");
    private static final Identifier MOD_BLOCK_REACH = id("cute_scale_block_reach");
    private static final Identifier MOD_ENTITY_REACH = id("cute_scale_entity_reach");
    private static final Identifier MOD_CAMERA_DIST = id("cute_scale_camera_dist");

    private static final Map<UUID, Float> SMOOTH_SCALE = new ConcurrentHashMap<>();

    private CuteScaleController() {
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Justarod.MODID, path);
    }

    /** 读取玩家当前的萌态体型属性值（非玩家生物回退原版 scale） */
    public static float getCuteScale(LivingEntity entity) {
        if (entity == null) return 1.0f;
        if (entity instanceof Player) {
            AttributeInstance attr = entity.getAttribute(JRAttributes.Companion.getPLAYER_CUTE_SCALE());
            if (attr != null) {
                return Mth.clamp((float) attr.getValue(), MIN_SCALE, MAX_SCALE);
            }
        }
        return entity.getScale();
    }

    /** 是否足够娇小，可以被蛔虫娘捧到胸口上 */
    public static boolean isSmallEnoughForChest(LivingEntity entity) {
        if (entity == null) return false;
        return getCuteScale(entity) <= CHEST_RIDE_MAX_SCALE || entity.getScale() <= CHEST_RIDE_MAX_SCALE;
    }

    /** 供客户端防抖混合器使用：当前实际生效的体型比例 */
    public static float getEffectiveClientScale(Player player) {
        if (player == null) return 1.0f;
        AttributeInstance attr = player.getAttribute(JRAttributes.Companion.getPLAYER_CUTE_SCALE());
        if (attr != null && Math.abs(attr.getValue() - 1.0) > 0.005) {
            return Mth.clamp(player.getScale(), MIN_SCALE, MAX_SCALE);
        }
        return player.getScale();
    }

    /** 服务端每 Tick 丝滑插值并同步联动全部物理修饰符 */
    public static void tickPlayer(Player player) {
        if (player.level().isClientSide()) return;
        AttributeInstance cuteAttr = player.getAttribute(JRAttributes.Companion.getPLAYER_CUTE_SCALE());
        if (cuteAttr == null) return;

        float target = Mth.clamp((float) cuteAttr.getValue(), MIN_SCALE, MAX_SCALE);
        float current = SMOOTH_SCALE.getOrDefault(player.getUUID(), target);

        // 丝滑过渡，避免单次注射生长/逆生长剂时眼高与碰撞箱瞬间跳变
        float diff = target - current;
        if (Math.abs(diff) <= 0.004f) {
            current = target;
        } else {
            float step = Mth.clamp(diff * 0.22f, -0.055f, 0.055f);
            if (Math.abs(step) < 0.004f) {
                step = Math.copySign(0.004f, diff);
            }
            current += step;
        }
        SMOOTH_SCALE.put(player.getUUID(), current);

        if (Math.abs(current - 1.0f) < 0.003f && Math.abs(target - 1.0f) < 0.003f) {
            clearAllModifiers(player);
            SMOOTH_SCALE.remove(player.getUUID());
            return;
        }

        applyDerivedModifiers(player, current);
    }

    /** 立即设置基础值并立刻对齐（供测试与指令使用） */
    public static void setCuteScaleImmediate(Player player, double targetScale) {
        AttributeInstance cuteAttr = player.getAttribute(JRAttributes.Companion.getPLAYER_CUTE_SCALE());
        if (cuteAttr == null) return;
        float clamped = Mth.clamp((float) targetScale, MIN_SCALE, MAX_SCALE);
        cuteAttr.setBaseValue(clamped);
        SMOOTH_SCALE.put(player.getUUID(), clamped);
        if (Math.abs(clamped - 1.0f) < 0.003f) {
            clearAllModifiers(player);
            SMOOTH_SCALE.remove(player.getUUID());
        } else {
            applyDerivedModifiers(player, clamped);
        }
    }

    public static void onPlayerRemoved(UUID uuid) {
        SMOOTH_SCALE.remove(uuid);
    }

    private static void applyDerivedModifiers(Player player, float s) {
        // 1. 模型与碰撞箱：丝滑跟随 s
        putTransient(player, Attributes.SCALE, MOD_SCALE, s - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

        // 2. 移动速度：亚线性曲线 s^0.42（缩小时轻快可控不乱窜，放大时沉稳不龟速）
        double speedFactor = Math.pow(s, 0.42) - 1.0;
        putTransient(player, Attributes.MOVEMENT_SPEED, MOD_SPEED, speedFactor, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

        // 3. 跳跃高度：s^0.30（缩小时不再一跳四层楼，保留轻巧跳跃感）
        double jumpFactor = Math.pow(s, 0.30) - 1.0;
        putTransient(player, Attributes.JUMP_STRENGTH, MOD_JUMP, jumpFactor, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

        // 4. 重力微调：缩小时略微轻盈柔和（s^0.20）
        double gravFactor = Math.pow(s, 0.20) - 1.0;
        putTransient(player, Attributes.GRAVITY, MOD_GRAVITY, gravFactor, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

        // 5. 跨步高度：缩小时保底 0.52（丝滑走上台阶与半砖不卡脚），放大时最高 2.2（巨人一步跨整格）
        double desiredStep = (s < 1.0f)
                ? Math.max(0.52, 0.6 * Math.pow(s, 0.18))
                : Math.min(2.2, 0.6 * Math.pow(s, 0.85));
        putTransient(player, Attributes.STEP_HEIGHT, MOD_STEP, desiredStep - 0.6, AttributeModifier.Operation.ADD_VALUE);

        // 6. 摔落伤害与安全摔落：娇小体型如小猫般轻盈耐摔
        double fallDmgFactor = Mth.clamp(Math.pow(s, 0.85) - 1.0, -0.78, 0.6);
        putTransient(player, Attributes.FALL_DAMAGE_MULTIPLIER, MOD_FALL_DMG, fallDmgFactor, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        double safeFallDelta = (s < 1.0f) ? (1.0 - s) * 1.6 : (s - 1.0) * 1.4;
        putTransient(player, Attributes.SAFE_FALL_DISTANCE, MOD_SAFE_FALL, safeFallDelta, AttributeModifier.Operation.ADD_VALUE);

        // 7. 方块/实体交互距离：柔和缩放 s^0.35（保留袖珍临场感又不至于摸不到箱子）
        double reachFactor = Math.pow(s, 0.35) - 1.0;
        putTransient(player, Attributes.BLOCK_INTERACTION_RANGE, MOD_BLOCK_REACH, reachFactor, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        putTransient(player, Attributes.ENTITY_INTERACTION_RANGE, MOD_ENTITY_REACH, reachFactor, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

        // 8. 第三人称相机拉距：缩小时自动拉近（0.25x -> 约 1.5 格），主角清晰可见不再缩成一个小点
        double camFactor = Mth.clamp(Math.pow(s, 0.72) - 1.0, -0.65, 1.35);
        putTransient(player, Attributes.CAMERA_DISTANCE, MOD_CAMERA_DIST, camFactor, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }

    private static void clearAllModifiers(Player player) {
        removeModifier(player, Attributes.SCALE, MOD_SCALE);
        removeModifier(player, Attributes.MOVEMENT_SPEED, MOD_SPEED);
        removeModifier(player, Attributes.JUMP_STRENGTH, MOD_JUMP);
        removeModifier(player, Attributes.GRAVITY, MOD_GRAVITY);
        removeModifier(player, Attributes.STEP_HEIGHT, MOD_STEP);
        removeModifier(player, Attributes.FALL_DAMAGE_MULTIPLIER, MOD_FALL_DMG);
        removeModifier(player, Attributes.SAFE_FALL_DISTANCE, MOD_SAFE_FALL);
        removeModifier(player, Attributes.BLOCK_INTERACTION_RANGE, MOD_BLOCK_REACH);
        removeModifier(player, Attributes.ENTITY_INTERACTION_RANGE, MOD_ENTITY_REACH);
        removeModifier(player, Attributes.CAMERA_DISTANCE, MOD_CAMERA_DIST);
    }

    private static void putTransient(Player player, Holder<Attribute> attr, Identifier modId,
                                     double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = player.getAttribute(attr);
        if (inst == null) return;
        AttributeModifier existing = inst.getModifier(modId);
        if (existing != null && Math.abs(existing.amount() - amount) < 0.0005 && existing.operation() == op) {
            return;
        }
        inst.addOrUpdateTransientModifier(new AttributeModifier(modId, amount, op));
    }

    private static void removeModifier(Player player, Holder<Attribute> attr, Identifier modId) {
        AttributeInstance inst = player.getAttribute(attr);
        if (inst != null && inst.hasModifier(modId)) {
            inst.removeModifier(modId);
        }
    }
}
