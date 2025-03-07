package org.cneko.justarod.item.syringe

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.attribute.EntityAttributes

class ReverseGrowthAgentItem:BaseSyringeItem(Settings()) {
    override fun applyEffect(target: LivingEntity) {
        target.attributes.getCustomInstance(EntityAttributes.GENERIC_SCALE)?.let { scale ->
            if (scale.baseValue > 0.1) {
                scale.baseValue -= 0.1
            }
        }
    }
}