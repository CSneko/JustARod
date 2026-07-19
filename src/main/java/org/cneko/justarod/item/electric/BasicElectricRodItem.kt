package org.cneko.justarod.item.electric

import net.minecraft.component.DataComponentTypes
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.level.Level
import org.cneko.justarod.damage.JRDamageTypes
import org.cneko.justarod.item.JRComponents

/*
其实漏电也不错的说... 低压的话还是挺棒的哦
酥酥麻麻耶嘿嘿~
 */
open class BasicElectricRodItem: SelfUsedElectricRodItem(Settings().component(JRComponents.USED_TIME_MARK,0).component(JRComponents.SPEED,10).maxDamage(10000)) {

    override fun appendTooltip(
        stack: ItemStack?,
        context: TooltipContext?,
        tooltip: MutableList<Component>?,
        type: TooltipFlag?
    ) {
        tooltip?.add(Component.translatable("item.justarod.basic_electric_rod.tooltip"))
        super.appendHoverText(stack, context, tooltip, type)
    }

    override fun damage(stack: ItemStack, amount: Int, world: Level?) {
        super<SelfUsedElectricRodItem>.hurt(stack, amount, world)
        // 随机额外减少
        if (stack.damage!=stack.maxDamage){
            val random = world?.random?.nextInt(500)
            if (random != null) {
                if (random+stack.damage>=stack.maxDamage){
                    stack.damage = stack.maxDamage
                }
            }
        }
    }

    override fun useOnSelf(
        stack: ItemStack,
        world: Level?,
        entity: LivingEntity,
        slot: Int,
        selected: Boolean
    ): InteractionResult {
        val result = super.useOnSelf(stack, world, entity, slot, selected)
        if (result == InteractionResult.SUCCESS){
            // 减少实体0.1~1.5血量
            val random = world?.random?.nextInt(5)?.plus(1)
            if (random != null) {
                entity.hurt(JRDamageTypes.sexualExcitement(entity), random.toFloat())
            }
        }
        return result
    }
}