package org.cneko.justarod.event

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.world.ServerWorld
import org.cneko.justarod.effect.JREffects
import org.cneko.justarod.entity.Pregnant
import org.cneko.justarod.item.rod.addEffect
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

// 注意：请将这里的 import 替换为你项目中实际的 NekoEntity 所在的包路径！
import org.cneko.toneko.common.mod.entities.NekoEntity

object YuriKissManager {
    // 记录实体上一次触发亲亲的时间戳，防止每 tick 都在疯狂冒爱心（冷却设为 1 秒）
    private val lastKissTime = ConcurrentHashMap<UUID, Long>()

    fun onWorldTick(world: ServerWorld) {
        // 每 10 tick (0.5秒) 检查一次，大幅节省服务器性能
        if (world.time % 10L != 0L) return

        // 遍历世界中的玩家，以玩家为中心搜索附近 32 格的实体。
        // 这样既能实现 玩家x玩家、玩家x猫娘，也能实现玩家视距内的 猫娘x猫娘。
        for (player in world.players) {
            val box = player.boundingBox.expand(32.0)

            // 找出所有正在潜行（蹲下）且符合百合条件的实体
            val candidates = world.getEntitiesByClass(LivingEntity::class.java, box) {
                isReadyYuriKiss(it)
            }

            // 遍历并两两匹配
            for (i in candidates.indices) {
                val e1 = candidates[i]

                // 检查 e1 的冷却 (5秒内不重复触发)
                val time1 = lastKissTime[e1.uuid] ?: 0L
                if (world.time - time1 < 20L) continue

                for (j in i + 1 until candidates.size) {
                    val e2 = candidates[j]

                    // 检查 e2 的冷却
                    val time2 = lastKissTime[e2.uuid] ?: 0L
                    if (world.time - time2 < 20L) continue

                    // 1. 距离判定 (距离平方 < 2.25，即实际距离 < 1.5格)
                    if (e1.squaredDistanceTo(e2) > 2.25) continue

                    // 2. 面朝向判定 (互相看着对方)
                    if (isFaceToFace(e1, e2)) {
                        // 触发贴贴！重置冷却时间
                        lastKissTime[e1.uuid] = world.time
                        lastKissTime[e2.uuid] = world.time

                        applyKissEffect(e1, e2, world)
                        break // e1 已经匹配成功，跳出内层循环，不再同时和第三个人亲
                    }
                }
            }
        }
    }

    /**
     * 判断是否是百合
     */
    private fun isReadyYuriKiss(entity: LivingEntity): Boolean {
        // 1. 对于 NekoEntity，判断是否带有 yuri 标签
        if (entity is NekoEntity) {
            return entity.moeTags.contains("yuri")
        }

        // 2. 对于玩家或其他实体，判断是否实现了 Pregnant 接口且是百合
        val pregnant = entity as? Pregnant

        // 如果是玩家，还要蹲下
        var sneaking = true
        if (entity is PlayerEntity && !entity.isSneaking) {
            sneaking = false
        }
        return pregnant != null && pregnant.isYuri && sneaking
    }

    /**
     * 判断两个实体是否面对面
     */
    private fun isFaceToFace(e1: LivingEntity, e2: LivingEntity): Boolean {
        // 获取两者的视线方向向量
        val look1 = e1.rotationVector.normalize()
        val look2 = e2.rotationVector.normalize()

        // 计算 e1 指向 e2 的位置方向向量
        val vec1To2 = e2.eyePos.subtract(e1.eyePos).normalize()
        val vec2To1 = vec1To2.negate() // 反过来就是 e2 指向 e1

        // 计算点乘（判断视线和位置方向是否基本一致）
        // 点乘结果 > 0.6 大约表示视角偏差在 50 度以内，容错率比较舒适
        val dot1 = look1.dotProduct(vec1To2)
        val dot2 = look2.dotProduct(vec2To1)

        return dot1 > 0.6 && dot2 > 0.6
    }

    /**
     * 应用亲亲效果（纯粒子和药水，无提示）
     */
    private fun applyKissEffect(e1: LivingEntity, e2: LivingEntity, world: ServerWorld) {
        // 在两者正中间偏上的位置（大概在头部高度）生成爱心粒子
        val midX = (e1.x + e2.x) / 2.0
        val midY = (e1.y + e2.y) / 2.0 + 1.2
        val midZ = (e1.z + e2.z) / 2.0

        // 参数：粒子类型，X，Y，Z，数量，X偏移，Y偏移，Z偏移，速度
        world.spawnParticles(ParticleTypes.HEART, midX, midY, midZ, 8, 0.3, 0.3, 0.3, 0.05)

        // 赋予百合花香效果 (持续 5 秒 = 100 tick)
        e1.addEffect(JREffects.LILY_PHEROMONE_EFFECT, 20 * 5, 0)
        e2.addEffect(JREffects.LILY_PHEROMONE_EFFECT, 20 * 5, 0)
    }
}