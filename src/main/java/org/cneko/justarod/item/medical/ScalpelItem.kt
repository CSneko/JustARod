package org.cneko.justarod.item.medical

import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.Hand
import org.cneko.justarod.JREnchantments
import org.cneko.justarod.JRUtil.Companion.containsEnchantment
import org.cneko.justarod.effect.JREffects
import org.cneko.justarod.item.JRItems
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.Items
import org.cneko.justarod.entity.Pregnant
import org.cneko.justarod.item.rod.addEffect
import org.cneko.toneko.common.mod.util.TickTaskQueue

// 继承自 MedicalItem
class ScalpelItem(settings: Settings) : MedicalItem(settings.maxCount(1).maxDamage(4)) {

    // appendTooltip 保持不变，它与物品使用逻辑无关
    override fun appendTooltip(stack: ItemStack, context: TooltipContext, tooltip: MutableList<Text>, type: TooltipType) {
        super.appendTooltip(stack, context, tooltip, type)
        if (stack.containsEnchantment(JREnchantments.HYSTERECTOMY)) {
            tooltip.add(Text.of("§c使用它可进行子宫切除"))
            tooltip.add(Text.of("§c此操作会永久切除子宫，请谨慎使用！"))
        } else if (stack.containsEnchantment(JREnchantments.UTERUS_INSTALLATION)) {
            tooltip.add(Text.of("§a使用它可以安装子宫"))
        }else if (stack.containsEnchantment(JREnchantments.MASTECTOMY)){
            tooltip.add(Text.of("§c使用它可以进行乳房切除"))
        }
    }

    /**
     * 检查手术刀是否可用的所有先决条件
     */
    override fun canApply(user: PlayerEntity, target: LivingEntity, stack: ItemStack, hand: Hand): Boolean {
        // 通用检查
        if (stack.damage >= stack.maxDamage) return false // 物品损坏
        if (target !is Pregnant) return false // 目标必须是实现了Pregnant接口

        // 根据附魔检查特定条件
        if (stack.containsEnchantment(JREnchantments.HYSTERECTOMY)) {
            return !target.isHysterectomy && target.isFemale // 目标尚未切除子宫
        } else if (stack.containsEnchantment(JREnchantments.UTERUS_INSTALLATION)) {
            val offHandStack = user.getStackInHand(if (hand == Hand.MAIN_HAND) Hand.OFF_HAND else Hand.MAIN_HAND)
            return target.isHysterectomy && offHandStack.isOf(JRItems.UTERUS) && target.isFemale // 目标需要安装，且使用者副手持有子宫
        }else if (stack.containsEnchantment(JREnchantments.ARTIFICIAL_ABORTION)){
            return target.pregnant >0 && target.isFemale
        }else if (stack.containsEnchantment(JREnchantments.MASTECTOMY)) {
            return target.isFemale
        }

        return false // 没有对应附魔，无法使用
    }

    /**
     * 根据失败的条件提供具体消息
     */
    override fun getFailureMessage(user: PlayerEntity, target: LivingEntity, stack: ItemStack): Text {
        if (stack.damage >= stack.maxDamage) return Text.of("§c手术刀已损坏！")
        if (target !is Pregnant) return Text.of("§c只能对可进行此手术的玩家使用！")

        if (stack.containsEnchantment(JREnchantments.HYSTERECTOMY)) {
            if (target.isHysterectomy) return if (user == target) Text.of("§c你已经切除过了！") else Text.of("§c对方已经切除过了！")
        } else if (stack.containsEnchantment(JREnchantments.UTERUS_INSTALLATION)) {
            if (!target.isHysterectomy) return if (user == target) Text.of("§c你不需要安装子宫！") else Text.of("§c对方不需要安装子宫！")
            // canApply已经检查过副手，这里为了更明确的消息再次检查
            val offHandStack = user.getStackInHand(Hand.OFF_HAND) // 假设主手是手术刀
            if (!offHandStack.isOf(JRItems.UTERUS)) return Text.of("§c你的副手必须持有子宫才能执行此操作！")
        }else if (stack.containsEnchantment(JREnchantments.ARTIFICIAL_ABORTION)){
            return if (user == target) Text.of("§c你没有怀孕！") else Text.of("§c对方没有怀孕")
        }

        return Text.of("§c不满足使用条件。") // 默认失败消息
    }

