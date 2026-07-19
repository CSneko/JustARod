package org.cneko.justarod.item.medical

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import org.cneko.justarod.entity.Pregnant

class SterilizationPills(properties: Properties) : MedicalItem(properties) {

    override fun appendTooltip(stack: ItemStack, context: TooltipContext, tooltip: MutableList<Component>, type: TooltipFlag) {
        super.appendHoverText(stack, context, tooltip, type)
        tooltip.add(Component.literal("§c请谨慎使用！！！"))
        tooltip.add(Component.literal("§c你没有悔改的机会！！！"))
    }

    /**
     * 检查是否可以进行绝育
     */
    override fun canApply(user: Player, target: LivingEntity, stack: ItemStack, hand: InteractionHand): Boolean {
        // 目标必须是玩家且尚未绝育
        return target is Pregnant && !target.isSterilization
    }

    /**
     * 获取失败时的消息
     */
    override fun getFailureMessage(user: Player, target: LivingEntity, stack: ItemStack): Component {
        if (target !is Pregnant) {
            return Component.literal("§c只能对玩家使用！")
        }
        if (target.isSterilization) {
            return if (user == target) Component.literal("§c你已经绝育了！") else Component.literal("§c对方已经绝育了！")
        }
        return Component.literal("§c无法使用。") // 默认失败消息
    }

    /**
     * 执行绝育效果
     */
    override fun applyEffect(user: Player, target: LivingEntity, stack: ItemStack, hand: InteractionHand) {
        if (target is Pregnant) {
            target.isSterilization = true
        }
    }

    /**
     * 消耗一粒药丸
     */
    override fun consumeItem(user: Player, target: LivingEntity, stack: ItemStack, hand: InteractionHand) {
        if (!user.abilities.isCreative()) {
            stack.shrink(1)
        }
    }

    /**
     * 获取成功时的消息
     */
    override fun getSuccessMessages(user: Player, target: LivingEntity, stack: ItemStack): ActionMessages {
        val isSelf = user == target

        return if (isSelf) {
            ActionMessages(
                userSuccessMessage = Component.literal("§c你绝育了！"),
                targetSuccessMessage = null,
                userExtraMessage = Component.literal("§c你将无法再次怀孕！")
            )
        } else {
            ActionMessages(
                userSuccessMessage = Component.literal("§a已为对方进行绝育！"),
                targetSuccessMessage = Component.literal("§c你被绝育了！"),
                targetExtraMessage = Component.literal("§c你将无法再次怀孕！")
            )
        }
    }
}