package org.cneko.justarod.item.rod
import org.cneko.justarod.JRIds

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.InteractionResult
import net.minecraft.world.level.Level

/*
请勿模仿
 */
class FirecrackerRodItem:SelfUsedItem(JRIds.itemProps("firecracker_rod").stacksTo(1).durability(2000)) {
    override fun useOnSelf(stack: ItemStack, world: Level?, entity: LivingEntity, slot: Int, selected: Boolean): InteractionResult {
        val result = super.useOnSelf(stack, world, entity, slot, selected)
        if (result == InteractionResult.SUCCESS) {
            // 生成没有伤害的爆炸效果
            if (world?.random?.nextInt(6) == 0) {
                val explosion = entity.level().explode(
                    entity,
                    entity.position().x,
                    entity.position().y,
                    entity.position().z,
                    3.0f,
                    false,
                    Level.ExplosionInteraction.NONE
                )
            }
        }
        return result
    }
}