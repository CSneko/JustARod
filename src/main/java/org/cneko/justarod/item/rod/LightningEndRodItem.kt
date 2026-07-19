package org.cneko.justarod.item.rod

import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LightningBolt
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.level.Level
import org.cneko.justarod.item.JRComponents

/*
毕竟是铜做的，多少带点毒吧
 */
class LightningEndRodItem: SelfUsedItem(Settings().maxCount(1).maxDamage(2000).component(JRComponents.Companion.USED_TIME_MARK,0)) {
    override fun useOnSelf(
        stack: ItemStack,
        world: Level?,
        entity: LivingEntity,
        slot: Int,
        selected: Boolean
    ): InteractionResult {
        val result:InteractionResult = super.useOnSelf(stack, world, entity, slot, selected)
        if (result == InteractionResult.SUCCESS){
            // 中毒中毒~
            val e = MobEffectInstance(MobEffects.POISON, 200, 0)
            entity.addEffect(e)
            // 雷雨天，召唤雷电
            if (entity.level().isThundering && world?.random?.nextInt(3) == 0){
                val light = LightningBolt(EntityType.LIGHTNING_BOLT,world)
                light.setPos(entity.x, entity.y, entity.z)
                entity.level().addFreshEntity(light)
            }
            return result
        }
        return result
    }

    override fun appendTooltip(
        stack: ItemStack?,
        context: TooltipContext?,
        tooltip: MutableList<Component>?,
        type: TooltipFlag?
    ) {
        super.appendHoverText(stack, context, tooltip, type)
        tooltip?.add(Component.translatable("item.justarod.lightning_end_rod.tooltip"))
    }
}