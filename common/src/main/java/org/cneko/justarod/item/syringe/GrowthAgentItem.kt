package org.cneko.justarod.item.syringe
import org.cneko.justarod.JRIds

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes

/*
喝~ 长大了
 */
class GrowthAgentItem: BaseSyringeItem(JRIds.itemProps("growth_agent")) {

    override fun applyEffect(target: LivingEntity) {
        target.getAttribute(Attributes.SCALE)?.let { scale ->
            if (scale.baseValue < 4) {
                scale.baseValue += 0.1
            }
        }
    }

}