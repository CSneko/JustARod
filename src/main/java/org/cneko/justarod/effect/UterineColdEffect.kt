package org.cneko.justarod.effect

import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import org.cneko.justarod.JRUtil.Companion.rodId

class UterineColdEffect : MobEffect(MobEffectCategory.HARMFUL, 0x99FFFF) {
    companion object{
        val LOCATION = rodId("uterine_cold")
    }
    init {
        // 1. 降低移动速度 (模拟身体寒冷、畏寒导致的行动迟缓)
        this.addAttributeModifier(
            Attributes.MOVEMENT_SPEED,
            LOCATION,
            -0.15, // 速度降低 15% (Level 1)
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        )

        // 2. 降低攻击速度 (模拟身体僵硬)
        this.addAttributeModifier(
            Attributes.ATTACK_SPEED,
            LOCATION,
            -0.1, // 攻速降低 10%
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        )
    }
}