    /**
     * 执行核心效果：改变状态、造成伤害、应用效果等
     */
    override fun applyEffect(user: PlayerEntity, target: LivingEntity, stack: ItemStack, hand: Hand) {
        // 目标必须是玩家才能设置isHysterectomy等属性
        if (target !is Pregnant) return

        // 通用效果：扣血和状态效果
        target.damage(target.world.damageSources.generic(), 10f)
        target.addStatusEffect(StatusEffectInstance(StatusEffects.SLOWNESS, 600, 1))
        target.addStatusEffect(StatusEffectInstance(StatusEffects.MINING_FATIGUE, 600, 1))
        target.addStatusEffect(StatusEffectInstance(Registries.STATUS_EFFECT.getEntry(JREffects.FAINT_EFFECT), 300, 1))

        // 根据附魔执行特定效果
        if (stack.containsEnchantment(JREnchantments.HYSTERECTOMY)) {
            target.isHysterectomy = true
            // 在目标位置掉落子宫
            target.dropStack(ItemStack(JRItems.UTERUS))
        } else if (stack.containsEnchantment(JREnchantments.UTERUS_INSTALLATION)) {
            target.isHysterectomy = false
            // 消耗副手的子宫
            val offHandStack = user.getStackInHand(if (hand == Hand.MAIN_HAND) Hand.OFF_HAND else Hand.MAIN_HAND)
            if (offHandStack.isOf(JRItems.UTERUS)) {
                offHandStack.decrement(1)
            }
        }else if (stack.containsEnchantment(JREnchantments.ARTIFICIAL_ABORTION)){
            val pre = target.pregnant
            if (pre > 20*60*20*5) {
                val task = TickTaskQueue()
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
            target.pregnant = 0
            target.dropStack(JRItems.MOLE.defaultStack)
        }else if (stack.containsEnchantment(JREnchantments.MASTECTOMY)){
            target.breastCancer = 0
            target.dropStack(Items.CHICKEN.defaultStack)
        }
    }

    /**
     * 消耗手术刀的耐久度
     */
    override fun consumeItem(user: PlayerEntity, target: LivingEntity, stack: ItemStack, hand: Hand) {
        // 每次成功使用消耗1点耐久度
        stack.damage(1, user, EquipmentSlot.MAINHAND)
    }

    /**
     * 获取成功操作后的提示消息
     */
    override fun getSuccessMessages(user: PlayerEntity, target: LivingEntity, stack: ItemStack): ActionMessages {
        val isSelf = user == target

        if (stack.containsEnchantment(JREnchantments.HYSTERECTOMY)) {
            return ActionMessages(
                userSuccessMessage = if (isSelf) Text.of("§a你成功为自己进行了子宫切除！") else Text.of("§a已为对方进行子宫切除！"),
                targetSuccessMessage = if (isSelf) null else Text.of("§c你被进行了子宫切除手术！")
            )
        } else if (stack.containsEnchantment(JREnchantments.UTERUS_INSTALLATION)) {
            return ActionMessages(
                userSuccessMessage = if (isSelf) Text.of("§a你成功为自己安装了子宫！") else Text.of("§a已为对方安装子宫！"),
                targetSuccessMessage = if (isSelf) null else Text.of("§a你被安装了子宫！")
            )
        }else if (stack.containsEnchantment(JREnchantments.MASTECTOMY)){
            return ActionMessages(
                userSuccessMessage = if (isSelf) Text.of("§a你成功切除了你自己的乳房") else Text.of("§a已为对方进行乳房切除"),
                targetSuccessMessage = if (isSelf) null else Text.of("§a你被进行了乳房切除！")
            )
        }
        // 不应该到达这里，但作为安全措施
        return ActionMessages(Text.of("操作完成。"), null)
    }
}