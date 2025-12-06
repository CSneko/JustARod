package org.cneko.justarod.effect

import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectCategory
import net.minecraft.util.Identifier
import org.cneko.justarod.JRUtil.Companion.rodId
import org.cneko.toneko.common.mod.effects.ExcitingEffect

class JumpNerfEffect: StatusEffect(StatusEffectCategory.HARMFUL, 0xe81845) {
    companion object{
        val LOCATION = rodId("jump_nerf")
    }
    init {
        this.addAttributeModifier(
            EntityAttributes.GENERIC_JUMP_STRENGTH,
            LOCATION,
            -0.3,
            EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE
        )
    }
}