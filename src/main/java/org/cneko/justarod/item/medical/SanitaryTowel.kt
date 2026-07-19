package org.cneko.justarod.item.medical

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import org.cneko.justarod.entity.Pregnant

class SanitaryTowel(properties: Properties) : MedicalItem(properties) {

    // 舒适效果持续时间（10分钟）的常量，提高可读性
    private val COMFORT_DURATION_TICKS = 20 * 60 * 10

    /**
     * 卫生巾只能由当前处于月经周期的玩家使用
     */
    override fun canApply(user: Player, target: LivingEntity, stack: ItemStack, hand: InteractionHand): Boolean {
        // 目标必须是处于正确周期阶段的玩家
        return target is Pregnant && target.isFemale &&
                target.menstruationCycle == Pregnant.MenstruationCycle.MENSTRUATION
    }

    /**
     * 提供无法使用的解释信息
     */
    override fun getFailureMessage(user: Player, target: LivingEntity, stack: ItemStack): Component {
        if (target !is Pregnant) {
            return Component.literal("§c此物品只能对特定玩家使用。")
        }

        // 这是最可能的失败原因
        val message = if (user == target) "§c你现在不需要使用这个。" else "§c对方现在不需要使用这个。"
        return Component.literal(message)
    }

    /**
     * 对目标施加舒适效果
     */
    override fun applyEffect(user: Player, target: LivingEntity, stack: ItemStack, hand: InteractionHand) {
        // 从canApply可知目标必定是
        (target as Pregnant).menstruationComfort = COMFORT_DURATION_TICKS
    }

    /**
     * 消耗一片卫生巾
     */
    override fun consumeItem(user: Player, target: LivingEntity, stack: ItemStack, hand: InteractionHand) {
        if (!user.abilities.isCreative()) {
            stack.shrink(1)
        }
    }

    /**
     * 使用成功后提供提示信息
     */
    override fun getSuccessMessages(user: Player, target: LivingEntity, stack: ItemStack): ActionMessages {
        val isSelf = user == target

        return ActionMessages(
            userSuccessMessage = if (isSelf) Component.literal("§a你使用了卫生巾，感觉舒服多了。") else Component.literal("§a你为 ${target.displayName?.string} 提供了卫生巾。"),
            targetSuccessMessage = if (isSelf) null else Component.literal("§a${user.displayName?.string} 给了你一片卫生巾，你感觉舒服多了。")
        )
    }
}