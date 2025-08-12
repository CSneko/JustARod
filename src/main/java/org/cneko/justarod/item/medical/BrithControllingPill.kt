package org.cneko.justarod.item.medical

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.Hand
import org.cneko.justarod.entity.Pregnant
import org.cneko.justarod.item.rod.addEffect

private const val BIRTH_CONTROL_DURATION_TICKS = 20 * 60 * 20

class BrithControllingPill(settings: Settings) : MedicalItem(settings) {

    override fun canApply(user: PlayerEntity, target: LivingEntity, stack: ItemStack, hand: Hand): Boolean {
        return target is Pregnant && target.isFemale()
    }

    override fun getFailureMessage(user: PlayerEntity, target: LivingEntity, stack: ItemStack): Text {
        if (target !is Pregnant || !target.isFemale()) {
            return Text.of("§c此物品只能对女性玩家使用。")
        }
        return Text.of("§c无法使用。")
    }

    override fun applyEffect(user: PlayerEntity, target: LivingEntity, stack: ItemStack, hand: Hand) {
        target as Pregnant

        if (target.brithControlling > 0) {
            val randomTime = target.random.nextInt(BIRTH_CONTROL_DURATION_TICKS)
            val change = if (target.random.nextBoolean()) randomTime else -randomTime
            target.menstruation += change
            target.addEffect(StatusEffects.NAUSEA, 20 * 20, 0)
            target.sendMessage(Text.of("§c重复服药扰乱了你的周期，你感到一阵恶心。"))
        }

        target.brithControlling = BIRTH_CONTROL_DURATION_TICKS

        if (target.isPCOS && target.random.nextInt(3) == 0) {
            target.isPCOS = false
            target.sendMessage(Text.of("§a你感觉身体状况有所好转！"))
        }
    }

    override fun consumeItem(user: PlayerEntity, target: LivingEntity, stack: ItemStack, hand: Hand) {
        if (!user.abilities.creativeMode) {
            stack.decrement(1)
        }
    }

    override fun getSuccessMessages(user: PlayerEntity, target: LivingEntity, stack: ItemStack): ActionMessages {
        val isSelf = user == target
        return ActionMessages(
            userSuccessMessage = if (isSelf) Text.of("§a你服下了一粒避孕药。") else Text.of("§a你给 ${target.displayName?.string} 服用了一粒避孕药。"),
            targetSuccessMessage = if (isSelf) null else Text.of("§e${user.displayName?.string} 给你服用了一粒避孕药。")
        )
    }
}
