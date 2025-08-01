package org.cneko.justarod.item

import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.world.World

class BrithControllingPill(settings: Item.Settings): Item(settings) {
    override fun use(world: World?, user: PlayerEntity?, hand: Hand?): TypedActionResult<ItemStack?>? {
        if (user == null || world?.isClient == true){
            return super.use(world, user, hand)
        }
        if (user.brithControlling > 0){
            val randomTime = user.random.nextInt(20*60*20)
            val plus = if (user.random.nextBoolean()){
                randomTime
            }else{
                -randomTime
            }
            user.menstruation += plus
            user.addEffect(StatusEffects.NAUSEA,20*20,0)
        }
        user.brithControlling = 20*60*20
        if (user.random.nextInt(3) == 0){
            user.isPCOS = false // 有几率治好PCOS
        }
        // 减少一个
        if (!user.isCreative) {
            user.getStackInHand(hand).decrement(1)
        }
        return TypedActionResult.success(user.getStackInHand(hand))
    }
}