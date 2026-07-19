package org.cneko.justarod.item.medical

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import org.cneko.justarod.entity.Pregnant

class PenicillinItem(settings: Item.Properties): MedicalItem(properties) {
    override fun canApply(
        user: Player,
        target: LivingEntity,
        stack: ItemStack,
        hand: InteractionHand
    ): Boolean {
        return true
    }

    override fun getFailureMessage(
        user: Player,
        target: LivingEntity,
        stack: ItemStack
    ): Component? {
        return null
    }

    override fun applyEffect(
        user: Player,
        target: LivingEntity,
        stack: ItemStack,
        hand: InteractionHand
    ) {
        target as Pregnant
        if (target.syphilis < 20*60*20*2){
            target.syphilis = 0
        }
    }

    override fun consumeItem(
        user: Player,
        target: LivingEntity,
        stack: ItemStack,
        hand: InteractionHand
    ) {
        stack.shrink(1)
    }

    override fun getSuccessMessages(
        user: Player,
        target: LivingEntity,
        stack: ItemStack
    ): ActionMessages? {
        return null
    }
}