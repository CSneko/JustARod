package org.cneko.justarod.entity.ai

import net.minecraft.entity.ai.goal.Goal
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.world.ServerWorld
import org.cneko.justarod.JRUtil.Companion.getPlayerInRange
import org.cneko.justarod.entity.SeeeeexNekoEntity
import org.cneko.toneko.common.mod.entities.INeko

/*
和你交配，同意一下
 */
class SexualIntercourseGoal(private val neko: SeeeeexNekoEntity) : Goal() {
    private var target: PlayerEntity? = null

    override fun canStart(): Boolean {
        return true
    }

    fun slowTick() {
        val world = neko.world

        if (world is ServerWorld) {


            target = when {
                target != null && neko.canMate(target as INeko) -> {
                    if (neko.nekoMateGoal.target == null) {
                        neko.tryMating(world, target as INeko)
                    }
                    target
                }

                neko.sexualDesire >= 50 -> world.getPlayerInRange(neko, neko.sexualDesire * 0.09f).find {neko.canMate(it as INeko) }
                else -> null
            }
        }
    }
}
