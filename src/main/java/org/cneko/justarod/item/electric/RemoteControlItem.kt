package org.cneko.justarod.item.electric

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.StackReference
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.registry.Registries
import net.minecraft.screen.slot.Slot
import net.minecraft.text.Text
import net.minecraft.util.ClickType
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.world.World
import org.cneko.toneko.common.mod.misc.ToNekoComponents

/*
被别人远控会怎么样呢... 虽然咱没有过...
 */
class RemoteControlItem(settings: Settings): Item(settings.maxCount(1)) {
    override fun onClicked(
        stack: ItemStack?,
        otherStack: ItemStack?,
        slot: Slot?,
        clickType: ClickType?,
        player: PlayerEntity?,
        cursorStackReference: StackReference?
    ): Boolean {
        // 主手不是空的
        if (player == null) return false
        if (player.getStackInHand(Hand.MAIN_HAND)?.isEmpty!!) {
            player.sendMessage(Text.translatable("item.justarod.remote_control.must_be_thing_in_hand"))
            return super.onClicked(stack, otherStack, slot, clickType, player, cursorStackReference)
        }
        val rodStack = player.getStackInHand(Hand.MAIN_HAND)

        if (rodStack.item !is MultiModeSelfUsedElectricRodItem) {
            player.sendMessage(Text.translatable("item.justarod.remote_control.must_be_multi_mode_rod"))
            return super.onClicked(stack, otherStack, slot, clickType, player, cursorStackReference)
        }
        // 设置id
        stack?.set(ToNekoComponents.ITEM_ID_COMPONENT, Registries.ITEM.getId(rodStack.item))
        return super.onClicked(stack, otherStack, slot, clickType, player, cursorStackReference)
    }

    override fun use(world: World?, user: PlayerEntity?, hand: Hand?): TypedActionResult<ItemStack?>? {
        if (user?.isSneaking == true){
            val stack = user.getStackInHand(hand)
            val id = stack?.get(ToNekoComponents.ITEM_ID_COMPONENT)
            if (id != null) {
                val item = Registries.ITEM.get(id)
                if (item is MultiModeSelfUsedElectricRodItem){
                    item.switchMode(stack)
                }
            }
        }else if (user?.isSneaking == false){
            val stack = user.getStackInHand(hand)
            val id = stack?.get(ToNekoComponents.ITEM_ID_COMPONENT)
            if (id != null) {
                val item = Registries.ITEM.get(id)
                if (item is MultiModeSelfUsedElectricRodItem) {
                    // 寻找附近10格的玩家
                    val players = world?.players?.filter {
                        val distance = user.distanceTo(it)
                        distance.let {
                            distance < 10
                        }
                    }
                    // 如果玩家副手没有装备该末地烛
                    players?.forEach { players ->
                        if (players.getStackInHand(Hand.OFF_HAND).item == item) {
                            // 找到玩家，设置末地烛模式
                            item.switchMode(players.getStackInHand(Hand.OFF_HAND))
                        }
                    }
                }
            }
        }
        return super.use(world, user, hand)
    }

    override fun appendTooltip(stack: ItemStack?, context: TooltipContext?, tooltip: MutableList<Text?>?, type: TooltipType?) {
        val item = stack?.get(ToNekoComponents.ITEM_ID_COMPONENT)?.let {
            Registries.ITEM.get(it)
        }
        if (item is MultiModeSelfUsedElectricRodItem){
            val mode = item.getTranslatableMode(item.getMode(stack)).string
            tooltip?.add(Text.translatable("item.justarod.multi_mode_rods.current_mode", mode))
            tooltip?.add(Text.translatable("item.justarod.multi_mode_rods.switch_mode"))
            tooltip?.add(Text.translatable("item.justarod.multi_mode_rods.all_modes"))
            item.getModes(stack).forEach {
                tooltip?.add(item.getTranslatableMode(it))
            }

        }
        super.appendTooltip(stack, context, tooltip, type)
    }
}