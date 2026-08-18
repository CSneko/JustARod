package org.cneko.justarod.item.syringe

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes

class ReverseGrowthAgentItem:BaseSyringeItem(Properties()) {
    override fun applyEffect(target: LivingEntity) {
        target.getAttribute(Attributes.SCALE)?.let { scale ->
            if (scale.baseValue > 0.2) {
                scale.baseValue -= 0.1
            }
        }
    }
}