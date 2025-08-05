package org.cneko.justarod.item.medical

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.Hand
import org.cneko.justarod.entity.Pregnant
import org.cneko.justarod.item.JRItems
import org.cneko.justarod.item.addEffect
import org.cneko.toneko.common.mod.util.TickTaskQueue

class AbortionPillItem(settings: Settings) : MedicalItem(settings) {

    // 怀孕晚期阈值常量（7个Minecraft日）
    private val LATE_TERM_PREGNANCY_TICKS = 20 * 60 * 20 * 7

    /**
     * 堕胎药只能对怀孕的玩家使用
     */
    override fun canApply(user: PlayerEntity, target: LivingEntity, stack: ItemStack, hand: Hand): Boolean {
        // 目标必须是当前怀孕的玩家
        return target is Pregnant && target.pregnant > 0
    }

    /**
     * 提供无法使用药丸的具体原因
     */
    override fun getFailureMessage(user: PlayerEntity, target: LivingEntity, stack: ItemStack): Text {
        if (target !is Pregnant) {
            return Text.of("§c此物品只能对特定玩家使用。")
        }
        if (target.pregnant <= 0) {
            val message = if (user == target) "§c你没有怀孕，不需要使用这个。" else "§c对方没有怀孕。"
            return Text.of(message)
        }
        return Text.of("§c现在无法使用。") // 通用回退消息
    }

    /**
     * 应用堕胎药的核心效果
     */
    override fun applyEffect(user: PlayerEntity, target: LivingEntity, stack: ItemStack, hand: Hand) {
        // 从canApply已知目标是怀孕的
        target as Pregnant

        // 检查怀孕时长以确定副作用严重程度
        if (target.pregnant >= LATE_TERM_PREGNANCY_TICKS) {
            // 晚期：副作用较轻，但仍会造成伤害
            target.damage(target.world.damageSources.generic(), 1f)
            target.sendMessage(Text.of("§c手术过程似乎比较顺利，但你仍然感到一阵剧痛。"))

        } else {
            // 早/中期：更危险，伴随大出血和绝育风险
            val task = TickTaskQueue() // 假设每次使用都应新建实例
            target.damage(target.world.damageSources.generic(), 2f) // 初始伤害

            // 安排持续伤害模拟大出血
            for (i in 1..10) { // 从1开始产生延迟
                task.addTask(20 * i) {
                    if (!target.isDead) {
                        target.damage(target.world.damageSources.generic(), 2f)
                    }
                }
            }
            // 有20%概率因并发症导致永久绝育和恶心
            if (target.random.nextInt(5) == 0) {
                target.isSterilization = true
                target.addEffect(StatusEffects.NAUSEA, 0, 20 * 15) // 恶心效果持续15秒

                // 通知玩家出现并发症
                val complicationMsg = "§c并发症！手术对你造成了永久性损伤！"
                if (user != target) {
                    user.sendMessage(Text.of("§e并发症发生了..."), false)
                }
                target.sendMessage(Text.of(complicationMsg))
            }
        }

        // 主要效果：终止妊娠并掉落结果物品
        target.pregnant = 0
        target.dropStack(JRItems.MOLE.defaultStack)
    }

    /**
     * 使用后消耗一颗药丸
     */
    override fun consumeItem(user: PlayerEntity, target: LivingEntity, stack: ItemStack, hand: Hand) {
        if (!user.abilities.creativeMode) {
            stack.decrement(1)
        }
    }

    /**
     * 为使用者和目标提供成功消息
     */
    override fun getSuccessMessages(user: PlayerEntity, target: LivingEntity, stack: ItemStack): ActionMessages {
        val isSelf = user == target

        return ActionMessages(
            userSuccessMessage = if (isSelf) Text.of("§e你服下了药丸，终止了妊娠...") else Text.of("§e你帮助 ${target.displayName?.string} 终止了妊娠。"),
            targetSuccessMessage = if (isSelf) null else Text.of("§c${user.displayName?.string} 给你服用了堕胎药，终止了你的妊娠！")
        )
    }
}