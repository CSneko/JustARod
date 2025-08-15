package org.cneko.justarod.effect

import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectCategory
import org.cneko.toneko.common.mod.effects.ExcitingEffect

class JumpNerfEffect: StatusEffect(StatusEffectCategory.HARMFUL, 0xe81845) {
    init {
        this.addAttributeModifier(
            EntityAttributes.GENERIC_JUMP_STRENGTH,
            ExcitingEffect.LOCATION,
            -0.3,
            EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE
        )
    }
}