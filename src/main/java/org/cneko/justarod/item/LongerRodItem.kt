package org.cneko.justarod.item

import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.world.World
import org.cneko.justarod.damage.JRDamageTypes

/*
插到顶了后也还会露出很大一截（你想试试共用吗）
 */
class LongerRodItem: BothUsedItem(Settings().maxCount(1).maxDamage(1000).component(JRComponents.USED_TIME_MARK,0)) {
    override fun getInstruction(): EndRodInstructions {
        return EndRodInstructions.SELF_AND_OTHER_ATTACK
    }

    override fun canAcceptEntity(stack: ItemStack, entity: Entity): Boolean {
        return true
    }

    override fun useOnSelf(stack: ItemStack, world: World?, entity: LivingEntity, slot: Int, selected: Boolean): ActionResult {
        val result = super.useOnSelf(stack, world, entity, slot, selected)
        if (result == ActionResult.SUCCESS){
            entity.damage(JRDamageTypes.sexualExcitement(entity), 10.0f)
            entity.sendMessage(Text.translatable("item.justarod.long_rod.already_top"))
        }
        return result
    }

    override fun appendTooltip(
        stack: ItemStack?,
        context: TooltipContext?,
        tooltip: MutableList<Text>?,
        type: TooltipType?
    ) {
        super.appendTooltip(stack, context, tooltip, type)
        tooltip?.add(Text.translatable("item.justarod.longer_rod.tooltip"))
    }
}