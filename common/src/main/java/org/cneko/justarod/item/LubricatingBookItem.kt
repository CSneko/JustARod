package org.cneko.justarod.item

import net.minecraft.core.component.DataComponents
import net.minecraft.world.food.Foods
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.Level
import org.cneko.justarod.effect.JREffects

/*
不上润滑也能玩，但是你最好上一个
不然小心痛的嗯啊嗯啊的叫♡
 */
class LubricatingBookItem : Item(
    Properties().stacksTo(1).food(Foods.APPLE)
){
    override fun finishUsingItem(stack: ItemStack, world: Level?, user: LivingEntity): ItemStack {
        val foodComponent = stack.get(DataComponents.FOOD)
        if (foodComponent != null) {
            // 为玩家添加状态效果
            JREffects.LUBRICATING_EFFECT?.let {
                val status = MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(it), 10000000, 0)
                user.addEffect(status)
            }

            return user.eat(world!!, stack)
        } else {
            return stack
        }
    }
}