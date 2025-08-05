package org.cneko.justarod.item.medical

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.text.Text
import net.minecraft.util.Hand
import org.cneko.justarod.entity.Pregnant

class SterilizationPills(settings: Settings) : MedicalItem(settings) {

    override fun appendTooltip(stack: ItemStack, context: TooltipContext, tooltip: MutableList<Text>, type: TooltipType) {
        super.appendTooltip(stack, context, tooltip, type)
        tooltip.add(Text.of("§c请谨慎使用！！！"))
        tooltip.add(Text.of("§c你没有悔改的机会！！！"))
    }

    /**
     * 检查是否可以进行绝育
     */
    override fun canApply(user: PlayerEntity, target: LivingEntity, stack: ItemStack, hand: Hand): Boolean {
        // 目标必须是玩家且尚未绝育
        return target is Pregnant && !target.isSterilization
    }

    /**
     * 获取失败时的消息
     */
    override fun getFailureMessage(user: PlayerEntity, target: LivingEntity, stack: ItemStack): Text {
        if (target !is Pregnant) {
            return Text.of("§c只能对玩家使用！")
        }
        if (target.isSterilization) {
            return if (user == target) Text.of("§c你已经绝育了！") else Text.of("§c对方已经绝育了！")
        }
        return Text.of("§c无法使用。") // 默认失败消息
    }

    /**
     * 执行绝育效果
     */
    override fun applyEffect(user: PlayerEntity, target: LivingEntity, stack: ItemStack, hand: Hand) {
        if (target is Pregnant) {
            target.isSterilization = true
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
     * 获取成功时的消息
     */
    override fun getSuccessMessages(user: PlayerEntity, target: LivingEntity, stack: ItemStack): ActionMessages {
        val isSelf = user == target

        return if (isSelf) {
            ActionMessages(
                userSuccessMessage = Text.of("§c你绝育了！"),
                targetSuccessMessage = null,
                userExtraMessage = Text.of("§c你将无法再次怀孕！")
            )
        } else {
            ActionMessages(
                userSuccessMessage = Text.of("§a已为对方进行绝育！"),
                targetSuccessMessage = Text.of("§c你被绝育了！"),
                targetExtraMessage = Text.of("§c你将无法再次怀孕！")
            )
        }
    }
}