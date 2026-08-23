package org.cneko.justarod.effect

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.core.registries.BuiltInRegistries
import org.cneko.justarod.entity.Pregnant

class UrethritisEffect: MobEffect(MobEffectCategory.HARMFUL, 0xff0ead0) {
    override fun applyEffectTick(entity: LivingEntity?, amplifier: Int): Boolean {
        if (entity is Pregnant) {
            entity as Pregnant
            if (entity.urethritis<=0){
                // 清除效果
                entity.removeEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(JREffects.URETHRITIS_EFFECT))
            }
        }
        return super.applyEffectTick(entity, amplifier)
    }
}