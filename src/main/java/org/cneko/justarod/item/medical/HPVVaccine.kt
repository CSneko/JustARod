package org.cneko.justarod.item.medical

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.Hand
import org.cneko.justarod.effect.PregnantEffect
import org.cneko.justarod.entity.Pregnant

class HPVVaccine(settings: Settings) : MedicalItem(settings) {

    /**
     * 当目标为未免疫的玩家时方可接种
     */
    override fun canApply(user: PlayerEntity, target: LivingEntity, stack: ItemStack, hand: Hand): Boolean {
        // 目标必须是玩家且未获得免疫力
        return target is Pregnant && !target.isImmune2HPV
    }

    /**
     * 获取接种失败时的提示信息
     */
    override fun getFailureMessage(user: PlayerEntity, target: LivingEntity, stack: ItemStack): Text {
        if (target !is Pregnant) {
            return Text.of("§c此物品只能对玩家使用。")
        }
        if (target.isImmune2HPV) {
            val message = if (user == target) "§c你已经接种过疫苗，获得了免疫力。" else "§c对方已经接种过疫苗了。"
            return Text.of(message)
        }
        return Text.of("§c无法使用。") // 通用回退提示
    }

    /**
     * 施加免疫效果
     */
    override fun applyEffect(user: PlayerEntity, target: LivingEntity, stack: ItemStack, hand: Hand) {
        // 通过canApply已确认target是pregnant
        (target as Pregnant).isImmune2HPV = true
    }

    /**
     * 消耗疫苗剂量
     */
    override fun consumeItem(user: PlayerEntity, target: LivingEntity, stack: ItemStack, hand: Hand) {
        if (!user.abilities.creativeMode) {
            stack.decrement(1)
        }
    }

    /**
     * 获取接种成功的提示消息
     */
    override fun getSuccessMessages(user: PlayerEntity, target: LivingEntity, stack: ItemStack): ActionMessages {
        val isSelf = user == target

        return ActionMessages(
            userSuccessMessage = if (isSelf) Text.of("§a你接种了HPV疫苗，现在你已获得免疫力。") else Text.of("§a你为 ${target.displayName?.string} 接种了HPV疫苗。"),
            targetSuccessMessage = if (isSelf) null else Text.of("§a${user.displayName?.string} 为你接种了HPV疫苗，你获得了免疫力。")
        )
    }
}