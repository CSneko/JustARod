package org.cneko.justarod.item

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.ChatFormatting
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.Rarity
import org.cneko.justarod.entity.Pregnant

class YuriMatingMatingMatingItem: Item(Settings().maxCount(1).rarity(Rarity.RARE)) {
    override fun useOnEntity(
        stack: ItemStack?,
        user: Player?,
        entity: LivingEntity?,
        hand: InteractionHand?
    ): InteractionResult? {
        val world = user?.world ?: return InteractionResult.PASS
        if (world.isClientSide) return InteractionResult.PASS
        if (entity !is Pregnant) return InteractionResult.PASS
        if (!entity.isYuri) {
            user.sendSystemMessage(Component.literal("毕竟人家不是百合哦~ 还是不要强迫人家啦...").withStyle(ChatFormatting.LIGHT_PURPLE), true)
            return InteractionResult.FAIL
        }
        if(entity.tryYuriPregnant()){
            user.sendSystemMessage(Component.literal("成功了哦~ 祝福你们的百合结晶健康成长吧！").withStyle(ChatFormatting.LIGHT_PURPLE), true)
        } else {
            user.sendSystemMessage(Component.literal("啊... 似乎失败了呢...").withStyle(ChatFormatting.LIGHT_PURPLE), true)
        }
        return super.useOnEntity(stack, user, entity, hand)
    }

    override fun appendTooltip(stack: ItemStack?, context: TooltipContext?, tooltip: MutableList<Component?>?, type: TooltipFlag?) {
        super.appendHoverText(stack, context, tooltip, type)
        tooltip?.add(Component.literal("§d对着她来吧"))
        tooltip?.add(Component.literal("§d一起产生百合之间爱の结晶"))
    }
}