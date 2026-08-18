package org.cneko.justarod.item.syringe

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes

/*
喝~ 长大了
 */
class GrowthAgentItem: BaseSyringeItem(Properties()) {

    override fun applyEffect(target: LivingEntity) {
        target.getAttribute(Attributes.SCALE)?.let { scale ->
            if (scale.baseValue < 4) {
                scale.baseValue += 0.1
            }
        }
    }

}