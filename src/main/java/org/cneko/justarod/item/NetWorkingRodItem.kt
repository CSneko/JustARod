package org.cneko.justarod.item

import net.minecraft.component.DataComponentTypes
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsageContext
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.world.World
import org.cneko.justarod.api.NetWorkingRodData

// 都看到这里了，还不去给我点个三连啊，不理你了哼！
class NetWorkingRodItem: SelfUsedItem(Settings().maxCount(1).maxDamage(NetWorkingRodData.MAX_DAMAGE).component(JRComponents.SPEED, NetWorkingRodData.SPEED).component(JRComponents.USED_TIME_MARK, 0)) {
    override fun getRodSpeed(stack: ItemStack?): Int {
        return NetWorkingRodData.SPEED
    }

    override fun useOnBlock(context: ItemUsageContext?): ActionResult {
        updateData(context?.player,context?.stack)
        return super.useOnBlock(context)
    }

    override fun onCraft(stack: ItemStack?, world: World?) {
        super.onCraft(stack, world)
        stack?.set(DataComponentTypes.MAX_DAMAGE, NetWorkingRodData.MAX_DAMAGE)
    }

    fun updateData(player: PlayerEntity?,stack: ItemStack?){
        NetWorkingRodData.update()
        stack?.set(DataComponentTypes.MAX_DAMAGE, NetWorkingRodData.MAX_DAMAGE)
        player?.sendMessage(Text.translatable("item.justarod.networking_rod.update"),true)
    }

    override fun getDefaultStack(): ItemStack {
        val stack = super.getDefaultStack()
        stack.set(DataComponentTypes.MAX_DAMAGE, NetWorkingRodData.MAX_DAMAGE)
        return stack
    }
}