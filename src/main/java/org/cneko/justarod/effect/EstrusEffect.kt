package org.cneko.justarod.effect

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.MoverType
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.phys.Vec3
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.Pose
import org.cneko.justarod.entity.Sexual
import org.cneko.toneko.common.mod.api.EntityPoseManager
import org.cneko.toneko.common.mod.effects.ExcitingEffect
import kotlin.random.Random

/*
其实人类没啥发情期，感觉到的发情也就是想要涩涩而已
 */
class EstrusEffect:MobEffect(MobEffectCategory.BENEFICIAL, 0xffb6c1) {
    init {
        this.addAttributeModifier(
            Attributes.MOVEMENT_SPEED,
            ExcitingEffect.LOCATION,
            0.3,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        )
    }
    override fun shouldApplyEffectTickThisTick(duration: Int, amplifier: Int): Boolean {
        return true
    }

    override fun applyEffectTick(entity: LivingEntity, amplifier: Int): Boolean {
        val world = entity.level()
        val random: RandomSource = world.random
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
            EntityPoseManager.setPose(entity, Pose.SWIMMING)

            // 随机移动位置
            val x: Int = random.nextInt(10) - 5
            val z: Int = random.nextInt(10) - 5
            entity.move(MoverType.SHULKER_BOX, Vec3(x * 0.03, 0.01, z * 0.03))
        }

        return super.applyEffectTick(entity, amplifier)
    }

}