package org.cneko.justarod.item

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import org.cneko.justarod.entity.BDSMable

class HandcuffesItem(settings: Settings): Item(settings) {
    override fun useOnEntity(
        stack: ItemStack?,
        user: PlayerEntity?,
        entity: LivingEntity?,
        hand: Hand?
    ): ActionResult? {
        if (entity is BDSMable && !entity.world.isClient) {
            if (entity.handcuffed>0){
                user?.sendMessage(Text.of("§c对方已经有手铐了哦~"))
                return ActionResult.FAIL
            }else{
                entity.handcuffed = 20*60*5
                user?.sendMessage(Text.of("§a成功插入手铐~"))
                if (user?.isCreative == false){
                    stack?.decrement(1)
                }
                return ActionResult.SUCCESS
            }
        }
        return super.useOnEntity(stack, user, entity, hand)
    }
}