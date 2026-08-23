package org.cneko.justarod.effect

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import org.cneko.justarod.JRUtil.Companion.rodId

class PregnantEffect: MobEffect(MobEffectCategory.NEUTRAL, 0xe9b8b3) {
    companion object{
        const val ID = "pregnant"
        val IDENTIFIER = rodId(ID)
    }
    init {
        // 速度减慢
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED,IDENTIFIER, -0.03, AttributeModifier.Operation.ADD_VALUE)
        // 跳跃高度减小
        this.addAttributeModifier(Attributes.JUMP_STRENGTH, IDENTIFIER, -0.1, AttributeModifier.Operation.ADD_VALUE)
        // 攻击力降低
        this.addAttributeModifier(Attributes.ATTACK_DAMAGE, IDENTIFIER, -0.5, AttributeModifier.Operation.ADD_VALUE)
    }

    override fun shouldApplyEffectTickThisTick(duration: Int, amplifier: Int): Boolean {
        return true
    }

    override fun applyInstantenousEffect(
        source: Entity?,
        attacker: Entity?,
        target: LivingEntity?,
        amplifier: Int,
        proximity: Double
    ) {
        super.applyInstantenousEffect(source, attacker, target, amplifier, proximity)
        // 0.0005%的几率触发反胃
        if (target != null && target.random.nextInt(200000) == 0) {
            target.addEffect(
                MobEffectInstance(
                    MobEffects.CONFUSION,
                    100, // 持续时间为5秒
                    0
                )
            )
        }
    }
}