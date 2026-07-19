package org.cneko.justarod.item.rod

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.InteractionResult
import net.minecraft.world.level.Level

/*
现实中不建议这么玩！！！
 */
class CactusRodItem: SelfUsedItem(Settings().maxCount(1).maxDamage(2000)) {
    override fun useOnSelf(stack: ItemStack, world: Level?, entity: LivingEntity, slot: Int, selected: Boolean): InteractionResult {
        if (super.useOnSelf(stack, world, entity, slot, selected) == InteractionResult.SUCCESS){
            // 扣血
            entity.hurt(entity.level().damageSources.cactus(), 10f)
            return InteractionResult.SUCCESS
        }
        return InteractionResult.PASS
    }
}