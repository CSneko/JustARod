package org.cneko.justarod.item

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.world.World

class SanitaryTowel(settings: Settings): Item(settings) {
    override fun use(world: World?, user: PlayerEntity?, hand: Hand?): TypedActionResult<ItemStack?>? {
        user?.menstruationComfort = 20*60*10 // 10 分钟的效果
        // 消耗一个
        if (user?.isCreative == false){
            user.getStackInHand(hand).decrement(1)
        }
        return super.use(world, user, hand)
    }
}