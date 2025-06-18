package org.cneko.justarod.item.electric

import net.minecraft.entity.LivingEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.world.World
import org.cneko.justarod.item.EndRodInstructions
import org.cneko.justarod.item.EndRodItem
import org.cneko.justarod.item.SelfUsedItemInterface
import team.reborn.energy.api.base.SimpleEnergyItem

/*
电动的就不需要自己动手啦... 不那么费力的说
但是振动的话就是越快越爽呢
 */
abstract class ElectricRodItem(settings: Settings) : EndRodItem(settings),SimpleEnergyItem {
    override fun getEnergyCapacity(stack: ItemStack?): Long {
        return stack?.maxDamage?.toLong()!!
    }

    override fun getEnergyMaxInput(stack: ItemStack?): Long {
        return 1000
    }

    override fun getEnergyMaxOutput(stack: ItemStack?): Long {
        return 1000
    }

    override fun appendTooltip(
        stack: ItemStack?,
        context: TooltipContext?,
        tooltip: MutableList<Text>?,
        type: TooltipType?
    ) {
        super.appendTooltip(stack, context, tooltip, type)
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
        tooltip?.add(Text.translatable("item.justarod.electric_rod.tooltip", showEnergy, maxShowEnergy).formatted(Formatting.GOLD))
    }

    override fun onCraft(stack: ItemStack?, world: World?) {
        super.onCraft(stack, world)
        stack?.damage = stack?.maxDamage!!
    }

    override fun damage(stack: ItemStack, amount: Int, world: World?) {
        super.damage(stack, amount, world)
        this.setStoredEnergy(stack, (stack.maxDamage - stack.damage).toLong())
    }

    override fun canDamage(stack: ItemStack, amount: Int): Boolean {
        return this.getStoredEnergy(stack)>this.getEnergyCapacity(stack)*0.01 && this.getStoredEnergy(stack) >= amount
    }

    override fun inventoryTick(
        stack: ItemStack?,
        world: World?,
        entity: net.minecraft.entity.Entity?,
        slot: Int,
        selected: Boolean
    ) {
        // 设置耐久与能量同步
        stack?.damage = stack?.maxDamage!! - this.getStoredEnergy(stack).toInt()
        super.inventoryTick(stack, world, entity, slot, selected)
    }
}

abstract class SelfUsedElectricRodItem(settings: Settings) : ElectricRodItem(settings), SelfUsedItemInterface {

    override fun appendTooltip(
        stack: ItemStack?,
        context: TooltipContext?,
        tooltip: MutableList<Text>?,
        type: TooltipType?
    ) {
        super.appendTooltip(stack, context, tooltip, type)
        val speed = this.getRodSpeed(stack)
        tooltip?.add(Text.translatable("item.justarod.end_rod.speed", speed).formatted(Formatting.LIGHT_PURPLE))
    }
    override fun inventoryTick(
        stack: ItemStack?,
        world: World?,
        entity: net.minecraft.entity.Entity?,
        slot: Int,
        selected: Boolean
    ) {
        super.inventoryTick(stack, world, entity, slot, selected)

        // 如果耐久为0或者实体不是LivingEntity，则不处理
        if(stack!!.damage == stack.maxDamage || entity !is LivingEntity) return

        val e: LivingEntity = entity

        // 如果放在副手
        if (
            e.getStackInHand(Hand.OFF_HAND) == stack //是的,直接用==
            || slot == Int.MIN_VALUE // now works with inserted rods
        ){
            // 减少一点耐久 (即使没耐久也不损坏)
            stack.damage++
            // 执行
            useOnSelf(stack, world, e, slot, selected)
        }
    }
    override fun getInstruction(): EndRodInstructions {
        return EndRodInstructions.USE_ON_SELF
    }
}