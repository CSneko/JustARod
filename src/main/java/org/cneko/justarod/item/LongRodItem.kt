package org.cneko.justarod.item

import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.world.World
import org.cneko.justarod.damage.JRDamageTypes
/*
其实可以插到顶了
 */
class LongRodItem : BothUsedItem(Settings().component(JRComponents.USED_TIME_MARK, 0).maxDamage(2000).maxCount(1)){
    override fun getInstruction(): EndRodInstructions {
        return EndRodInstructions.USE_ON_OTHER_INSERT
    }

    override fun canAcceptEntity(stack: ItemStack, entity: Entity): Boolean {
        return true
    }
    override fun useOnSelf(stack: ItemStack, world: World?, entity: LivingEntity, slot: Int, selected: Boolean): ActionResult {
        val result = super.useOnSelf(stack, world, entity, slot, selected)
        if (result == ActionResult.SUCCESS){
            entity.damage(JRDamageTypes.sexualExcitement(entity), 5.0f)
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
        tooltip?.add(Text.translatable("item.justarod.long_rod.tooltip"))
    }
}