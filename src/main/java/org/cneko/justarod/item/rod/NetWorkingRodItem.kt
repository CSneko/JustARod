package org.cneko.justarod.item.rod

import net.minecraft.component.DataComponentTypes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.level.Level
import org.cneko.justarod.api.NetWorkingRodData
import org.cneko.justarod.item.JRComponents

// 都看到这里了，还不去给我点个三连啊，不理你了哼！
class NetWorkingRodItem: SelfUsedItem(Settings().maxCount(1).maxDamage(NetWorkingRodData.MAX_DAMAGE).component(
    JRComponents.Companion.SPEED, NetWorkingRodData.SPEED).component(JRComponents.Companion.USED_TIME_MARK, 0)) {
    override fun getRodSpeed(stack: ItemStack?): Int {
        return NetWorkingRodData.SPEED
    }

    override fun useOnBlock(context: UseOnContext?): InteractionResult {
        updateData(context?.player,context?.stack)
        return super.useOnBlock(context)
    }

    override fun onCraft(stack: ItemStack?, world: Level?) {
        super.onCraft(stack, world)
        stack?.set(DataComponentTypes.MAX_DAMAGE, NetWorkingRodData.MAX_DAMAGE)
    }

    fun updateData(player: Player?,stack: ItemStack?){
        NetWorkingRodData.update()
        stack?.set(DataComponentTypes.MAX_DAMAGE, NetWorkingRodData.MAX_DAMAGE)
        player?.sendSystemMessage(Component.translatable("item.justarod.networking_rod.update"),true)
    }

    override fun getDefaultStack(): ItemStack {
        val stack = super.getDefaultStack()
        stack.set(DataComponentTypes.MAX_DAMAGE, NetWorkingRodData.MAX_DAMAGE)
        return stack
    }
}