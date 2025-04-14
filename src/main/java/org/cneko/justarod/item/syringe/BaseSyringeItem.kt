package org.cneko.justarod.item.syringe

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import org.cneko.toneko.common.mod.items.BazookaItem.Ammunition

abstract class BaseSyringeItem(settings: Settings?) : Item(settings),Ammunition {
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

    override fun hitOnAir(p0: LivingEntity?, p1: BlockPos?, p2: ItemStack?, p3: ItemStack?) {
    }

    override fun hitOnEntity(p0: LivingEntity?, p1: LivingEntity?, p2: ItemStack?, p3: ItemStack?) {
        p1?.let { applyEffect(it) }
    }

    override fun hitOnBlock(p0: LivingEntity?, p1: BlockPos?, p2: ItemStack?, p3: ItemStack?) {
    }

    override fun getMaxDistance(p0: ItemStack?, p1: ItemStack?): Float {
        return 30f
    }

    override fun getSpeed(p0: ItemStack?, p1: ItemStack?): Float {
        return 1f
    }

    override fun getCooldownTicks(p0: ItemStack?, p1: ItemStack?): Int {
        return 20
    }


}
