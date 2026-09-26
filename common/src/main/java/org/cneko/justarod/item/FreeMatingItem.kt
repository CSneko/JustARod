package org.cneko.justarod.item

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import org.cneko.justarod.entity.Pregnant
import org.cneko.justarod.item.JRItems.Companion.BYT
import java.util.function.Predicate

class FreeMatingItem(properties: Properties): Item(properties) {
    // 26.x：覆写签名参数不可空
    override fun interactLivingEntity(
        stack: ItemStack,
        user: Player,
        entity: LivingEntity,
        hand: InteractionHand
    ): InteractionResult {
        // PlayerMixin 让 Player 实现 Pregnant（编译期需显式转换）
        val pregnantUser = user as Pregnant
        if (!user.level().isClientSide) {
            if (!pregnantUser.canPregnant()) {
                user.sendSystemMessage(Component.literal("§c你目前还不能怀孕哦"))
            } else {
                // 26.x：Inventory#offhand 字段移除，改用 getOffhandItem()（原逻辑：副手没有 BYT 才会怀孕）
                if (!user.offhandItem.`is`(BYT)) {
                    pregnantUser.tryPregnant()
                    pregnantUser.setBabyCount(pregnantUser.calculateBabyCount(entity))
                    pregnantUser.setChildrenType(entity.type)
                    user.sendSystemMessage(Component.literal("§a交配完成！"))
                    user.sendSystemMessage(Component.literal("§b你怀上了${Component.translatable(entity.type.descriptionId).string}的宝宝哦~"))
                    // 获取对方的负面buff
                    val effects = entity.activeEffects?.filter { !it.effect.value().isBeneficial }
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
        return super.interactLivingEntity(stack, user, entity, hand)
    }
}
