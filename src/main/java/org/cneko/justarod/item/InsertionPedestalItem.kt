package org.cneko.justarod.item

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.world.World

class InsertionPedestalItem:Item(Settings()) {
    override fun use(world: World?, user: PlayerEntity?, hand: Hand?): TypedActionResult<ItemStack> {
        // 必须是潜行，并且是副手，并且主手不是空的
        if (!user?.isSneaking!! || hand == Hand.MAIN_HAND || !user.getStackInHand(Hand.MAIN_HAND).isEmpty) {
            return super.use(world, user, hand)
        }
        val stack = user.getStackInHand(Hand.OFF_HAND)
        val rodStack = user.getStackInHand(Hand.MAIN_HAND)

        if (rodStack.item !is EndRodItem){
            user.sendMessage(Text.translatable("item.justarod.insertion_pedestal.must_be_rod"))
            return super.use(world, user, hand)
        }
        // 获取副手末地烛的id
        val id = rodStack.item.getId()
        stack.set(JRComponents.ROD_ID, id)
        return TypedActionResult.success(rodStack)
    }
}

private fun Item.getId(): String {
    return Registries.ITEM.getId(this).path
}
