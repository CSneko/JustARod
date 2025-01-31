package org.cneko.justarod.entity.ai

import net.minecraft.entity.ai.goal.Goal
import net.minecraft.server.world.ServerWorld
import org.cneko.justarod.entity.SeeeeexNekoEntity
import org.cneko.justarod.JRUtil.Companion.getNekoInRange
import org.cneko.toneko.common.mod.entities.INeko

class SexualIntercourseGoal(val neko: SeeeeexNekoEntity) : Goal() {
    private var target: INeko? = null

    override fun canStart(): Boolean {
        return true
    }

    fun slowTick() {
        val world = neko.world

        if (world is ServerWorld) {
            val nearbyNekos = world.getNekoInRange(neko, neko.sexualDesire * 0.09f)
            // 大于 3 个 Neko，不进行操作
            if (nearbyNekos.size > 3) {
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

                neko.sexualDesire >= 50 -> nearbyNekos.find {neko.canMate(it) }
                else -> null
            }
        }
    }
}
