package org.cneko.justarod.entity.ai

import net.minecraft.entity.ai.goal.Goal
import net.minecraft.server.world.ServerWorld
import org.cneko.justarod.entity.SeeeeexNekoEntity
import org.cneko.justarod.JRUtil.Companion.getNekoInRange
import org.cneko.toneko.common.api.NekoQuery
import org.cneko.toneko.common.mod.entities.INeko

class SexualIntercourseGoal(private val neko: SeeeeexNekoEntity) : Goal() {
    private var target: INeko? = null

    override fun canStart(): Boolean {
        return true
    }

    fun slowTick() {
        val world = neko.world

        if (world is ServerWorld) {
            // 大于 500 个 Neko，不进行操作
            if (NekoQuery.NekoData.getNekoCount() > 500) {
                target = null
                return
            }

            target = when {
                target != null && neko.canMate(target!!) -> {
                    if (neko.nekoMateGoal.target == null) {
                        neko.tryMating(world, target!!)
                    }
                    target
                }

                neko.sexualDesire >= 50 -> world.getNekoInRange(neko, neko.sexualDesire * 0.09f).find {neko.canMate(it) }
                else -> null
            }
        }
    }
}
