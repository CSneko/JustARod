package org.cneko.justarod.item.syringe

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes

class ReverseGrowthAgentItem:BaseSyringeItem(Settings()) {
    override fun applyEffect(target: LivingEntity) {
        target.attributes.getCustomInstance(Attributes.SCALE)?.let { scale ->
            if (scale.baseValue > 0.1) {
                scale.baseValue -= 0.1
            }
        }
    }
}