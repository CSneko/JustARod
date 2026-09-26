package org.cneko.justarod.item.rod
import org.cneko.justarod.JRIds

import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.level.Level
import org.cneko.justarod.api.NetWorkingRodData
import org.cneko.justarod.item.JRComponents

// 都看到这里了，还不去给我点个三连啊，不理你了哼！
class NetWorkingRodItem: SelfUsedItem(JRIds.itemProps("networking_rod").stacksTo(1).durability(NetWorkingRodData.MAX_DAMAGE).component(
    JRComponents.Companion.SPEED, NetWorkingRodData.SPEED).component(JRComponents.Companion.USED_TIME_MARK, 0)) {
    override fun getRodSpeed(stack: ItemStack?): Int {
        return NetWorkingRodData.SPEED
    }

    override fun useOn(context: UseOnContext): InteractionResult {
        updateData(context.player, context.itemInHand)
        return super.useOn(context)
    }

    override fun onCraftedBy(
        stack: ItemStack,
        player: Player
) {
        super.onCraftedBy(stack, player)
        stack?.set(DataComponents.MAX_DAMAGE, NetWorkingRodData.MAX_DAMAGE)
    }

    fun updateData(player: Player?, stack: ItemStack?){
        NetWorkingRodData.update()
        stack?.set(DataComponents.MAX_DAMAGE, NetWorkingRodData.MAX_DAMAGE)
        player?.sendOverlayMessage(Component.translatable("item.justarod.networking_rod.update"))
    }

    override fun getDefaultInstance(): ItemStack {
        val stack = super.getDefaultInstance()
        stack?.set(DataComponents.MAX_DAMAGE, NetWorkingRodData.MAX_DAMAGE)
        return stack
    }
}