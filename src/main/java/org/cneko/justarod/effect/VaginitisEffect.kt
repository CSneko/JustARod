package org.cneko.justarod.effect

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectCategory
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects

class VaginitisEffect: StatusEffect(StatusEffectCategory.HARMFUL, 0xD3D3D3) {
    override fun canApplyUpdateEffect(duration: Int, amplifier: Int): Boolean {
        return true
    }

    override fun applyUpdateEffect(entity: LivingEntity?, amplifier: Int): Boolean {
        // 1/200的概率缓慢
        if (entity != null && entity.random.nextInt(200) == 0) {
            entity.addStatusEffect(StatusEffectInstance(StatusEffects.SLOWNESS, 200, 0))
        }
        return super.applyUpdateEffect(entity, amplifier)
    }
}