package org.cneko.justarod.item

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand

class FreeMatingItem(settings: Settings): Item(settings) {
    override fun useOnEntity(
        stack: ItemStack?,
        user: PlayerEntity?,
        entity: LivingEntity?,
        hand: Hand?
    ): ActionResult? {
        if (user?.world?.isClient == false) {
            if (!user.canPregnant()) {
                user.sendMessage(Text.of("§c你目前还不能怀孕哦"))
            } else {
                user.pregnant = 20 * 60 * 20 * 5 // 5天
                user.childrenType = entity?.type
                user.sendMessage(Text.of("§a交配完成"))
                user.sendMessage(Text.of("§b你怀上了${Text.translatable(entity?.type?.translationKey).string}的宝宝哦~"))
                // 获取对方的负面buff
                val effects = entity?.statusEffects?.filter { !it.effectType.value().isBeneficial }
                if (effects?.isEmpty() == false) {
                    for (effect in effects) {
                        user.addStatusEffect(effect)
                    }
                    user.sendMessage(Text.of("§c你被对方传染了！"))
                }
            }
        }
        return super.useOnEntity(stack, user, entity, hand)
    }
}