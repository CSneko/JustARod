package org.cneko.justarod.item
import org.cneko.justarod.JRIds

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import org.cneko.justarod.entity.Insertable

// 取出来，再插回去
class RetrieverItem:Item(JRIds.itemProps("retriever")) {
    // 26.x：覆写签名参数不可空
    override fun interactLivingEntity(stack: ItemStack, user: Player, entity: LivingEntity, hand: InteractionHand): InteractionResult {
        val insertable = entity as? Insertable
        if (insertable == null) {
            return super.interactLivingEntity(stack, user, entity, hand)
        }
        // 看看有没有rod
        if (!insertable.hasRodInside()) {
            // 没有你取个啥？
            if (!entity.level().isClientSide) {
                user.sendSystemMessage(Component.translatable("item.justarod.retriever.no_rod"))
            }
            return super.interactLivingEntity(stack, user, entity, hand)
        }
        // 取出来！
        val rod = insertable.rodInside
        insertable.rodInside = ItemStack.EMPTY
        user.drop(rod, true)
        return super.interactLivingEntity(stack, user, entity, hand)
    }
}
