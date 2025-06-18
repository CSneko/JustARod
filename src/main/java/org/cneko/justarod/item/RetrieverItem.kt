package org.cneko.justarod.item

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import org.cneko.justarod.entity.Insertable

// 取出来，再插回去
class RetrieverItem:Item(Settings()) {
    override fun useOnEntity(stack: ItemStack?, user: PlayerEntity?, entity: LivingEntity?, hand: Hand?): ActionResult {
        if (entity == null || entity !is Insertable){
            return super.useOnEntity(stack, user, entity, hand)
        }
        // 看看有没有rod
        if (!entity.hasRodInside()){
            // 没有你取个啥？
            if (user?.world?.isClient == false) {
                user.sendMessage(Text.translatable("item.justarod.retriever.no_rod"))
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