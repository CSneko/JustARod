package org.cneko.justarod.effect

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.world.effect.MobEffects
import org.cneko.justarod.entity.Powerable

class StrongEffect: MobEffect(MobEffectCategory.BENEFICIAL, 0xffb6c1) {
    // 每tick都会调用一次，直到返回false
    override fun shouldApplyEffectTickThisTick(duration: Int, amplifier: Int): Boolean {
        return true
    }

    // 这个方法在应用药水效果时的每个tick会被调用。
    override fun applyEffectTick(entity: LivingEntity, amplifier: Int): Boolean {
        // 如果有虚弱的效果，则取消该效果。
        if (entity.hasEffect(MobEffects.WEAKNESS)) {
            entity.removeEffect(MobEffects.WEAKNESS)
        }
        // 增加体力
        if (entity is Powerable){
            entity.power += 0.03 * (amplifier+1)
        }
        return super.applyEffectTick(entity, amplifier)
    }
}