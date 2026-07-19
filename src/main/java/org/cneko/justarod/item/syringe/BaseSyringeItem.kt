package org.cneko.justarod.item.syringe

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import org.cneko.toneko.common.mod.items.BazookaItem.Ammunition

abstract class BaseSyringeItem(properties: Properties?) : Item(properties),Ammunition {
    override fun use(world: Level?, user: Player?, hand: InteractionHand?): InteractionResultHolder<ItemStack> {
        user?.let { applyEffect(it) }
        consumeItem(user, hand)
        return super.use(world, user, hand)
    }

    override fun useOnEntity(stack: ItemStack?, user: Player?, entity: LivingEntity?, hand: InteractionHand?): InteractionResult {
        entity?.let { applyEffect(it) }
        consumeItem(user, hand)
        return InteractionResult.SUCCESS
    }

    abstract fun applyEffect(target: LivingEntity)

    private fun consumeItem(user: Player?, hand: InteractionHand?) {
        user?.getItemInHand(hand)?.shrink(1)
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
