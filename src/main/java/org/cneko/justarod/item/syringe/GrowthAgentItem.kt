package org.cneko.justarod.item.syringe

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.attribute.EntityAttributes

/*
喝~ 长大了
 */
class GrowthAgentItem: BaseSyringeItem(Settings()) {

    override fun applyEffect(target: LivingEntity) {
        target.attributes.getCustomInstance(EntityAttributes.GENERIC_SCALE)?.let { scale ->
            if (scale.baseValue < 4) {
                scale.baseValue += 0.1
            }
        }
    }

}