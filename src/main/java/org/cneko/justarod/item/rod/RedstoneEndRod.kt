package org.cneko.justarod.item.rod

import net.minecraft.entity.LivingEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResult
import net.minecraft.world.World
import org.cneko.justarod.item.JRComponents

class RedstoneEndRod:SelfUsedItem(Settings().maxCount(1).maxDamage(1000).component(JRComponents.Companion.USED_TIME_MARK, 0).component(
    JRComponents.Companion.SPEED, 2)) {
    override fun useOnSelf(stack: ItemStack, world: World?, entity: LivingEntity, slot: Int, selected: Boolean): ActionResult {
        return super.useOnSelf(stack, world, entity, slot, selected)
    }
}