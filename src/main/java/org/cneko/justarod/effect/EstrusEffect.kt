package org.cneko.justarod.effect

import net.minecraft.entity.EntityPose
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.MovementType
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectCategory
import net.minecraft.particle.ParticleTypes
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.random.Random
import org.cneko.justarod.entity.Sexual
import org.cneko.toneko.common.mod.api.EntityPoseManager
import org.cneko.toneko.common.mod.effects.ExcitingEffect

/*
其实人类没啥发情期，感觉到的发情也就是想要涩涩而已
 */
class EstrusEffect:StatusEffect(StatusEffectCategory.BENEFICIAL, 0xffb6c1) {
    init {
        this.addAttributeModifier(
            EntityAttributes.GENERIC_MOVEMENT_SPEED,
            ExcitingEffect.LOCATION,
            0.3,
            EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE
        )
    }
    override fun canApplyUpdateEffect(duration: Int, amplifier: Int): Boolean {
        return true
    }

    override fun applyUpdateEffect(entity: LivingEntity, amplifier: Int): Boolean {
        val world = entity.world
        val random: Random = world.random
        //添加爱心效
        world.addParticle(
            ParticleTypes.HEART,
            entity.x + random.nextInt(2) - 1,
            entity.y + random.nextInt(2) + 2,
            entity.z + random.nextInt(2) - 1,
            0.0,
            2.0,
            0.0
        )

        if (entity is Sexual){
            // 1/10的概率增加性欲
            if(entity.random.nextInt(10) == 0){
                entity.increaseSexualDesire(1)
            }
        }else{
            // 让实体趴下
            EntityPoseManager.setPose(entity, EntityPose.SWIMMING)

            // 随机移动位置
            val x: Int = random.nextInt(10) - 5
            val z: Int = random.nextInt(10) - 5
            entity.move(MovementType.SHULKER_BOX, Vec3d(x * 0.03, 0.01, z * 0.03))
        }

        return super.applyUpdateEffect(entity, amplifier)
    }

}