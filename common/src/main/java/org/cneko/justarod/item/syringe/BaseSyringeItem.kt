package org.cneko.justarod.item.syringe

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import org.cneko.toneko.common.mod.items.BazookaItem.Ammunition

abstract class BaseSyringeItem(properties: Properties) : Item(properties), Ammunition {
    override fun use(world: Level, user: Player, hand: InteractionHand): InteractionResult {
        user.let { applyEffect(it) }
        consumeItem(user, hand)
        return super.use(world, user, hand)
    }

    override fun interactLivingEntity(stack: ItemStack, user: Player, entity: LivingEntity, hand: InteractionHand): InteractionResult {
        applyEffect(entity)
        consumeItem(user, hand)
        return InteractionResult.SUCCESS
    }

    abstract fun applyEffect(target: LivingEntity)

    private fun consumeItem(user: Player, hand: InteractionHand) {
        user.getItemInHand(hand).shrink(1)
    }

    override fun hitOnAir(shooter: LivingEntity?, pos: BlockPos?, bazooka: ItemStack?, ammunition: ItemStack?) {
    }

    override fun hitOnEntity(shooter: LivingEntity?, target: LivingEntity?, bazooka: ItemStack?, ammunition: ItemStack?) {
        target?.let { applyEffect(it) }
    }

    override fun hitOnBlock(shooter: LivingEntity?, pos: BlockPos?, bazooka: ItemStack?, ammunition: ItemStack?) {
    }

    override fun getMaxDistance(bazooka: ItemStack?, ammunition: ItemStack?): Float {
        return 30f
    }

    override fun getSpeed(bazooka: ItemStack?, ammunition: ItemStack?): Float {
        return 1f
    }

    override fun getCooldownTicks(bazooka: ItemStack?, ammunition: ItemStack?): Int {
        return 20
    }
}
