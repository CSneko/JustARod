package org.cneko.justarod.item

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import org.cneko.justarod.entity.BDSMable

class ElectricShockDeviceItem(settings: Settings): Item(settings) {
    override fun useOnEntity(
        stack: ItemStack?,
        user: PlayerEntity?,
        entity: LivingEntity?,
        hand: Hand?
    ): ActionResult? {
        if (entity is BDSMable && user?.world?.isClient == false){
            if (entity.electricShock>0){
                user.sendMessage(Text.of("§c对方已经有电击器了哦~"))
                return ActionResult.FAIL
            }else{
                entity.electricShock = 20*60*20
                if (!user.isCreative){
                    stack?.decrement(1)
                }
                user.sendMessage(Text.of("§a成功插入电击器~"))
                return ActionResult.SUCCESS
            }
        }
        return super.useOnEntity(stack, user, entity, hand)
    }
}