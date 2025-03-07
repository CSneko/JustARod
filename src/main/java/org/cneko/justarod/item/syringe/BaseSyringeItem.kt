package org.cneko.justarod.item.syringe

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.world.World

abstract class BaseSyringeItem(settings: Settings?) : Item(settings) {
    override fun use(world: World?, user: PlayerEntity?, hand: Hand?): TypedActionResult<ItemStack> {
        user?.let { applyEffect(it) }
        consumeItem(user, hand)
        return super.use(world, user, hand)
    }

    override fun useOnEntity(stack: ItemStack?, user: PlayerEntity?, entity: LivingEntity?, hand: Hand?): ActionResult {
        entity?.let { applyEffect(it) }
        consumeItem(user, hand)
        return ActionResult.SUCCESS
    }

    abstract fun applyEffect(target: LivingEntity)

    private fun consumeItem(user: PlayerEntity?, hand: Hand?) {
        user?.getStackInHand(hand)?.decrement(1)
    }
}
