package org.cneko.justarod.effect

import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectCategory
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import org.cneko.justarod.JRUtil.Companion.rodId

class PregnantEffect: StatusEffect(StatusEffectCategory.NEUTRAL, 0xe9b8b3) {
    companion object{
        const val ID = "pregnant"
        val IDENTIFIER = rodId(ID)
    }
    init {
        // 速度减慢
        this.addAttributeModifier(EntityAttributes.GENERIC_MOVEMENT_SPEED,IDENTIFIER, -0.03, EntityAttributeModifier.Operation.ADD_VALUE)
        // 跳跃高度减小
        this.addAttributeModifier(EntityAttributes.GENERIC_JUMP_STRENGTH, IDENTIFIER, -0.1, EntityAttributeModifier.Operation.ADD_VALUE)
        // 攻击力降低
        this.addAttributeModifier(EntityAttributes.GENERIC_ATTACK_DAMAGE, IDENTIFIER, -0.5, EntityAttributeModifier.Operation.ADD_VALUE)
    }

    override fun canApplyUpdateEffect(duration: Int, amplifier: Int): Boolean {
        return true
    }

    override fun applyInstantEffect(
        source: Entity?,
        attacker: Entity?,
        target: LivingEntity?,
        amplifier: Int,
        proximity: Double
    ) {
        super.applyInstantEffect(source, attacker, target, amplifier, proximity)
        // 0.0005%的几率触发反胃
        if (target != null && target.random.nextInt(200000) == 0) {
            target.addStatusEffect(
                StatusEffectInstance(
                    StatusEffects.NAUSEA,
                    100, // 持续时间为5秒
                    0
                )
            )
        }
    }
}