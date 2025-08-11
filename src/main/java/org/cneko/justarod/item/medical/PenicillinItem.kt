package org.cneko.justarod.item.medical

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.Hand
import org.cneko.justarod.entity.Pregnant

class PenicillinItem(settings: Item.Settings): MedicalItem(settings) {
    override fun canApply(
        user: PlayerEntity,
        target: LivingEntity,
        stack: ItemStack,
        hand: Hand
    ): Boolean {
        return true
    }

    override fun getFailureMessage(
        user: PlayerEntity,
        target: LivingEntity,
        stack: ItemStack
    ): Text? {
        return null
    }

    override fun applyEffect(
        user: PlayerEntity,
        target: LivingEntity,
        stack: ItemStack,
        hand: Hand
    ) {
        target as Pregnant
        if (target.syphilis < 20*60*20*2){
            target.syphilis = 0
        }
    }

    override fun consumeItem(
        user: PlayerEntity,
        target: LivingEntity,
        stack: ItemStack,
        hand: Hand
    ) {
        stack.decrement(1)
    }

    override fun getSuccessMessages(
        user: PlayerEntity,
        target: LivingEntity,
        stack: ItemStack
    ): ActionMessages? {
        return null
    }
}