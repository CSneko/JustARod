package org.cneko.justarod.effect

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects

class VaginitisEffect: MobEffect(MobEffectCategory.HARMFUL, 0xD3D3D3) {
    override fun shouldApplyEffectTickThisTick(duration: Int, amplifier: Int): Boolean {
        return true
    }

    override fun applyEffectTick(entity: LivingEntity?, amplifier: Int): Boolean {
        // 1/200的概率缓慢
        if (entity != null && entity.random.nextInt(200) == 0) {
            entity.addEffect(MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 0))
        }
        return super.applyEffectTick(entity, amplifier)
    }
}