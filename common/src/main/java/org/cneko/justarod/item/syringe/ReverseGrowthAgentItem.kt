package org.cneko.justarod.item.syringe
import org.cneko.justarod.JRAttributes
import org.cneko.justarod.JRIds

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player

class ReverseGrowthAgentItem: BaseSyringeItem(JRIds.itemProps("reverse_growth_agent")) {
    override fun applyEffect(target: LivingEntity) {
        if (target is Player) {
            val cuteScale = target.getAttribute(JRAttributes.PLAYER_CUTE_SCALE)
            if (cuteScale != null) {
                if (cuteScale.baseValue > 0.25) {
                    cuteScale.baseValue = (cuteScale.baseValue - 0.1).coerceAtLeast(0.25)
                }
                return
            }
        }
        target.getAttribute(Attributes.SCALE)?.let { scale ->
            if (scale.baseValue > 0.2) {
                scale.baseValue -= 0.1
            }
        }
    }
}