package org.cneko.justarod.item

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.Rarity
import org.cneko.justarod.entity.Pregnant

class YuriMatingMatingMatingItem: Item(Settings().maxCount(1).rarity(Rarity.RARE)) {
    override fun useOnEntity(
        stack: ItemStack?,
        user: PlayerEntity?,
        entity: LivingEntity?,
        hand: Hand?
    ): ActionResult? {
        val world = user?.world ?: return ActionResult.PASS
        if (world.isClient) return ActionResult.PASS
        if (entity !is Pregnant) return ActionResult.PASS
        if (!entity.isYuri) {
            user.sendMessage(Text.literal("毕竟人家不是百合哦~ 还是不要强迫人家啦...").formatted(Formatting.LIGHT_PURPLE), true)
            return ActionResult.FAIL
        }
        if(entity.tryYuriPregnant()){
            user.sendMessage(Text.literal("成功了哦~ 祝福你们的百合结晶健康成长吧！").formatted(Formatting.LIGHT_PURPLE), true)
        } else {
            user.sendMessage(Text.literal("啊... 似乎失败了呢...").formatted(Formatting.LIGHT_PURPLE), true)
        }
        return super.useOnEntity(stack, user, entity, hand)
    }

    override fun appendTooltip(stack: ItemStack?, context: TooltipContext?, tooltip: MutableList<Text?>?, type: TooltipType?) {
        super.appendTooltip(stack, context, tooltip, type)
        tooltip?.add(Text.of("§d对着她来吧"))
        tooltip?.add(Text.of("§d一起产生百合之间爱の结晶"))
    }
}