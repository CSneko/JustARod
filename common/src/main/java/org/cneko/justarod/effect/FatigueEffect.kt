package org.cneko.justarod.effect

import net.minecraft.resources.Identifier
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import org.cneko.justarod.Justarod.MODID

// 熬夜的代价……黑眼圈、注意力涣散、浑身没劲喵
class FatigueEffect : MobEffect(MobEffectCategory.HARMFUL, 0x4b3869) {
    companion object {
        val FATIGUE_MOVEMENT_SPEED_LOCATION: Identifier =
            Identifier.fromNamespaceAndPath(MODID, "fatigue.movement_speed")
        val FATIGUE_ATTACK_DAMAGE_LOCATION: Identifier =
            Identifier.fromNamespaceAndPath(MODID, "fatigue.attack_damage")
        val FATIGUE_ATTACK_SPEED_LOCATION: Identifier =
            Identifier.fromNamespaceAndPath(MODID, "fatigue.attack_speed")
    }

    init {
        // 每一级（疲劳阶段）都会加深的减益：轻度几乎无感，极限时寸步难行
        this.addAttributeModifier(
            Attributes.MOVEMENT_SPEED,
            FATIGUE_MOVEMENT_SPEED_LOCATION,
            -0.04,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        )
        this.addAttributeModifier(
            Attributes.ATTACK_DAMAGE,
            FATIGUE_ATTACK_DAMAGE_LOCATION,
            -0.06,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        )
        this.addAttributeModifier(
            Attributes.ATTACK_SPEED,
            FATIGUE_ATTACK_SPEED_LOCATION,
            -0.06,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        )
    }
}
