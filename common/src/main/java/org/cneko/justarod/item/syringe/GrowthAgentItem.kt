package org.cneko.justarod.item.syringe
import org.cneko.justarod.JRAttributes
import org.cneko.justarod.JRIds

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player

/*
喝~ 长大了（玩家优先走更丝滑防抖的萌态体型属性 player.cute_scale）
 */
class GrowthAgentItem: BaseSyringeItem(JRIds.itemProps("growth_agent")) {

    override fun applyEffect(target: LivingEntity) {
        if (target is Player) {
            val cuteScale = target.getAttribute(JRAttributes.PLAYER_CUTE_SCALE)
            if (cuteScale != null) {
                if (cuteScale.baseValue < 4.0) {
                    cuteScale.baseValue = (cuteScale.baseValue + 0.1).coerceAtMost(4.0)
                }
                return
            }
        }
        target.getAttribute(Attributes.SCALE)?.let { scale ->
            if (scale.baseValue < 4) {
                scale.baseValue += 0.1
            }
        }
    }

}