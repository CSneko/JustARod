package org.cneko.justarod.item

import net.minecraft.entity.LivingEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResult
import net.minecraft.world.World

/*
请勿模仿
 */
class FirecrackerRodItem:SelfUsedItem(Settings().maxCount(1).maxDamage(2000)) {
    override fun useOnSelf(stack: ItemStack, world: World?, entity: LivingEntity, slot: Int, selected: Boolean): ActionResult {
        val result = super.useOnSelf(stack, world, entity, slot, selected)
        if (result == ActionResult.SUCCESS) {
            // 生成没有伤害的爆炸效果
            if (world?.random?.nextInt(6) == 0) {
                val explosion = entity.world.createExplosion(
                    entity,
                    entity.pos.x,
                    entity.pos.y,
                    entity.pos.z,
                    3.0f,
                    false,
                    World.ExplosionSourceType.NONE
                )
            }
        }
        return result
    }
}