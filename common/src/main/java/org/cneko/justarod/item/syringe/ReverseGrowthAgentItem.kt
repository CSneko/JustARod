package org.cneko.justarod.item.syringe
import org.cneko.justarod.JRIds

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes

class ReverseGrowthAgentItem:BaseSyringeItem(JRIds.itemProps("reverse_growth_agent")) {
    override fun applyEffect(target: LivingEntity) {
        target.getAttribute(Attributes.SCALE)?.let { scale ->
            if (scale.baseValue > 0.2) {
                scale.baseValue -= 0.1
            }
        }
    }
}