package org.cneko.justarod.effect

import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectCategory
import net.minecraft.registry.Registries
import org.cneko.justarod.entity.Pregnant

class UrethritisEffect: StatusEffect(StatusEffectCategory.HARMFUL, 0xff0ead0) {
    override fun applyUpdateEffect(entity: LivingEntity?, amplifier: Int): Boolean {
        if (entity is Pregnant) {
            entity as Pregnant
            if (entity.urethritis<=0){
                // 清除效果
                entity.removeStatusEffect(Registries.STATUS_EFFECT.getEntry(JREffects.URETHRITIS_EFFECT))
            }
        }
        return super.applyUpdateEffect(entity, amplifier)
    }
}