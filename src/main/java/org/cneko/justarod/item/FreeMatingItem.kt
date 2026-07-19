package org.cneko.justarod.item

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import org.cneko.justarod.item.JRItems.Companion.BYT
import java.util.function.Predicate

class FreeMatingItem(properties: Properties): Item(properties) {
    override fun useOnEntity(
        stack: ItemStack?,
        user: Player?,
        entity: LivingEntity?,
        hand: InteractionHand?
    ): InteractionResult? {
        if (user?.world?.isClientSide == false) {
            if (!user.canPregnant()) {
                user.sendSystemMessage(Component.literal("§c你目前还不能怀孕哦"))
            } else {
                if (!user.getInventory().offhand.stream()
                        .anyMatch(Predicate { item: ItemStack? -> item!!.is(BYT) })
                ) {
                    user.tryPregnant()
                    user.babyCount = user.calculateBabyCount(entity)
                    user.childrenType = entity?.type
                    user.sendSystemMessage(Component.literal("§a交配完成！"))
                    user.sendSystemMessage(Component.literal("§b你怀上了${Component.translatable(entity?.type?.translationKey).string}的宝宝哦~"))
                    // 获取对方的负面buff
                    val effects = entity?.activeEffects?.filter { !it.effect.value().isBeneficial }
                    if (effects?.isEmpty() == false) {
                        for (effect in effects) {
                            user.addEffect(effect)
                        }
                        user.sendSystemMessage(Component.literal("§c你被对方传染了！"))
                    }
                }else{
                    user.sendSystemMessage(Component.literal("§a交配完成！"))
                    user.sendSystemMessage(Component.literal("§b你并没有怀孕哦~"))
                    user.sendSystemMessage(Component.literal("§c你没有感染对方的疾病"))
                }

            }
        }
        return super.useOnEntity(stack, user, entity, hand)
    }
}