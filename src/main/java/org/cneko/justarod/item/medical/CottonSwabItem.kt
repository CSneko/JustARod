package org.cneko.justarod.item.medical

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.text.Text
import net.minecraft.util.Hand
import org.cneko.justarod.effect.JREffects
import org.cneko.justarod.entity.Pregnant
import org.cneko.justarod.item.JRComponents
import org.cneko.justarod.item.rod.hasEffect

/**
 * 棉签物品类，用于医疗采样
 */
class CottonSwabItem(settings: Settings) : MedicalItem(settings.maxCount(1)) {

    // 工具提示逻辑仅用于显示，保留在此类中
    override fun appendTooltip(stack: ItemStack, context: TooltipContext, tooltip: MutableList<Text>, type: TooltipType) {
        super.appendTooltip(stack, context, tooltip, type)
        stack.get(JRComponents.SECRETIONS_APPEARANCE)?.let { appearance ->
            tooltip.add(Text.of("§7颜色&气味: §f$appearance")) // 添加分泌物外观提示
        }
    }

    /**
     * 判断是否可以使用棉签
     * @return 当目标是玩家且棉签未被使用过时返回true
     */
    override fun canApply(user: PlayerEntity, target: LivingEntity, stack: ItemStack, hand: Hand): Boolean {
        return target is Pregnant && target.isFemale && !stack.contains(JRComponents.SECRETIONS_APPEARANCE)
    }

    /**
     * 获取使用失败时的提示消息
     */
    override fun getFailureMessage(user: PlayerEntity, target: LivingEntity, stack: ItemStack): Text {
        if (target !is Pregnant) {
            return Text.of("§c只能对玩家使用。") // 非玩家目标提示
        }
        if (stack.contains(JRComponents.SECRETIONS_APPEARANCE)) {
            return Text.of("§c这根棉签已经被使用过了哦。") // 已使用提示
        }
        return Text.of("§c无法使用。") // 通用失败提示
    }

    /**
     * 应用效果：从目标采集样本并将结果存储在棉签上
     * 读取目标状态但不修改
     */
    override fun applyEffect(user: PlayerEntity, target: LivingEntity, stack: ItemStack, hand: Hand) {
        target as Pregnant
        if (!target.isFemale) return

        // 使用when语句比长if-else-if链更清晰
        val appearance = when {
            target.hasEffect(JREffects.VAGINITIS_EFFECT) -> "§7灰白色，鱼腥味" // 阴道炎效果
            target.isHydatidiformMole -> "§4暗红色，无明显异味" // 葡萄胎
            target.menstruationCycle == Pregnant.MenstruationCycle.MENSTRUATION -> "§c鲜红色，轻微金属味" // 月经期
            target.hpv >= 20 * 60 * 20 * 3 -> "§6褐色，恶臭味" // HPV感染
            target.isPCOS -> "§o量过少，难以观察" // 多囊卵巢综合征
            else -> "§f乳白色半透明，无明显异味" // 正常情况
        }

        // 在物品堆栈上设置数据组件
        stack.set(JRComponents.SECRETIONS_APPEARANCE, appearance)
    }

    /**
     * 棉签使用后不会被消耗，而是改变状态
     * 因此此方法不做任何操作
     */
    override fun consumeItem(user: PlayerEntity, target: LivingEntity, stack: ItemStack, hand: Hand) {
        // 不消耗物品
    }

    /**
     * 获取采样操作的成功消息
     */
    override fun getSuccessMessages(user: PlayerEntity, target: LivingEntity, stack: ItemStack): ActionMessages {
        val isSelf = user == target // 是否对自己使用

        return ActionMessages(
            userSuccessMessage = if (isSelf) Text.of("§a你成功采集了样本。") else Text.of("§a你成功为 ${target.displayName?.string} 采集了样本。"), // 使用者成功消息
            targetSuccessMessage = if (isSelf) null else Text.of("§e${user.displayName?.string} 使用棉签采集了你的样本。") // 目标成功消息
        )
    }
}