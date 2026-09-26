package org.cneko.justarod.client.slime;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.cneko.justarod.entity.slime.TamedSlimeEntity;

/**
 * 客户端：手动「吞噬」技能的状态镜像，以及屏幕右侧提示要用的判断。
 *
 * <p>冷却与「前方有几个真正会被吞掉的目标」都由服务端算好、用
 * {@link org.cneko.justarod.packet.SlimeDevourStatePayload} 推过来——
 * 客户端**不复制**那套完整过滤规则（剩余空间账本、友军判定、体积计算），
 * 只在这里缓存结果。
 *
 * <p>唯一的例外是「提示该不该亮」的**粗判**（{@link #anyTargetAhead}）：
 * 服务端每 5 tick 才推一次状态，生物完全可能在这几十毫秒里走开或被吞掉，
 * 所以客户端自己再用本地实体列表快速核一遍「前方还有没有活物」。
 * 这个粗判**故意宽松**（只看距离与锥体，不看空间/友军/等级），
 * 因此最坏情况只是提示亮着、按下去被服务端拒绝，而不会出现「明明能吞却灰着」。
 */
public final class SlimeDevourClientState {

    /** 状态对应的史莱姆实体 id；-1 表示没有 */
    private static int slimeEntityId = -1;
    /** 冷却剩余 tick（服务端权威值） */
    private static int cooldownTicks = 0;
    /** 前方可吞目标数（服务端算好的） */
    private static int targetCount = 0;

    /** 与 {@link TamedSlimeEntity#DEVOUR_CONE_DEGREES} 保持一致（±60° → cos60°） */
    private static final double HINT_CONE_COS = Math.cos(Math.toRadians(60.0D));

    private SlimeDevourClientState() {
    }

    /** 收到服务端状态包 */
    public static void accept(int entityId, int cooldown, int targets) {
        slimeEntityId = entityId;
        cooldownTicks = Math.max(0, cooldown);
        targetCount = Math.max(0, targets);
    }

    /** 掉线 / 下马时清空，避免残留一个不存在的史莱姆的状态 */
    public static void reset() {
        slimeEntityId = -1;
        cooldownTicks = 0;
        targetCount = 0;
    }

    /** 当前骑着的、属于自己的史莱姆；没有则返回 null */
    public static TamedSlimeEntity riddenSlime() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return null;
        return player.getVehicle() instanceof TamedSlimeEntity slime && slime.isOwner(player) ? slime : null;
    }

    /** 冷却剩余秒数（向上取整，玩家看到「1s」时确实还剩一点冷却） */
    public static int cooldownSeconds() {
        return Math.max(0, (cooldownTicks + 19) / 20);
    }

    /**
     * 现在按下去能不能吞到东西。
     *
     * <p>四个条件：服务端有状态、且确实是「当前骑着的这头」（防止换乘后旧状态还没被覆盖）；
     * 冷却是 0；服务端数出的目标数 &gt; 0；最后再叠一层客户端粗判。
     */
    public static boolean canDevourNow() {
        TamedSlimeEntity slime = riddenSlime();
        if (slime == null || slime.getId() != slimeEntityId) return false;
        if (cooldownTicks > 0 || targetCount <= 0) return false;
        return anyTargetAhead(slime);
    }

    /** 是否正在冷却 */
    public static boolean onCooldown() {
        return cooldownTicks > 0;
    }

    /**
     * 客户端粗判：史莱姆自己报出的技能范围（随体积变化，见
     * {@link TamedSlimeEntity#devourRange()}）内、骑手前方 ±60° 锥体里有没有活物。
     *
     * <p>刻意宽松（不查剩余空间、不查友军、不查等级），宁可让提示亮着被服务端拒绝，
     * 也不要出现「明明能吞却显示灰的」。
     */
    private static boolean anyTargetAhead(TamedSlimeEntity slime) {
        LocalPlayer player = Minecraft.getInstance().player;
        Entity controller = slime.getControllingPassenger();
        Vec3 look = player != null ? player.getViewVector(1.0F) : slime.getViewVector(1.0F);
        Vec3 eye = slime.getEyePosition();
        AABB area = slime.getBoundingBox().inflate(slime.devourRange());
        for (LivingEntity candidate : slime.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e.isAlive() && e != slime && e != controller)) {
            if (candidate.isPassenger() || slime.storage().containsEntity(candidate.getUUID())) continue;
            if (candidate instanceof TamableAnimal animal && animal.isTame()) continue;
            Vec3 toTarget = candidate.position().add(0.0D, candidate.getBbHeight() * 0.5D, 0.0D).subtract(eye);
            if (toTarget.lengthSqr() <= 1.0E-4D) return true;
            if (look.dot(toTarget.normalize()) >= HINT_CONE_COS) return true;
        }
        return false;
    }

    /**
     * 屏幕右侧「吞噬」那一行的文案。
     *
     * @param keyName 吞噬键的显示名（已本地化）
     */
    public static Component devourHint(String keyName) {
        if (riddenSlime() == null) {
            return Component.translatable("justarod.slime.devour.hint", keyName);
        }
        if (cooldownTicks > 0) {
            return Component.translatable("justarod.slime.devour.cooldown", cooldownSeconds());
        }
        if (!canDevourNow()) {
            return Component.translatable("justarod.slime.devour.no_target");
        }
        return Component.translatable("justarod.slime.devour.hint", keyName);
    }

    /** 提示文字的颜色：可用=亮绿、没有目标=灰、冷却中=偏黄 */
    public static int devourHintColor() {
        if (cooldownTicks > 0) return 0xFFFFD24A;
        return canDevourNow() ? 0xFF7CFC7C : 0xFF8A8A8A;
    }
}
