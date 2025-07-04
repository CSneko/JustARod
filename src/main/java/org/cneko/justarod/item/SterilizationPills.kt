package org.cneko.justarod.item

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.text.Text
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.world.World

class SterilizationPills(settings: Settings): Item(settings) {
    override fun appendTooltip(stack: ItemStack?, context: TooltipContext?, tooltip: List<Text?>?, type: TooltipType?) {
        tooltip?.plus(Text.of("§c请谨慎使用！！！"))
        super.appendTooltip(stack, context, tooltip, type)
    }

    override fun use(world: World?, user: PlayerEntity?, hand: Hand?): TypedActionResult<ItemStack?>? {
        if (user != null && !user.world.isClient){
            if (!user.isSterilization) {
                user.isSterilization = true
                user.sendMessage(Text.of("§c你绝育了！"))
                user.sendMessage(Text.of("§c你将无法再次怀孕！"))
            }
        }
        return super.use(world, user, hand)
    }
}