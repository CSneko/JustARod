package org.cneko.justarod.item
import org.cneko.justarod.JRIds

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
    JRIds.itemProps("lubricating_book").stacksTo(1).food(Foods.APPLE)
){
    override fun finishUsingItem(stack: ItemStack, world: Level, user: LivingEntity): ItemStack {
        val foodComponent = stack.get(DataComponents.CONSUMABLE)
        if (foodComponent != null) {
            // 为玩家添加状态效果（26.x：效果注册改为 Holder，直接使用）
            val status = MobEffectInstance(JREffects.LUBRICATING_EFFECT, 10000000, 0)
            user.addEffect(status)

            // 26.x：LivingEntity#eat 移除，通过 CONSUMABLE 组件触发进食逻辑
            return foodComponent.onConsume(world, user, stack)
        } else {
            return stack
        }
    }
}