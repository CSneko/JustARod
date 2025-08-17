package org.cneko.justarod.item

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import org.cneko.justarod.entity.BDSMable

class EyePatchItem(settings: Settings): Item(settings) {
    override fun useOnEntity(
        stack: ItemStack?,
        user: PlayerEntity?,
        entity: LivingEntity?,
        hand: Hand?
    ): ActionResult? {
        if (entity is BDSMable && !entity.world.isClient) {
            if (entity.eyePatch>0){
                user?.sendMessage(Text.of("§c对方已经有眼罩了哦~"))
                return ActionResult.FAIL
            }else{
                entity.eyePatch = 20*60*5
                user?.sendMessage(Text.of("§a成功插入眼罩~"))
                if (user?.isCreative == false){
                    stack?.decrement(1)
                }
                return ActionResult.SUCCESS
            }
        }
        return super.useOnEntity(stack, user, entity, hand)
    }
}