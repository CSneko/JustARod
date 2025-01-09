package org.cneko.justarod.item

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.StackReference
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.registry.Registries
import net.minecraft.screen.slot.Slot
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.ClickType
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.world.World
import org.cneko.justarod.damage.JRDamageTypes
import org.cneko.justarod.entity.Insertable

class InsertionPedestalItem:Item(Settings()) {
    override fun use(world: World?, user: PlayerEntity?, hand: Hand?): TypedActionResult<ItemStack> {
        return super.use(world, user, hand)
    }

    override fun onClicked(
        stack: ItemStack?,
        otherStack: ItemStack?,
        slot: Slot?,
        clickType: ClickType?,
        player: PlayerEntity?,
        cursorStackReference: StackReference?
    ): Boolean {
        if (player?.world?.isClient == false) {
            // 必须是潜行，并且是副手，并且主手不是空的
            if (player.getStackInHand(Hand.MAIN_HAND)?.isEmpty!!) {
                player.sendMessage(Text.translatable("item.justarod.insertion_pedestal.must_be_thing_in_hand"))
                return super.onClicked(stack, otherStack, slot, clickType, player, cursorStackReference)
            }
            val rodStack = player.getStackInHand(Hand.MAIN_HAND)

            if (rodStack.item !is SelfUsedItemInterface) {
                player.sendMessage(Text.translatable("item.justarod.insertion_pedestal.must_be_rod"))
                return super.onClicked(stack, otherStack, slot, clickType, player, cursorStackReference)
            }
            stack?.let {
                if (it.components.contains(JRComponents.ROD_INSIDE)) {
                    player.dropStack(it.getOrDefault(JRComponents.ROD_INSIDE, ItemStack.EMPTY))
                }
                it.set(JRComponents.ROD_INSIDE, rodStack)
                // 去掉一个rod
                if (rodStack.count > 1) {
                    rodStack.decrement(1)
                } else {
                    player.inventory.removeOne(rodStack)
                }
            }
        }
        return super.onClicked(stack, otherStack, slot, clickType, player, cursorStackReference)
    }

    override fun useOnEntity(stack: ItemStack?, user: PlayerEntity?, entity: LivingEntity?, hand: Hand?): ActionResult {
        if (entity is Insertable && user?.world?.isClient == false){
            // 已经在里面了哇，塞不进去啦
            if(entity.hasRodInside()){
                user.sendMessage(Text.translatable("item.justarod.insertion_pedestal.already_has_rod"))
                return ActionResult.FAIL
            }
                val rod: ItemStack? = stack?.getOrDefault(JRComponents.ROD_INSIDE, ItemStack.EMPTY)
            if (rod?.isEmpty == true){
                // 什么也没有呢...
                user.sendMessage(Text.translatable("item.justarod.insertion_pedestal.no_rod"))
                return ActionResult.FAIL
            }
            // 插入
            stack?.let { user.setStackInHand(hand,entity.insertRod(user,it).value) }

        }
        return super.useOnEntity(stack, user, entity, hand)
    }

    override fun appendTooltip(
        stack: ItemStack?,
        context: TooltipContext?,
        tooltip: MutableList<Text>?,
        type: TooltipType?
    ) {
        super.appendTooltip(stack, context, tooltip, type)
        val rod = stack?.getOrDefault(JRComponents.ROD_INSIDE, ItemStack.EMPTY)
        if (rod?.isEmpty == true){
            tooltip?.add(Text.translatable("item.justarod.insertion_pedestal.no_rod"))
        } else {
            tooltip?.add(Text.translatable("item.justarod.insertion_pedestal.has_rod",Text.translatable(rod?.item?.translationKey)))
            rod?.item?.appendTooltip(rod,context,tooltip,type)
        }
    }
}

fun ServerPlayerEntity.hasRodInside(): Boolean{
    return (this as Insertable).hasRodInside()
}
fun <T> T.insertRod(player: PlayerEntity,pedestal: ItemStack): TypedActionResult<ItemStack>
where T : LivingEntity, T : Insertable {
    this.rodInside = pedestal.getOrDefault(JRComponents.ROD_INSIDE, ItemStack.EMPTY)
    if (!player.isCreative) {
        pedestal.remove(JRComponents.ROD_INSIDE)
    }
    // 受伤
    this.damage(JRDamageTypes.sexualExcitement(this), 4f)
    this.sendMessage(Text.translatable("item.justarod.insertion_pedestal.insert_rod"))
    return TypedActionResult.success(pedestal)
}


private fun Item.getId(): String {
    return Registries.ITEM.getId(this).path
}
