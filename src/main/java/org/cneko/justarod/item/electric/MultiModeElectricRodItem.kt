package org.cneko.justarod.item.electric

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.text.Text
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.world.World
import org.cneko.justarod.item.JRComponents

/*
很智能的呢，多种模式任你选择~
 */
abstract class MultiModeSelfUsedElectricRodItem(settings:Settings): SelfUsedElectricRodItem(settings) {
    fun getMode(stack: ItemStack): String {
        return stack.getOrDefault(JRComponents.MODE, this.getDefaultMode(stack))
    }

    open fun getDefaultMode(stack: ItemStack): String {
        return getModes(stack)[0]
    }

    open fun getModes(stack: ItemStack): List<String> {
        return listOf("none")
    }

    fun getTranslatableMode(mode: String): Text {
        return Text.translatable("item.justarod.multi_mode_rods.mode.$mode")
    }

    fun switchMode(stack: ItemStack){
        val modes = getModes(stack)
        val currentMode = getMode(stack)
        if (modes.contains(currentMode)) {
            val nextMode = modes[(modes.indexOf(currentMode) + 1) % modes.size]
            stack.set(JRComponents.MODE, nextMode)
        }else{
            stack.set(JRComponents.MODE, getDefaultMode(stack))
        }
    }

    override fun use(world: World?, user: PlayerEntity?, hand: Hand?): TypedActionResult<ItemStack> {
        user?.let {
            if (user.isSneaking){
                val stack = user.getStackInHand(hand)
                switchMode(stack)
                return TypedActionResult.success(stack)
            }
        }
        return super.use(world, user, hand)
    }

    override fun appendTooltip(
        stack: ItemStack?,
        context: TooltipContext?,
        tooltip: MutableList<Text>?,
        type: TooltipType?
    ) {
        super.appendTooltip(stack, context, tooltip, type)
        stack?.let {
            val mode = getTranslatableMode(getMode(stack)).string
            tooltip?.add(Text.translatable("item.justarod.multi_mode_rods.current_mode", mode))
            tooltip?.add(Text.translatable("item.justarod.multi_mode_rods.switch_mode"))
            tooltip?.add(Text.translatable("item.justarod.multi_mode_rods.all_modes"))
            getModes(stack).forEach {
                tooltip?.add(getTranslatableMode(it))
            }
        }
    }
}