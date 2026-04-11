package org.cneko.justarod.entity.ai

import net.minecraft.entity.ai.goal.Goal
import net.minecraft.server.world.ServerWorld
import org.cneko.justarod.JRUtil.Companion.getPlayerInRange
import org.cneko.justarod.entity.SeeeeexNekoEntity
import org.cneko.toneko.common.mod.entities.INeko
import org.cneko.toneko.common.mod.entities.NekoEntity

/*
和你交配，同意一下
 */
class SexualIntercourseGoal(private val neko: SeeeeexNekoEntity) : Goal() {
    // 将目标类型改为 INeko 以兼容 PlayerEntity 和 NekoEntity
    private var target: INeko? = null

    override fun canStart(): Boolean {
        return true
    }

    fun slowTick() {
        val world = neko.world

        if (world is ServerWorld) {
            // 如果已有目标且可以交配
            if (target != null && neko.canMate(target)) {
                if (neko.nekoMateGoal.target == null) {
                    neko.tryMating(world, target!!)
                }
                return
            }

            // 如果没有目标，且欲望达标，则开始寻找新目标
            if (neko.sexualDesire >= 50) {
                val range = neko.sexualDesire * 0.09f

                // 1. 优先寻找范围内的玩家 (默认行为)
                var newTarget: INeko? = world.getPlayerInRange(neko, range).find { neko.canMate(it as INeko) } as INeko?

                // 2. 如果开启了AUTO_MATE_WITH_NEKO 且 周围没有合适的玩家，则寻找周围其他的Neko
                if (newTarget == null && SeeeeexNekoEntity.AUTO_MATE_WITH_NEKO) {
                    val searchBox = neko.boundingBox.expand(range.toDouble())
                    newTarget = world.getEntitiesByClass(NekoEntity::class.java, searchBox) {
                        it != neko && neko.canMate(it) // 排除自己，并且满足交配条件
                    }.minByOrNull { it.distanceTo(neko) } // 找离得最近的那个
                }

                target = newTarget
            } else {
                target = null
            }
        }
    }
}