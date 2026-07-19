package org.cneko.justarod.api

import net.minecraft.world.entity.player.Player
import net.minecraft.server.level.ServerPlayer
import org.cneko.toneko.common.mod.entities.INeko
import org.cneko.toneko.common.mod.entities.NekoEntity
import org.cneko.toneko.common.mod.util.EntityUtil
import java.util.HashMap

// 你也想来和我一起开银趴吗~ 嘿嘿~~
class ImpactModel {
    companion object {
        private val using: HashMap<Player, Boolean> = HashMap()
        private val tickCounter: HashMap<Player, Int> = HashMap()

        fun isEnable(player: Player): Boolean {
            return using.getOrDefault(player, false)
        }

        fun setEnable(player: Player, enable: Boolean) {
            using[player] = enable
            if (!enable) {
                tickCounter.remove(player) // 清理计数器
            }
        }

        fun tick(player: Player) {
            if (using.getOrDefault(player, false)) {
                val currentTick = tickCounter.getOrDefault(player, 0)
                if (currentTick >= 20) { // 每20个tick触发
                    // 获取附近所有的NekoEntity
                    val entities = EntityUtil.getLivingEntitiesInRange(player, player.level(), 16.0f)
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


private fun NekoEntity.canMate(player: Player): Boolean {
    return this.canMate(player as INeko)
}
private fun NekoEntity.tryMating(player: Player) {
    if (player is ServerPlayer) {
        this.tryMating(player.serverLevel(), player as INeko)
    }
}
fun Player.isEnableImpact(): Boolean {
    return ImpactModel.isEnable(this)
}
