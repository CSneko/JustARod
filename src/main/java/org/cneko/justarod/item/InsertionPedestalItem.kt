package org.cneko.justarod.item

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.StackReference
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsageContext
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.registry.Registries
import net.minecraft.screen.slot.Slot
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.*
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import org.cneko.justarod.damage.JRDamageTypes
import org.cneko.justarod.entity.Insertable
import org.cneko.toneko.common.mod.items.BazookaItem.Ammunition

/*
趁着别人不注意，偷偷脱下她的胖次给她塞一个进去
 */
class InsertionPedestalItem:Item(Settings()),Ammunition {
    override fun onClicked(
        probablyPedestalStack: ItemStack?,
        otherStack: ItemStack?,
        slot: Slot?,
        clickType: ClickType?,
        player: PlayerEntity?,
        cursorStackReference: StackReference?
    ): Boolean {
        if (player?.world?.isClient == false) {
            // 主手不是空的
            if (player.getStackInHand(Hand.MAIN_HAND)?.isEmpty!!) {
                player.sendMessage(Text.translatable("item.justarod.insertion_pedestal.must_be_thing_in_hand"))
                return super.onClicked(probablyPedestalStack, otherStack, slot, clickType, player, cursorStackReference)
            }
            val rodStack = player.getStackInHand(Hand.MAIN_HAND)

            if (rodStack.item !is SelfUsedItemInterface) {
                player.sendMessage(Text.translatable("item.justarod.insertion_pedestal.must_be_rod"))
                return super.onClicked(probablyPedestalStack, otherStack, slot, clickType, player, cursorStackReference)
            }

            probablyPedestalStack?.let { pedestalStack ->
                if (pedestalStack.components.contains(JRComponents.ROD_INSIDE)) {
                    player.dropStack(pedestalStack.getOrDefault(JRComponents.ROD_INSIDE, ItemStack.EMPTY))
                }
                if (pedestalStack.count == 1){
                    pedestalStack.set(JRComponents.ROD_INSIDE, rodStack)
                }else{
                    //issue #11
                    pedestalStack.count -= 1
                    val newPedestalStack = pedestalStack.copy()
                    newPedestalStack.count = 1
                    newPedestalStack.set(JRComponents.ROD_INSIDE, rodStack)
                    if (!player.giveItemStack(newPedestalStack)){//usually inventory full,we cannot give stack
                        player.dropStack(newPedestalStack)
                    }
                }
                // 去掉一个rod
                if (rodStack.count > 1) {
                    rodStack.decrement(1)
                } else {
                    player.inventory.removeOne(rodStack)
                }
            }
        }
        return super.onClicked(probablyPedestalStack, otherStack, slot, clickType, player, cursorStackReference)
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
            stack?.let { entity.insertRod(user, it) }
            //stack?.let { user.setStackInHand(hand,entity.insertRod(user,it).value) }

        }
        return super.useOnEntity(stack, user, entity, hand)
    }
    //我只能做到右键方块触发,因为我不知道有没有api能在右键不到方块的时候触发
    override fun useOnBlock(context: ItemUsageContext?): ActionResult {
        if (context != null){
            if (
                context.stack != null
                && context.player != null
                ) {
                val player = context.player!!
                val usingStack = context.stack!!
                if (player.isSneaking) {
                    if (usingStack.components.contains(JRComponents.ROD_INSIDE)) {
                        val givingStack = usingStack.getOrDefault(JRComponents.ROD_INSIDE, ItemStack.EMPTY)
                        if (!givingStack.isEmpty){
                            if (!player.giveItemStack(givingStack)) {
                                player.dropStack(givingStack)
                            }//我都想弄个static方法出来了,就叫giveOrDropStack
                            usingStack.remove(JRComponents.ROD_INSIDE)
                        }
                    }
                }
            }
        }
        return super.useOnBlock(context)
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

    override fun hitOnEntity(shooter: LivingEntity?, target: LivingEntity?, bazooka: ItemStack?, ammo: ItemStack?) {
        if (target is Insertable && shooter is PlayerEntity && !target.world.isClient){
            useOnEntity(ammo,shooter,target,Hand.MAIN_HAND)
        }
    }

    override fun hitOnBlock(p0: LivingEntity?, p1: BlockPos?, p2: ItemStack?, p3: ItemStack?) {
    }

    override fun hitOnAir(p0: LivingEntity?, p1: BlockPos?, p2: ItemStack?, p3: ItemStack?) {
    }

    override fun getSpeed(p0: ItemStack?, p1: ItemStack?): Float {
        return 1f
    }

    override fun getMaxDistance(p0: ItemStack?, p1: ItemStack?): Float {
        return 30f
    }

    override fun getCooldownTicks(p0: ItemStack?, p1: ItemStack?): Int {
        return 20
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
