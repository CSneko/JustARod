package org.cneko.justarod.item.medical

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.Hand
import org.cneko.justarod.entity.Pregnant
import org.cneko.justarod.item.addEffect

// 效果持续时间常量（1个Minecraft日）
private const val BIRTH_CONTROL_DURATION_TICKS = 20 * 60 * 20

class BrithControllingPill(settings: Settings) : MedicalItem(settings) {

    /**
     * 任何玩家都可以使用该药丸
     * 没有特定的"失败"前提条件，
     * 因为它的效果（如副作用）即使在玩家已经处于药效下也会生效
     * 因此总是返回true
     */
    override fun canApply(user: PlayerEntity, target: LivingEntity, stack: ItemStack, hand: Hand): Boolean {
        // 该药丸始终可以对玩家使用
        return target is Pregnant
    }

    /**
     * 由于canApply非常宽松，除非目标不是玩家，否则很少会调用此方法
     */
    override fun getFailureMessage(user: PlayerEntity, target: LivingEntity, stack: ItemStack): Text {
        if (target !is Pregnant) {
            return Text.of("§c此物品只能对玩家使用。")
        }
        return Text.of("§c无法使用。") // 通用回退消息
    }

    /**
     * 应用避孕药的效果
     */
    override fun applyEffect(user: PlayerEntity, target: LivingEntity, stack: ItemStack, hand: Hand) {
        target as Pregnant // 从canApply可知目标是

        // 如果玩家已经处于药效下，则应用副作用
        if (target.brithControlling > 0) {
            val randomTime = target.random.nextInt(BIRTH_CONTROL_DURATION_TICKS)
            val change = if (target.random.nextBoolean()) randomTime else -randomTime

            target.menstruation += change // 扰乱月经周期
            target.addEffect(StatusEffects.NAUSEA, 20 * 20, 0) // 20秒的恶心效果

            // 发送关于副作用的提示消息
            target.sendMessage(Text.of("§c重复服药扰乱了你的周期，你感到一阵恶心。"))
        }

        // 应用或重置主效果持续时间
        target.brithControlling = BIRTH_CONTROL_DURATION_TICKS

        // 有1/3的几率治愈多囊卵巢综合征
        if (target.isPCOS && target.random.nextInt(3) == 0) {
            target.isPCOS = false
            // 通知玩家这个积极的副作用
            target.sendMessage(Text.of("§a你感觉身体状况有所好转！"))
        }
    }

    /**
     * 消耗一粒药丸
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
            userSuccessMessage = if (isSelf) Text.of("§a你服下了一粒避孕药。") else Text.of("§a你给 ${target.displayName?.string} 服用了一粒避孕药。"),
            targetSuccessMessage = if (isSelf) null else Text.of("§e${user.displayName?.string} 给你服用了一粒避孕药。")
        )
    }
}