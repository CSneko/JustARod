package org.cneko.justarod.item.rod

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.level.Level
import org.cneko.justarod.damage.JRDamageTypes
import org.cneko.justarod.item.JRComponents

/*
插到顶了后也还会露出很大一截（你想试试共用吗）
 */
class LongerRodItem: BothUsedItem(Settings().maxCount(1).maxDamage(1000).component(JRComponents.Companion.USED_TIME_MARK,0)) {
    override fun getInstruction(): EndRodInstructions {
        return EndRodInstructions.SELF_AND_OTHER_ATTACK
    }

    override fun canAcceptEntity(stack: ItemStack, entity: Entity): Boolean {
        return true
    }

    override fun useOnSelf(stack: ItemStack, world: Level?, entity: LivingEntity, slot: Int, selected: Boolean): InteractionResult {
        val result = super.useOnSelf(stack, world, entity, slot, selected)
        if (result == InteractionResult.SUCCESS){
            entity.hurt(JRDamageTypes.sexualExcitement(entity), 10.0f)
            entity.sendSystemMessage(Component.translatable("item.justarod.long_rod.already_top"))
        }
        return result
    }

    override fun appendTooltip(
        stack: ItemStack?,
        context: TooltipContext?,
        tooltip: MutableList<Component>?,
        type: TooltipFlag?
    ) {
        super.appendHoverText(stack, context, tooltip, type)
        tooltip?.add(Component.translatable("item.justarod.longer_rod.tooltip"))
    }
}