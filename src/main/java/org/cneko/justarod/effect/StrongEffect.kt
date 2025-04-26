package org.cneko.justarod.effect

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectCategory
import net.minecraft.entity.effect.StatusEffects
import org.cneko.justarod.entity.Powerable

class StrongEffect: StatusEffect(StatusEffectCategory.BENEFICIAL, 0xffb6c1) {
    // 每tick都会调用一次，直到返回false
    override fun canApplyUpdateEffect(duration: Int, amplifier: Int): Boolean {
        return true
    }

    // 这个方法在应用药水效果时的每个tick会被调用。
    override fun applyUpdateEffect(entity: LivingEntity, amplifier: Int): Boolean {
        // 如果有虚弱的效果，则取消该效果。
        if (entity.hasStatusEffect(StatusEffects.WEAKNESS)) {
            entity.removeStatusEffect(StatusEffects.WEAKNESS)
        }
        // 增加体力
        if (entity is Powerable){
            entity.power += 0.03 * (amplifier+1)
        }
        return super.applyUpdateEffect(entity, amplifier)
    }
}