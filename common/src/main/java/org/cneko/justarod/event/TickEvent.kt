package org.cneko.justarod.event

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import org.cneko.justarod.api.ImpactModel

class TickEvent {
    companion object{
        fun init() {
            // 26.x Fabric：START_WORLD_TICK/END_WORLD_TICK 更名为 START_LEVEL_TICK/END_LEVEL_TICK
            ServerTickEvents.START_LEVEL_TICK.register(ServerTickEvents.StartLevelTick { world ->
                if (world.dimension() == org.cneko.justarod.body.BodyWorld.DIMENSION && !org.cneko.justarod.config.JRConfig.isInsideBodyEnabled()) return@StartLevelTick
                for (player in world.players()) {
                    if (ImpactModel.isEnable(player)){
                        ImpactModel.tick(player)
                    }
                }
            })

            ServerTickEvents.END_LEVEL_TICK.register(ServerTickEvents.EndLevelTick { world ->
                if (world.dimension() == org.cneko.justarod.body.BodyWorld.DIMENSION && !org.cneko.justarod.config.JRConfig.isInsideBodyEnabled()) return@EndLevelTick
                YuriKissManager.onWorldTick(world)
            })


        }
    }
}
