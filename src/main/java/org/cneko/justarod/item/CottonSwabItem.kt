package org.cneko.justarod.item

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.text.Text
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.world.World
import org.cneko.justarod.effect.JREffects

class CottonSwabItem(settings: Settings): Item(settings.maxCount(1)) {
    override fun use(world: World?, user: PlayerEntity?, hand: Hand?): TypedActionResult<ItemStack?>? {
        val stack = user?.getStackInHand(hand)
        if (stack != null && !user.world.isClient) {
            if (stack.contains(JRComponents.SECRETIONS_APPEARANCE)){
                user.sendMessage(Text.of("§c这根棉签已经被使用过了哦"))
            }else{
                if (user.hasEffect(JREffects.VAGINITIS_EFFECT)){
                    stack.set(JRComponents.SECRETIONS_APPEARANCE, "§7灰白色")
                }else if (user.isHydatidiformMole){
                    stack.set(JRComponents.SECRETIONS_APPEARANCE, "§4暗红色")
                }else{
                    stack.set(JRComponents.SECRETIONS_APPEARANCE, "§f透明")
                }
            }
        }
        return super.use(world, user, hand)
    }

    override fun appendTooltip(stack: ItemStack?, context: TooltipContext?, tooltip: MutableList<Text>?, type: TooltipType?) {
        if (stack != null) {
            if (stack.contains(JRComponents.SECRETIONS_APPEARANCE)){
                tooltip?.add(Text.of("沾染的颜色：" + stack.get(JRComponents.SECRETIONS_APPEARANCE)))
            }
        }
    }

}