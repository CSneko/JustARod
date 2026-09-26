package org.cneko.justarod.item.rod
import org.cneko.justarod.JRIds

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
其实可以插到顶了
 */
class LongRodItem : BothUsedItem(JRIds.itemProps("long_rod").component(JRComponents.Companion.USED_TIME_MARK, 0).durability(2000).stacksTo(1)){
    override fun getInstruction(): EndRodInstructions {
        return EndRodInstructions.USE_ON_OTHER_INSERT
    }

    override fun canAcceptEntity(stack: ItemStack, entity: Entity): Boolean {
        return true
    }
    override fun useOnSelf(stack: ItemStack, world: Level?, entity: LivingEntity, slot: Int, selected: Boolean): InteractionResult {
        val result = super.useOnSelf(stack, world, entity, slot, selected)
        if (result == InteractionResult.SUCCESS){
            entity.hurt(JRDamageTypes.sexualExcitement(entity), 5.0f)
            (entity as? net.minecraft.world.entity.player.Player)?.sendSystemMessage(Component.translatable("item.justarod.long_rod.already_top"))
        }
        return result
    }

        override fun appendHoverText(
        stack: ItemStack,
        context: net.minecraft.world.item.Item.TooltipContext,
        display: net.minecraft.world.item.component.TooltipDisplay,
        adder: java.util.function.Consumer<Component>,
        type: TooltipFlag
    ) {
        super.appendHoverText(stack, context, display, adder, type)
        adder.accept(Component.translatable("item.justarod.long_rod.tooltip"))
    }
}