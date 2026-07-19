package org.cneko.justarod.item.rod

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.InteractionResult
import net.minecraft.world.level.Level

/*
请勿模仿
 */
class FirecrackerRodItem:SelfUsedItem(Settings().maxCount(1).maxDamage(2000)) {
    override fun useOnSelf(stack: ItemStack, world: Level?, entity: LivingEntity, slot: Int, selected: Boolean): InteractionResult {
        val result = super.useOnSelf(stack, world, entity, slot, selected)
        if (result == InteractionResult.SUCCESS) {
            // 生成没有伤害的爆炸效果
            if (world?.random?.nextInt(6) == 0) {
                val explosion = entity.level().explode(
                    entity,
                    entity.pos.x,
                    entity.pos.y,
                    entity.pos.z,
                    3.0f,
                    false,
                    Level.ExplosionSourceType.NONE
                )
            }
        }
        return result
    }
}