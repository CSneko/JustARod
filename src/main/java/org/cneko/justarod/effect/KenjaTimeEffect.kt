package org.cneko.justarod.effect

import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectCategory
import org.cneko.toneko.common.mod.effects.ExcitingEffect

class KenjaTimeEffect: StatusEffect(StatusEffectCategory.BENEFICIAL, 0x3cfa3c)  {
    init{
        this.addAttributeModifier(
            EntityAttributes.GENERIC_MOVEMENT_SPEED,
            ExcitingEffect.LOCATION,
            -0.15,
            EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE
        )
    }
}