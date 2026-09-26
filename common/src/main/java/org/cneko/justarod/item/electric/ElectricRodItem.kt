package org.cneko.justarod.item.electric

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import net.minecraft.world.InteractionHand
import net.minecraft.world.level.Level
import org.cneko.justarod.item.rod.EndRodInstructions
import org.cneko.justarod.item.rod.EndRodItem
import org.cneko.justarod.item.rod.SelfUsedItemInterface

/*
电动的就不需要自己动手啦... 不那么费力的说
但是振动的话就是越快越爽呢
 */
abstract class ElectricRodItem(properties: Properties) : EndRodItem(properties) {
    // 26.x：TeamReborn Energy 接口已移除，这些改为普通方法（仅内部能量语义）
    open fun getEnergyCapacity(stack: ItemStack): Long {
        return stack.maxDamage.toLong()
    }

    open fun getEnergyMaxInput(stack: ItemStack): Long {
        return 1000
    }

    open fun getEnergyMaxOutput(stack: ItemStack): Long {
        return 1000
    }

    // 26.x 的 TeamReborn Energy 尚未适配；这里直接用耐久值充当内部能量存储。
    fun getStoredEnergy(stack: ItemStack?): Long {
        return (stack?.maxDamage?.toLong() ?: 0L) - (stack?.damageValue ?: 0)
    }

    fun setStoredEnergy(stack: ItemStack?, energy: Long) {
        val clamped = energy.coerceIn(0, stack?.maxDamage?.toLong() ?: 0)
        stack?.damageValue = (stack.maxDamage - clamped.toInt()).coerceAtLeast(0)
    }

        override fun appendHoverText(
        stack: ItemStack,
        context: net.minecraft.world.item.Item.TooltipContext,
        display: net.minecraft.world.item.component.TooltipDisplay,
        adder: java.util.function.Consumer<Component>,
        type: TooltipFlag
    ) {
        super.appendHoverText(stack, context, display, adder, type)
        val energy = getStoredEnergy(stack)
        val showEnergy: String = if (energy in 1000..1000000){
            "${energy / 1000}k"
        }else if (energy >= 1000000){
            "${energy / 1000000}M"
        }else{
            "$energy"
        }
        val maxEnergy = getEnergyCapacity(stack)
        val maxShowEnergy: String = if (maxEnergy in 1000..1000000){
            "${maxEnergy / 1000}k"
        }else if (maxEnergy >= 1000000){
            "${maxEnergy / 1000000}M"
        }else{
            "$maxEnergy"
        }
        adder.accept(Component.translatable("item.justarod.electric_rod.tooltip", showEnergy, maxShowEnergy).withStyle(ChatFormatting.GOLD))
    }

    override fun onCraftedPostProcess(stack: ItemStack, world: Level) {
        super.onCraftedPostProcess(stack, world)
        stack.damageValue = stack.maxDamage
    }

    override fun damage(stack: ItemStack, amount: Int, world: Level?) {
        super.damage(stack, amount, world)
        this.setStoredEnergy(stack, (stack.maxDamage - stack.damageValue).toLong())
    }

    override fun canDamage(stack: ItemStack, amount: Int): Boolean {
        return this.getStoredEnergy(stack)>this.getEnergyCapacity(stack)*0.01 && this.getStoredEnergy(stack) >= amount
    }

    override fun inventoryTick(
        stack: ItemStack,
        world: net.minecraft.server.level.ServerLevel,
        entity: net.minecraft.world.entity.Entity,
        slot: EquipmentSlot?
) {
        // 设置耐久与能量同步
        stack.damageValue = stack.maxDamage - this.getStoredEnergy(stack).toInt()
        super.inventoryTick(stack, world, entity, slot)
    }
}

abstract class SelfUsedElectricRodItem(properties: Properties) : ElectricRodItem(properties), SelfUsedItemInterface {

        override fun appendHoverText(
        stack: ItemStack,
        context: net.minecraft.world.item.Item.TooltipContext,
        display: net.minecraft.world.item.component.TooltipDisplay,
        adder: java.util.function.Consumer<Component>,
        type: TooltipFlag
    ) {
        super.appendHoverText(stack, context, display, adder, type)
        val speed = this.getRodSpeed(stack)
        adder.accept(Component.translatable("item.justarod.end_rod.speed", speed).withStyle(ChatFormatting.LIGHT_PURPLE))
    }
    override fun inventoryTick(
        stack: ItemStack,
        world: net.minecraft.server.level.ServerLevel,
        entity: net.minecraft.world.entity.Entity,
        slot: EquipmentSlot?
) {
        super.inventoryTick(stack, world, entity, slot)

        // 如果耐久为0或者实体不是LivingEntity，则不处理
        if(stack.damageValue == stack.maxDamage || entity !is LivingEntity) return

        // 只在服务端执行使用逻辑（效果/伤害/移动由服务端决定并同步给客户端）
        if (world.isClientSide) return

        val e: LivingEntity = entity

        // 如果放在副手
        if (
            e.getItemInHand(InteractionHand.OFF_HAND) == stack //是的,直接用==
        ){
            // 减少一点耐久 (即使没耐久也不损坏)
            stack.damageValue++
            // 执行
            useOnSelf(stack, world, e, slot?.ordinal ?: -1, true)
        }
    }
    override fun getInstruction(): EndRodInstructions {
        return EndRodInstructions.USE_ON_SELF
    }
}