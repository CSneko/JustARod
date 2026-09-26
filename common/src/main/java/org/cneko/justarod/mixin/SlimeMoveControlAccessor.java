package org.cneko.justarod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 打开史莱姆专用移动控制器的两个方法。
 *
 * <p>26.x 的 {@code Slime.SlimeMoveControl} 是包私有内部类，而且它**不认坐标**：
 * 它的 {@code tick()} 只朝自己的 {@code yRot} 字段转 + 按 {@code speedModifier} 起跳，
 * 那个 {@code yRot} 只能通过 {@code setDirection(yaw, aggressive)} 改。
 * 所以「让史莱姆去某个坐标」的正确姿势是：
 * <pre>
 *   setDirection(朝目标的 yaw, aggressive);
 *   setWantedMovement(speedMultiplier);   // 每 tick 调一次，否则跳一下就停
 * </pre>
 * 原版自己的 {@code SlimeAttackGoal} / {@code SlimeRandomDirectionGoal} 也是这么做的。
 *
 * <p>反例（旧代码）：调用基类 {@code MoveControl#setWantedPosition(x, y, z, speed)}——
 * 它只写入 {@code wantedX/Y/Z} 并把 operation 置为 MOVE_TO，而 {@code SlimeMoveControl}
 * 从不读这三个字段，于是史莱姆只会朝「出生时记录的朝向」原地跳，
 * 表现就是「不跟人走，只会远距离传送过来」。
 */
@Mixin(targets = "net.minecraft.world.entity.monster.Slime$SlimeMoveControl")
public interface SlimeMoveControlAccessor {

    /** 设定跳跃朝向与「是否激进」（激进会缩短起跳间隔） */
    @Invoker("setDirection")
    void justarod$setDirection(float yaw, boolean aggressive);

    /** 设定速度倍率并把控制器置为「正在移动」，从而持续起跳 */
    @Invoker("setWantedMovement")
    void justarod$setWantedMovement(double speedMultiplier);
}
