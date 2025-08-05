package org.cneko.justarod.item.medical

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.Hand
import org.cneko.justarod.entity.Pregnant

class SanitaryTowel(settings: Settings) : MedicalItem(settings) {

    // 舒适效果持续时间（10分钟）的常量，提高可读性
    private val COMFORT_DURATION_TICKS = 20 * 60 * 10

    /**
     * 卫生巾只能由当前处于月经周期的玩家使用
     */
    override fun canApply(user: PlayerEntity, target: LivingEntity, stack: ItemStack, hand: Hand): Boolean {
        // 目标必须是处于正确周期阶段的玩家
        return target is Pregnant &&
                target.menstruationCycle == Pregnant.MenstruationCycle.MENSTRUATION
    }

    /**
     * 提供无法使用的解释信息
     */
    override fun getFailureMessage(user: PlayerEntity, target: LivingEntity, stack: ItemStack): Text {
        if (target !is Pregnant) {
            return Text.of("§c此物品只能对特定玩家使用。")
        }

        // 这是最可能的失败原因
        val message = if (user == target) "§c你现在不需要使用这个。" else "§c对方现在不需要使用这个。"
        return Text.of(message)
    }

    /**
     * 对目标施加舒适效果
     */
    override fun applyEffect(user: PlayerEntity, target: LivingEntity, stack: ItemStack, hand: Hand) {
        // 从canApply可知目标必定是
        (target as Pregnant).menstruationComfort = COMFORT_DURATION_TICKS
    }

    /**
     * 消耗一片卫生巾
     */
    override fun consumeItem(user: PlayerEntity, target: LivingEntity, stack: ItemStack, hand: Hand) {
        if (!user.abilities.creativeMode) {
            stack.decrement(1)
        }
    }

    /**
     * 使用成功后提供提示信息
     */
    override fun getSuccessMessages(user: PlayerEntity, target: LivingEntity, stack: ItemStack): ActionMessages {
        val isSelf = user == target

        return ActionMessages(
            userSuccessMessage = if (isSelf) Text.of("§a你使用了卫生巾，感觉舒服多了。") else Text.of("§a你为 ${target.displayName?.string} 提供了卫生巾。"),
            targetSuccessMessage = if (isSelf) null else Text.of("§a${user.displayName?.string} 给了你一片卫生巾，你感觉舒服多了。")
        )
    }
}