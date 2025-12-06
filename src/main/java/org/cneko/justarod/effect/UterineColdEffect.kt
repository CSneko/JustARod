package org.cneko.justarod.effect

import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectCategory
import org.cneko.justarod.JRUtil.Companion.rodId

class UterineColdEffect : StatusEffect(StatusEffectCategory.HARMFUL, 0x99FFFF) {
    companion object{
        val LOCATION = rodId("uterine_cold")
    }
    init {
        // 1. 降低移动速度 (模拟身体寒冷、畏寒导致的行动迟缓)
        this.addAttributeModifier(
            EntityAttributes.GENERIC_MOVEMENT_SPEED,
            LOCATION,
            -0.15, // 速度降低 15% (Level 1)
            EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        )

        // 2. 降低攻击速度 (模拟身体僵硬)
        this.addAttributeModifier(
            EntityAttributes.GENERIC_ATTACK_SPEED,
            LOCATION,
            -0.1, // 攻速降低 10%
            EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        )
    }
}