package org.cneko.justarod.item.electric

import net.minecraft.component.DataComponentTypes
import net.minecraft.entity.LivingEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.world.World
import org.cneko.justarod.damage.JRDamageTypes
import org.cneko.justarod.item.JRComponents

/*
其实漏电也不错的说... 低压的话还是挺棒的哦
 */
open class BasicElectricRodItem: SelfUsedElectricRodItem(Settings().component(JRComponents.USED_TIME_MARK,0).component(JRComponents.SPEED,10).maxDamage(10000)) {

    override fun appendTooltip(
        stack: ItemStack?,
        context: TooltipContext?,
        tooltip: MutableList<Text>?,
        type: TooltipType?
    ) {
        tooltip?.add(Text.translatable("item.justarod.basic_electric_rod.tooltip"))
        super.appendTooltip(stack, context, tooltip, type)
    }

    override fun damage(stack: ItemStack, amount: Int, world: World?) {
        super<SelfUsedElectricRodItem>.damage(stack, amount, world)
        // 随机额外减少
        if (stack.damage!=stack.maxDamage){
            val random = world?.random?.nextInt(500)
            if (random != null) {
                if (random+stack.damage>=stack.maxDamage){
                    stack.damage = stack.maxDamage
                }
            }
        }
    }

    override fun useOnSelf(
        stack: ItemStack,
        world: World?,
        entity: LivingEntity,
        slot: Int,
        selected: Boolean
    ): ActionResult {
        val result = super.useOnSelf(stack, world, entity, slot, selected)
        if (result == ActionResult.SUCCESS){
            // 减少实体0.1~1.5血量
            val random = world?.random?.nextInt(5)?.plus(1)
            if (random != null) {
                entity.damage(JRDamageTypes.sexualExcitement(entity), random.toFloat())
            }
        }
        return result
    }
}