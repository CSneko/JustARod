package org.cneko.justarod.event;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;

// 我又活啦
public class EntityRespawnEvent {
    public static void init(){
        ServerPlayerEvents.AFTER_RESPAWN.register(SET_ROD_AFTER_RESPAWN);
    }
    private static final ServerPlayerEvents.AfterRespawn SET_ROD_AFTER_RESPAWN =
            (oldPlayer, newPlayer, alive) -> {
        var rodInside = oldPlayer.getRodInside();
        if (!rodInside.isEmpty()){
            newPlayer.setRodInside(rodInside);
        }
    };
}
