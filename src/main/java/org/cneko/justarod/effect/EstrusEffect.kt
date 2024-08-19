package org.cneko.justarod.effect

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.MovementType
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectCategory
import net.minecraft.particle.ParticleTypes
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.random.Random

class EstrusEffect:StatusEffect(StatusEffectCategory.BENEFICIAL, 0xe9b8b5) {
    override fun canApplyUpdateEffect(duration: Int, amplifier: Int): Boolean {
        return true
    }

    override fun applyUpdateEffect(entity: LivingEntity, amplifier: Int): Boolean {
        val world = entity.world
        val random: Random = world.random
        // 1/20添加爱心效果
        if (kotlin.random.Random.nextInt(20) == 0) {
            world.addParticle(
                ParticleTypes.HEART,
                entity.x + random.nextInt(2) - 1,
                entity.y + random.nextInt(2) + 2,
                entity.z + random.nextInt(2) - 1,
                0.0,
                2.0,
                0.0
            )
        }
        // 随机移动玩家的位置
        val x: Int = random.nextInt(10) - 5
        val z: Int = random.nextInt(10) - 5
        entity.move(MovementType.SHULKER_BOX, Vec3d(x * 0.1, 0.0, z * 0.1))

        return super.applyUpdateEffect(entity, amplifier)
    }
}