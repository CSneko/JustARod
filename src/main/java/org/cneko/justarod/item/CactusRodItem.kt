package org.cneko.justarod.item

import net.minecraft.entity.LivingEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResult
import net.minecraft.world.World

/*
现实中不建议这么玩！！！
 */
class CactusRodItem: SelfUsedItem(Settings().maxCount(1).maxDamage(2000)) {
    override fun useOnSelf(stack: ItemStack, world: World?, entity: LivingEntity, slot: Int, selected: Boolean): ActionResult {
        if (super.useOnSelf(stack, world, entity, slot, selected) == ActionResult.SUCCESS){
            // 扣血
            entity.damage(entity.world.damageSources.cactus(), 10f)
            return ActionResult.SUCCESS
        }
        return ActionResult.PASS
    }
}