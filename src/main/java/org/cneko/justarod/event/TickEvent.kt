package org.cneko.justarod.event

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import org.cneko.justarod.api.ImpactModel
import org.cneko.justarod.api.isEnableImpact

class TickEvent {
    companion object{
        fun init() {

            ServerTickEvents.START_WORLD_TICK.register { world ->
                if (world.isClient) return@register
                for (player in world.players) {
                    if (player.isEnableImpact()){
                        ImpactModel.tick(player)
                    }
                }
            }


        }
    }
}