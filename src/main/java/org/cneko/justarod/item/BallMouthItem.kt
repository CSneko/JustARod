package org.cneko.justarod.item

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import org.cneko.justarod.entity.BallMouthable

class BallMouthItem(settings: Item.Settings): Item(settings) {
    override fun useOnEntity(
        stack: ItemStack?,
        user: PlayerEntity?,
        entity: LivingEntity?,
        hand: Hand?
    ): ActionResult? {
        if (entity is BallMouthable){
            if (entity.ballMouth>0){
                user?.sendMessage(Text.of("§c对方已经有禁言口罩了哦~"))
                return ActionResult.FAIL
            }else{
                entity.ballMouth = 20*60*5
                user?.sendMessage(Text.of("§a成功插入禁言口罩~"))
                return ActionResult.SUCCESS
            }
        }
        return super.useOnEntity(stack, user, entity, hand)
    }
}