package org.cneko.justarod.item
import org.cneko.justarod.JRIds

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

class YuriMatingMatingMatingItem: Item(JRIds.itemProps("yuri_mating_mating_mating").stacksTo(1).rarity(Rarity.RARE)) {
    // 26.x：参数改为非空
    override fun interactLivingEntity(
        stack: ItemStack,
        user: Player,
        entity: LivingEntity,
        hand: InteractionHand
    ): InteractionResult {
        val world = user.level()
        if (world.isClientSide) return InteractionResult.PASS
        if (entity !is Pregnant) return InteractionResult.PASS
        if (!entity.isYuri) {
            user.sendSystemMessage(Component.literal("毕竟人家不是百合哦~ 还是不要强迫人家啦...").withStyle(ChatFormatting.LIGHT_PURPLE))
            return InteractionResult.FAIL
        }
        if(entity.tryYuriPregnant()){
            user.sendSystemMessage(Component.literal("成功了哦~ 祝福你们的百合结晶健康成长吧！").withStyle(ChatFormatting.LIGHT_PURPLE))
        } else {
            user.sendSystemMessage(Component.literal("啊... 似乎失败了呢...").withStyle(ChatFormatting.LIGHT_PURPLE))
        }
        return super.interactLivingEntity(stack, user, entity, hand)
    }

        override fun appendHoverText(
        stack: ItemStack,
        context: net.minecraft.world.item.Item.TooltipContext,
        display: net.minecraft.world.item.component.TooltipDisplay,
        adder: java.util.function.Consumer<Component>,
        type: TooltipFlag
    ) {
        super.appendHoverText(stack, context, display, adder, type)
        adder.accept(Component.literal("§d对着她来吧"))
        adder.accept(Component.literal("§d一起产生百合之间爱の结晶"))
    }
}