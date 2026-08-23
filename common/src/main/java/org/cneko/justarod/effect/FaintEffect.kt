package org.cneko.justarod.effect

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Pose
import org.cneko.toneko.common.mod.api.EntityPoseManager
import org.cneko.toneko.common.mod.effects.ExcitingEffect
import org.cneko.toneko.common.mod.packets.EntityPosePayload

// 虽然但是，晕乎乎的还是不舒服对吧
class FaintEffect: MobEffect(MobEffectCategory.BENEFICIAL, 0x3c3c3c)  {
    init {
        this.addAttributeModifier(
            Attributes.MOVEMENT_SPEED,
            ExcitingEffect.LOCATION,
            -2.0,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        )
        this.addAttributeModifier(
            Attributes.ATTACK_DAMAGE,
            ExcitingEffect.LOCATION,
            -2.0,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        )
        this.addAttributeModifier(
            Attributes.ATTACK_SPEED,
            ExcitingEffect.LOCATION,
            -2.0,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        )
        this.addAttributeModifier(
            Attributes.JUMP_STRENGTH,
            ExcitingEffect.LOCATION,
            -2.0,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        )
    }

    override fun shouldApplyEffectTickThisTick(duration: Int, amplifier: Int): Boolean {
        return true
    }

    override fun applyEffectTick(entity: LivingEntity?, amplifier: Int): Boolean {
        EntityPoseManager.setPose(entity, Pose.SLEEPING)
        return super.applyEffectTick(entity, amplifier)
    }
}