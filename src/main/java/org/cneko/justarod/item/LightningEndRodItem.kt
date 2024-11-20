package org.cneko.justarod.item

import net.minecraft.entity.EntityType
import net.minecraft.entity.LightningEntity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.ItemStack
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.world.World

/*
毕竟是铜做的，多少带点毒吧
 */
class LightningEndRodItem: SelfUsedItem(Settings().maxCount(1).maxDamage(2000).component(JRComponents.USED_TIME_MARK,0)) {
    override fun useOnSelf(
        stack: ItemStack,
        world: World?,
        entity: LivingEntity,
        slot: Int,
        selected: Boolean
    ): ActionResult {
        val result:ActionResult = super.useOnSelf(stack, world, entity, slot, selected)
        if (result == ActionResult.SUCCESS){
            // 中毒中毒~
            val e = StatusEffectInstance(StatusEffects.POISON, 200, 0)
            entity.addStatusEffect(e)
            // 雷雨天，召唤雷电
            if (entity.world.isThundering && world?.random?.nextInt(3) == 0){
                val light = LightningEntity(EntityType.LIGHTNING_BOLT,world)
                light.setPos(entity.x, entity.y, entity.z)
                entity.world.spawnEntity(light)
            }
            return result
        }
        return result
    }

    override fun appendTooltip(
        stack: ItemStack?,
        context: TooltipContext?,
        tooltip: MutableList<Text>?,
        type: TooltipType?
    ) {
        super.appendTooltip(stack, context, tooltip, type)
        tooltip?.add(Text.translatable("item.justarod.lightning_end_rod.tooltip"))
    }
}