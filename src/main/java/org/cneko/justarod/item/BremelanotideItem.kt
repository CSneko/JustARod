package org.cneko.justarod.item

import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.world.World
import org.cneko.justarod.effect.JREffects

/*
这个的话呢.... 怎么说呢....
它...
可能...
就是...
咱也不了解
 */
class BremelanotideItem : Item(Settings()){
    companion object{
        const val CHEMICAL_FORMULA = "C50H68N14O10"
    }

    override fun use(world: World?, user: PlayerEntity?, hand: Hand?): TypedActionResult<ItemStack> {
        // 给予玩家发情&反胃效果
        user?.addEffect(StatusEffects.NAUSEA,600,1)
        user?.addEffect(JREffects.ESTRUS_EFFECT,5000,1)
        // 减少一个
        user?.getStackInHand(hand)?.decrement(1)
        return super.use(world, user, hand)
    }
}
