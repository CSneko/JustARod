package org.cneko.justarod.item.electric

import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.SlotAccess
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.inventory.Slot
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.level.Level
import org.cneko.toneko.common.mod.misc.ToNekoComponents

/*
被别人远控会怎么样呢... 虽然咱没有过...
 */
class RemoteControlItem(properties: Properties): Item(Properties().stacksTo(1)) {
    override fun use(world: Level?, user: Player?, hand: InteractionHand?): InteractionResultHolder<ItemStack?>? {
        if (user?.isShiftKeyDown() == true){
            val stack = user.getItemInHand(hand)
            val id = stack?.get(ToNekoComponents.ITEM_ID_COMPONENT)
            if (id != null) {
                val item = BuiltInRegistries.ITEM.get(id)
                if (item is MultiModeSelfUsedElectricRodItem){
                    item.switchMode(stack)
                }
            }
        }else if (user?.isShiftKeyDown() == false){
            val stack = user.getItemInHand(hand)
            val id = stack?.get(ToNekoComponents.ITEM_ID_COMPONENT)
            if (id != null) {
                val item = BuiltInRegistries.ITEM.get(id)
                if (item is MultiModeSelfUsedElectricRodItem) {
                    // 寻找附近10格的玩家
                    val players = world?.players()?.filter {
                        val distance = user.distanceTo(it)
                        distance.let {
                            distance < 10
                        }
                    }
                    // 如果玩家副手没有装备该末地烛
                    players?.forEach { players ->
                        if (players.getItemInHand(InteractionHand.OFF_HAND).item == item) {
                            // 找到玩家，设置末地烛模式
                            item.switchMode(players.getItemInHand(InteractionHand.OFF_HAND))
                        }
                    }
                }
            }
        }
        return super.use(world, user, hand)
    }

    override fun appendHoverText(stack: ItemStack?, context: TooltipContext?, tooltip: MutableList<Component?>?, type: TooltipFlag?) {
        val item = stack?.get(ToNekoComponents.ITEM_ID_COMPONENT)?.let {
            BuiltInRegistries.ITEM.get(it)
        }
        if (item is MultiModeSelfUsedElectricRodItem){
            val mode = item.getTranslatableMode(item.getMode(stack)).string
            tooltip?.add(Component.translatable("item.justarod.multi_mode_rods.current_mode", mode))
            tooltip?.add(Component.translatable("item.justarod.multi_mode_rods.switch_mode"))
            tooltip?.add(Component.translatable("item.justarod.multi_mode_rods.all_modes"))
            item.getModes(stack).forEach {
                tooltip?.add(item.getTranslatableMode(it))
            }

        }
        super.appendHoverText(stack, context, tooltip, type)
    }
}