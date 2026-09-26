package org.cneko.justarod.item.electric

import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.level.Level
import org.cneko.justarod.item.JRComponents

/*
很智能的呢，多种模式任你选择~
 */
abstract class MultiModeSelfUsedElectricRodItem(properties:Properties): SelfUsedElectricRodItem(properties) {
    fun getMode(stack: ItemStack): String {
        return stack.getOrDefault(JRComponents.MODE, this.getDefaultMode(stack))
    }

    open fun getDefaultMode(stack: ItemStack): String {
        return getModes(stack)[0]
    }

    open fun getModes(stack: ItemStack): List<String> {
        return listOf("none")
    }

    fun getTranslatableMode(mode: String): Component {
        return Component.translatable("item.justarod.multi_mode_rods.mode.$mode")
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

    override fun use(world: Level, user: Player, hand: InteractionHand): InteractionResult {
        user?.let {
            if (user.isShiftKeyDown()){
                val stack = user.getItemInHand(hand)
                switchMode(stack)
                return InteractionResult.SUCCESS
            }
        }
        return super.use(world, user, hand)
    }

        override fun appendHoverText(
        stack: ItemStack,
        context: net.minecraft.world.item.Item.TooltipContext,
        display: net.minecraft.world.item.component.TooltipDisplay,
        adder: java.util.function.Consumer<Component>,
        type: TooltipFlag
    ) {
        super.appendHoverText(stack, context, display, adder, type)
        stack?.let {
            val mode = getTranslatableMode(getMode(stack)).string
            adder.accept(Component.translatable("item.justarod.multi_mode_rods.current_mode", mode))
            adder.accept(Component.translatable("item.justarod.multi_mode_rods.switch_mode"))
            adder.accept(Component.translatable("item.justarod.multi_mode_rods.all_modes"))
            getModes(stack).forEach {
                adder.accept(getTranslatableMode(it))
            }
        }
    }
}