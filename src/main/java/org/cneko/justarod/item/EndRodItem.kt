package org.cneko.justarod.item

import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.nbt.NbtCompound
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.world.World
import org.cneko.justarod.effects.JREffects

abstract class EndRodItem(settings: Settings) : Item(settings), EndRodItemInterface {
    override fun onUse(stack: ItemStack, world: World?, entity: LivingEntity, slot: Int, selected: Boolean) : ActionResult{
        // 添加计数
        val count = stack.getOrDefault(JRComponents.USED_TIME_MARK, 0)
        stack.set(JRComponents.USED_TIME_MARK, count + 1)
        return ActionResult.SUCCESS
    }

    override fun appendTooltip(stack: ItemStack?, context: TooltipContext?, tooltip: MutableList<Text>?, type: TooltipType?) {
        super.appendTooltip(stack, context, tooltip, type)
        // 将使用次数添加到tooltip中
        val markedCount: Int = stack?.getOrDefault(JRComponents.USED_TIME_MARK, 0)!!
        tooltip?.add(Text.translatable("item.justarod.end_rod.used_count", markedCount).formatted(Formatting.GREEN))
    }
}

open class SelfUsedItem(settings: Settings) : EndRodItem(settings), SelfUsedItemInterface {
    override fun inventoryTick(stack: ItemStack, world: World?, entity: Entity?, slot: Int, selected: Boolean) {
        super.inventoryTick(stack, world, entity, slot, selected)

        // 如果耐久为0或者实体不是LivingEntity，则不处理
        if(stack.damage == stack.maxDamage || entity !is LivingEntity) return

        val e:LivingEntity = entity

        // 如果放在副手
        if (e.offHandStack == stack){
            // 减少一点耐久 (即使没耐久也不损坏)
            stack.damage++
            // 执行
            useOnSelf(stack, world, e, slot, selected)
        }

    }


    override fun useOnSelf(stack: ItemStack, world: World?, entity: LivingEntity, slot: Int, selected: Boolean): ActionResult {
        onUse(stack, world, entity, slot, selected)
        // 给予玩家gc效果
        JREffects.TATER_EFFECT?.let {
            val orgasm = StatusEffectInstance(Registries.STATUS_EFFECT.getEntry(it), 100, 0)
            entity.addStatusEffect(orgasm)
        }
        return ActionResult.SUCCESS
        // 默认不处理
    }
}

/**
 * 给自己使用的末地烛接口
 */
interface SelfUsedItemInterface{
    /**
     * 使用末地烛
     * @param stack 使用的末地烛
     * @param world 使用的末地烛所在的世界
     * @param entity 使用的末地烛所在的实体
     * @param slot 使用的末地烛所在的槽位
     * @param selected 是否是选中的末地烛
     * @return 使用结果
     */
    fun useOnSelf(stack: ItemStack, world: World?, entity: LivingEntity, slot: Int, selected: Boolean):ActionResult
}
interface EndRodItemInterface{
    /**
     * 使用末地烛
     * @param stack 使用的末地烛
     * @param world 使用的末地烛所在的世界
     * @param entity 使用的末地烛所在的实体
     * @param slot 使用的末地烛所在的槽位
     * @param selected 是否是选中的末地烛
     */
    fun onUse(stack: ItemStack, world: World?, entity: LivingEntity, slot: Int, selected: Boolean) : ActionResult
}