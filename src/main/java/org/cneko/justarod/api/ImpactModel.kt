package org.cneko.justarod.api

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.network.ServerPlayerEntity
import org.cneko.toneko.common.mod.entities.INeko
import org.cneko.toneko.common.mod.entities.NekoEntity
import org.cneko.toneko.common.mod.util.EntityUtil
import java.util.HashMap

// 你也想来和我一起开银趴吗~ 嘿嘿~~
class ImpactModel {
    companion object {
        private val using: HashMap<PlayerEntity, Boolean> = HashMap()
        private val tickCounter: HashMap<PlayerEntity, Int> = HashMap()

        fun isEnable(player: PlayerEntity): Boolean {
            return using.getOrDefault(player, false)
        }

        fun setEnable(player: PlayerEntity, enable: Boolean) {
            using[player] = enable
            if (!enable) {
                tickCounter.remove(player) // 清理计数器
            }
        }

        fun tick(player: PlayerEntity) {
            if (using.getOrDefault(player, false)) {
                val currentTick = tickCounter.getOrDefault(player, 0)
                if (currentTick >= 4) { // 每5个tick触发
                    // 获取附近所有的NekoEntity
                    val entities = EntityUtil.getLivingEntitiesInRange(player, player.world, 16.0f)
                    for (e in entities) {
                        if (e is NekoEntity) {
                            if (e.canMate(player)) {
                                e.tryMating(player)
                            }
                        }
                    }
                    tickCounter[player] = 0 // 重置计数器
                } else {
                    tickCounter[player] = currentTick + 1
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
fun PlayerEntity.isEnableImpact(): Boolean {
    return ImpactModel.isEnable(this)
}
