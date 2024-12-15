package org.cneko.justarod.item

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.world.World
import org.cneko.justarod.damage.JRDamageTypes
import org.cneko.justarod.entity.Insertable

class InsertionPedestalItem:Item(Settings()) {
    override fun use(world: World?, user: PlayerEntity?, hand: Hand?): TypedActionResult<ItemStack> {
        // 必须是潜行，并且是副手，并且主手不是空的
        if (!user?.isSneaking!! || hand == Hand.MAIN_HAND || !user.getStackInHand(Hand.MAIN_HAND).isEmpty) {
            return super.use(world, user, hand)
        }
        val stack = user.getStackInHand(Hand.OFF_HAND)
        val rodStack = user.getStackInHand(Hand.MAIN_HAND)

        if (rodStack.item !is SelfUsedItemInterface){
            user.sendMessage(Text.translatable("item.justarod.insertion_pedestal.must_be_rod"))
            return super.use(world, user, hand)
        }
        // 获取副手末地烛的id
        val id = rodStack.item.getId()
        stack.set(JRComponents.ROD_ID, id)

        return TypedActionResult.success(rodStack)
    }

    override fun useOnEntity(stack: ItemStack?, user: PlayerEntity?, entity: LivingEntity?, hand: Hand?): ActionResult {
        if (entity is Insertable){
            // 已经在里面了哇，塞不进去啦
            if(entity.hasRodInside()){
                user?.sendMessage(Text.translatable("item.justarod.insertion_pedestal.already_has_rod"))
                return ActionResult.FAIL
            }
            val id = stack?.getOrDefault(JRComponents.ROD_ID, "") ?: ""
            if (id.isEmpty()){
                // 什么也没有呢...
                user?.sendMessage(Text.translatable("item.justarod.insertion_pedestal.no_rod"))
                return ActionResult.FAIL
            }
            // 插入
            stack?.let { user?.setStackInHand(hand,entity.insertRod(it).value) }

        }
        return super.useOnEntity(stack, user, entity, hand)
    }
}

fun ServerPlayerEntity.hasRodInside(): Boolean{
    return (this as Insertable).hasRodInside()
}
fun <T> T.insertRod(pedestal: ItemStack): TypedActionResult<ItemStack>
where T : LivingEntity, T : Insertable {
    this.rodInside = pedestal.getOrDefault(JRComponents.ROD_ID, "")
    // 受伤
    this.damage(JRDamageTypes.sexualExcitement(this), 4f)
    this.sendMessage(Text.translatable("item.justarod.insertion_pedestal.insert_rod"))
    return TypedActionResult.success(pedestal)
}


private fun Item.getId(): String {
    return Registries.ITEM.getId(this).path
}
