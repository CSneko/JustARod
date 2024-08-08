package org.cneko.justarod.item

import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.FoodComponents
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.world.World
import org.cneko.justarod.effect.JREffects


class LubricatingBookItem : Item(
    Settings().maxCount(1).food(FoodComponents.APPLE)
){
    override fun finishUsing(stack: ItemStack, world: World?, user: LivingEntity): ItemStack {
        val foodComponent = stack.get(DataComponentTypes.FOOD)
        if (foodComponent != null) {
            // 为玩家添加状态效果
            JREffects.LUBRICATING_EFFECT?.let {
                val status = StatusEffectInstance(Registries.STATUS_EFFECT.getEntry(it),10000000, 0)
                user.addStatusEffect(status)
            }

            return user.eatFood(world, stack, foodComponent)
        } else {
            return stack
        }
    }
}