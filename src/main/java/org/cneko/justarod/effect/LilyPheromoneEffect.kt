package org.cneko.justarod.effect

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectCategory
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.world.ServerWorld
import org.cneko.justarod.entity.Pregnant
import kotlin.math.max

class LilyPheromoneEffect : StatusEffect(StatusEffectCategory.BENEFICIAL, 0xFFC0CB) {

    // 决定效果多久执行一次
    // 设定为每 40 tick (2秒) 触发一次核心逻辑，避免每 tick 运算消耗性能
    override fun canApplyUpdateEffect(duration: Int, amplifier: Int): Boolean {
        return duration % 40 == 0
    }

    // 每次触发时执行的具体逻辑
    override fun applyUpdateEffect(entity: LivingEntity, amplifier: Int): Boolean {
        super.applyUpdateEffect(entity, amplifier)

        // 仅在服务端处理数值和粒子
        if (entity.world.isClient) return true

        // 1. 基础治愈：微量回血 (百合的治愈之力)
        if (entity.health < entity.maxHealth) {
            entity.heal(1.0f + amplifier.toFloat()) // 每次回半颗心
        }

        // 2. 视觉表现：散发阵阵花香
        // 樱花花瓣 (CHERRY_LEAVES)非常适合百合氛围
        val serverWorld = entity.world as? ServerWorld
        serverWorld?.spawnParticles(
            ParticleTypes.CHERRY_LEAVES,
            entity.x, entity.y + 1.0, entity.z,
            2, 0.4, 0.5, 0.4, 0.05
        )

        // 3. 核心生理系统联动：驱散病痛
        if (entity is Pregnant) {

            // (1) 驱散宫寒 (UterineCold)
            // 每次触发减少 100 tick 的寒气积累 (两人抱在一起取暖)
            val currentCold = entity.uterineCold
            if (currentCold > 0) {
                entity.uterineCold = max(0, currentCold - 100 * (amplifier + 1))
            }

            // (2) 缓解痛经 (MenstruationComfort)
            // 极大补充月经舒适度。因为每 2 秒触发一次，这里补充 600 tick (30秒) 的舒适度
            // 这样只要身上有这个 Buff，舒适度就会永远处于溢出状态，从而完全免疫痛经扣血
            entity.menstruationComfort = entity.menstruationComfort + 600

            // (3) 舒缓紧张与焦虑
            // 稍微减缓憋尿/憋屎带来的急迫感（精神放松），每次扣减 20 tick 进度
            if (entity.urination > 0) {
                entity.urination = max(0, entity.urination - 20)
            }
            if (entity.excretion > 0) {
                entity.excretion = max(0, entity.excretion - 20)
            }
        }
        return true
    }
}