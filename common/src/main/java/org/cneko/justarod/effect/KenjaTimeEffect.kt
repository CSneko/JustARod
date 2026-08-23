package org.cneko.justarod.effect

import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import org.cneko.toneko.common.mod.effects.ExcitingEffect

class KenjaTimeEffect: MobEffect(MobEffectCategory.BENEFICIAL, 0x3cfa3c)  {
    init{
        this.addAttributeModifier(
            Attributes.MOVEMENT_SPEED,
            ExcitingEffect.LOCATION,
            -0.15,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        )
    }
}