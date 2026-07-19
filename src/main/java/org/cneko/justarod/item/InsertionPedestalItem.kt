package org.cneko.justarod.item

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.inventory.SlotAccess
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.item.TooltipFlag
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.inventory.Slot
import net.minecraft.server.level.ServerPlayer
import net.minecraft.network.chat.Component
// FIXME: wildcard util.* needs manual import splitting
import net.minecraft.core.BlockPos
import org.cneko.justarod.damage.JRDamageTypes
import org.cneko.justarod.entity.Insertable
import org.cneko.justarod.item.rod.SelfUsedItemInterface
import org.cneko.toneko.common.mod.items.BazookaItem.Ammunition

/*
趁着别人不注意，偷偷脱下她的胖次给她塞一个进去
你不会以为我是姛吧
emm.... 也行
 */
class InsertionPedestalItem:Item(Settings()),Ammunition {
    override fun onClicked(
        probablyPedestalStack: ItemStack?,
        otherStack: ItemStack?,
        slot: Slot?,
        clickType: ClickType?,
        player: Player?,
        cursorStackReference: SlotAccess?
    ): Boolean {
        if (player?.world?.isClientSide == false) {
            // 主手不是空的
            if (player.getItemInHand(InteractionHand.MAIN_HAND)?.isEmpty!!) {
                player.sendSystemMessage(Component.translatable("item.justarod.insertion_pedestal.must_be_thing_in_hand"))
                return super.onClicked(probablyPedestalStack, otherStack, slot, clickType, player, cursorStackReference)
            }
            val rodStack = player.getItemInHand(InteractionHand.MAIN_HAND)

            if (rodStack.item !is SelfUsedItemInterface) {
                player.sendSystemMessage(Component.translatable("item.justarod.insertion_pedestal.must_be_rod"))
                return super.onClicked(probablyPedestalStack, otherStack, slot, clickType, player, cursorStackReference)
            }

            probablyPedestalStack?.let { pedestalStack ->
                if (pedestalStack.components.contains(JRComponents.ROD_INSIDE)) {
                    player.spawnAtLocation(pedestalStack.getOrDefault(JRComponents.ROD_INSIDE, ItemStack.EMPTY))
                }
                if (pedestalStack.count == 1){
                    pedestalStack.set(JRComponents.ROD_INSIDE, rodStack)
                }else{
                    //issue #11
                    pedestalStack.count -= 1
                    val newPedestalStack = pedestalStack.copy()
                    newPedestalStack.count = 1
                    newPedestalStack.set(JRComponents.ROD_INSIDE, rodStack)
                    if (!player.addItem(newPedestalStack)){//usually inventory full,we cannot give stack
                        player.spawnAtLocation(newPedestalStack)
                    }
                }
                // 去掉一个rod
                if (rodStack.count > 1) {
                    rodStack.shrink(1)
                } else {
                    player.inventory.removeOne(rodStack)
                }
            }
        }
        return super.onClicked(probablyPedestalStack, otherStack, slot, clickType, player, cursorStackReference)
    }

    override fun useOnEntity(stack: ItemStack?, user: Player?, entity: LivingEntity?, hand: InteractionHand?): InteractionResult {
        if (entity is Insertable && user?.world?.isClientSide == false){
            // 已经在里面了哇，塞不进去啦
            if(entity.hasRodInside()){
                user.sendSystemMessage(Component.translatable("item.justarod.insertion_pedestal.already_has_rod"))
                return InteractionResult.FAIL
            }
            val rod: ItemStack? = stack?.getOrDefault(JRComponents.ROD_INSIDE, ItemStack.EMPTY)
            if (rod?.isEmpty == true){
                // 什么也没有呢...
                user.sendSystemMessage(Component.translatable("item.justarod.insertion_pedestal.no_rod"))
                return InteractionResult.FAIL
            }
            // 插入
            stack?.let { entity.insertRod(user, it) }
            //stack?.let { user.setStackInHand(hand,entity.insertRod(user,it).value) }

        }
        return super.useOnEntity(stack, user, entity, hand)
    }
    //我只能做到右键方块触发,因为我不知道有没有api能在右键不到方块的时候触发
    override fun useOnBlock(context: UseOnContext?): InteractionResult {
        if (context != null){
            if (
                context.stack != null
                && context.player != null
                ) {
                val player = context.player!!
                val usingStack = context.stack!!
                if (player.isShiftKeyDown()) {
                    if (usingStack.components.contains(JRComponents.ROD_INSIDE)) {
                        val givingStack = usingStack.getOrDefault(JRComponents.ROD_INSIDE, ItemStack.EMPTY)
                        if (!givingStack.isEmpty){
                            if (!player.addItem(givingStack)) {
                                player.spawnAtLocation(givingStack)
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
        tooltip: MutableList<Component>?,
        type: TooltipFlag?
    ) {
        super.appendHoverText(stack, context, tooltip, type)
        val rod = stack?.getOrDefault(JRComponents.ROD_INSIDE, ItemStack.EMPTY)
        if (rod?.isEmpty == true){
            tooltip?.add(Component.translatable("item.justarod.insertion_pedestal.no_rod"))
        } else {
            tooltip?.add(Component.translatable("item.justarod.insertion_pedestal.has_rod",Component.translatable(rod?.item?.translationKey)))
            rod?.item?.appendHoverText(rod,context,tooltip,type)
        }
    }

    override fun hitOnEntity(shooter: LivingEntity?, target: LivingEntity?, bazooka: ItemStack?, ammo: ItemStack?) {
        if (target is Insertable && shooter is Player && !target.level().isClientSide){
            useOnEntity(ammo,shooter,target,InteractionHand.MAIN_HAND)
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

fun ServerPlayer.hasRodInside(): Boolean{
    return (this as Insertable).hasRodInside()
}
fun <T> T.insertRod(player: Player,pedestal: ItemStack): InteractionResultHolder<ItemStack>
        where T : LivingEntity, T : Insertable {
    this.rodInside = pedestal.getOrDefault(JRComponents.ROD_INSIDE, ItemStack.EMPTY)
    if (!player.isCreative) {
        pedestal.remove(JRComponents.ROD_INSIDE)
    }
    // 受伤
    this.hurt(JRDamageTypes.sexualExcitement(this), 4f)
    this.sendSystemMessage(Component.translatable("item.justarod.insertion_pedestal.insert_rod"))
    return InteractionResultHolder.success(pedestal)
}


private fun Item.getId(): String {
    return BuiltInRegistries.ITEM.getId(this).path
}
