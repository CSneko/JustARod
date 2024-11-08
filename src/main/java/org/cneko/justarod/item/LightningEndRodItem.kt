package org.cneko.justarod.item

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.ItemStack
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.world.World

class LightningEndRodItem: SelfUsedItem(Settings().maxCount(1).maxDamage(1000).component(JRComponents.USED_TIME_MARK,0)) {
    override fun useOnSelf(
        stack: ItemStack,
        world: World?,
        entity: LivingEntity,
        slot: Int,
        selected: Boolean
    ): ActionResult {
        val result:ActionResult = super.useOnSelf(stack, world, entity, slot, selected)
        if (result == ActionResult.SUCCESS){
            // 中毒中毒~
            val e = StatusEffectInstance(StatusEffects.POISON, 200, 0)
            entity.addStatusEffect(e)
            return result
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
        tooltip?.add(Text.translatable("item.justarod.lightning_end_rod.tooltip"))
    }
}