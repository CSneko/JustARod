package org.cneko.justarod.item

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import org.cneko.justarod.entity.Insertable

// 取出来，再插回去
class RetrieverItem:Item(Settings()) {
    override fun useOnEntity(stack: ItemStack?, user: Player?, entity: LivingEntity?, hand: InteractionHand?): InteractionResult {
        if (entity == null){
            return super.useOnEntity(stack, user, entity, hand)
        }
        // 看看有没有rod
        if (!entity.hasRodInside()){
            // 没有你取个啥？
            if (user?.world?.isClientSide == false) {
                user.sendSystemMessage(Component.translatable("item.justarod.retriever.no_rod"))
            }
            return super.useOnEntity(stack, user, entity, hand)
        }
        // 取出来！
        val rod = entity.rodInside
        entity.rodInside = ItemStack.EMPTY
        user?.dropItem(rod, true)
        return super.useOnEntity(stack, user, entity, hand)
    }
}