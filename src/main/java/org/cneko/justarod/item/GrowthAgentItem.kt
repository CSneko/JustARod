package org.cneko.justarod.item

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.world.World

class GrowthAgentItem: Item(Settings()) {
    override fun use(world: World?, user: PlayerEntity?, hand: Hand?): TypedActionResult<ItemStack> {
        // 增大目标的尺寸
        if(user?.attributes?.hasAttribute(EntityAttributes.GENERIC_SCALE) == true){
            val ins =user.attributes.getCustomInstance(EntityAttributes.GENERIC_SCALE)
            val value = ins?.baseValue ?: 1.0
            if (value < 4){
                ins?.baseValue = value + 0.1
            }
        }
        // 减少一个
        user?.getStackInHand(hand)?.decrement(1)
        return super.use(world, user, hand)
    }

    override fun useOnEntity(stack: ItemStack?, user: PlayerEntity?, entity: LivingEntity?, hand: Hand?): ActionResult {
        if (entity != null) {
            // 增大目标的尺寸
            if (entity.attributes?.hasAttribute(EntityAttributes.GENERIC_SCALE) == true) {
                val ins = entity.attributes.getCustomInstance(EntityAttributes.GENERIC_SCALE)
                val value = ins?.baseValue ?: 1.0
                if (value < 4) {
                    ins?.baseValue = value + 0.1
                }
            }
            // 减少一个
            user?.getStackInHand(hand)?.decrement(1)
            return ActionResult.SUCCESS
        }
        return super.useOnEntity(stack, user, entity, hand)
    }

}