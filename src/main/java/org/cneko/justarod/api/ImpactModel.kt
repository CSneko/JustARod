package org.cneko.justarod.api

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.network.ServerPlayerEntity
import org.cneko.toneko.common.mod.entities.INeko
import org.cneko.toneko.common.mod.entities.NekoEntity
import org.cneko.toneko.common.mod.util.EntityUtil
import java.util.HashMap

class ImpactModel {
    companion object{
        val using: HashMap<PlayerEntity,Boolean> = HashMap()
        fun isEnable(player: PlayerEntity): Boolean {
            return using.getOrDefault(player,false)
        }
        fun setEnable(player: PlayerEntity,enable: Boolean){
            using[player] = enable
        }
        fun tick(player: PlayerEntity){
            if(using.getOrDefault(player,false)){
                // 获取附近所有的NekoEntity
                val entities = EntityUtil.getLivingEntitiesInRange(player, player.world,16.0f)
                for (e in entities){
                    if (e is NekoEntity){
                        if (e.canMate(player)){
                            e.tryMating(player)
                        }
                    }
                }
            }
        }
    }
}

private fun NekoEntity.canMate(player: PlayerEntity): Boolean {
    return this.canMate(player as INeko)
}
private fun NekoEntity.tryMating(player: PlayerEntity) {
    if (player is ServerPlayerEntity) {
        this.tryMating(player.serverWorld, player as INeko)
    }
}
public fun PlayerEntity.isEnableImpact(): Boolean {
    return ImpactModel.isEnable(this)
}
