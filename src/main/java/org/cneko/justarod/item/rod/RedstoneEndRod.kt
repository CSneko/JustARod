package org.cneko.justarod.item.rod

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.InteractionResult
import net.minecraft.world.level.Level
import org.cneko.justarod.item.JRComponents

/*
自动款的好用喵，不费力呢♡
 */
class RedstoneEndRod:SelfUsedItem(Settings().maxCount(1).maxDamage(1000).component(JRComponents.Companion.USED_TIME_MARK, 0).component(
    JRComponents.Companion.SPEED, 2)) {
    override fun useOnSelf(stack: ItemStack, world: Level?, entity: LivingEntity, slot: Int, selected: Boolean): InteractionResult {
        return super.useOnSelf(stack, world, entity, slot, selected)
    }
}