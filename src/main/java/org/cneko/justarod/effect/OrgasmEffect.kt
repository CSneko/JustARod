package org.cneko.justarod.effect

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.MovementType
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectCategory
import net.minecraft.particle.ParticleTypes
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.random.Random

/*
在高潮后会疯狂喘气，而且会沉浸在其中，脑子很难正常思考，也很难正常说话，有点晕乎乎的感觉，只想躺在床上
而且呢，还会不自觉的发出娇喘，而且会感觉很热，但总体来说还是很舒服的（不信你可以试试）
还有就是，在门口抽插其实会比在里面更有感觉哦
 */
class OrgasmEffect : StatusEffect(StatusEffectCategory.BENEFICIAL, 0xe9b8b3) {
    // 每tick都会调用一次，直到返回false
    override fun canApplyUpdateEffect(duration: Int, amplifier: Int): Boolean {
        return true
    }

    // 这个方法在应用药水效果时的每个tick会被调用。
    override fun applyUpdateEffect(entity: LivingEntity, amplifier: Int): Boolean {
        val world = entity.world
        val random: Random = world.random
        // 添加爱心效果
        world.addParticle(
            ParticleTypes.HEART,
            entity.x + random.nextInt(1) - 1,
            entity.y + random.nextInt(2) + 2,
            entity.z + random.nextInt(2) - 1,
            0.0,
            amplifier+1.5,
            0.0
        )
        if (random.nextBoolean()) {
            // 随机移动玩家的位置
            val x: Int = random.nextInt((amplifier + 1) * 5) - 5
            val z: Int = random.nextInt((amplifier + 1) * 5) - 5
            entity.move(MovementType.SHULKER_BOX, Vec3d(x * 0.001, amplifier * 0.0003, z * 0.001))
        }
        return super.applyUpdateEffect(entity, amplifier)
    }
}