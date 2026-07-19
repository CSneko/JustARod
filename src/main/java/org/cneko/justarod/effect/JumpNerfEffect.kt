package org.cneko.justarod.effect

import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.resources.ResourceLocation
import org.cneko.justarod.JRUtil.Companion.rodId
import org.cneko.toneko.common.mod.effects.ExcitingEffect

class JumpNerfEffect: MobEffect(MobEffectCategory.HARMFUL, 0xe81845) {
    companion object{
        val LOCATION = rodId("jump_nerf")
    }
    init {
        this.addAttributeModifier(
            Attributes.JUMP_STRENGTH,
            LOCATION,
            -0.3,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        )
    }
}