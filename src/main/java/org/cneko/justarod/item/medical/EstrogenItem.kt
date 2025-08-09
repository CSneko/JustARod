package org.cneko.justarod.item.medical

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.Hand
import org.cneko.justarod.entity.Pregnant

class EstrogenItem(settings: Settings): MedicalItem(settings) {
    override fun canApply(
        user: PlayerEntity,
        target: LivingEntity,
        stack: ItemStack,
        hand: Hand
    ): Boolean {
        return target is Pregnant
    }

    override fun getFailureMessage(
        user: PlayerEntity,
        target: LivingEntity,
        stack: ItemStack
    ): Text {
        return Text.empty()
    }

    override fun applyEffect(
        user: PlayerEntity,
        target: LivingEntity,
        stack: ItemStack,
        hand: Hand
    ) {
        target as Pregnant
        if (target.isFemale) {
            target.menstruation += 12000 // 调节月经是吗，不会喵！
        }
        if (target.random.nextBoolean()){
            // 稍微恶心
            target.addStatusEffect(StatusEffectInstance(StatusEffects.NAUSEA,120,0))
        }
        if (target.random.nextBoolean()){
            // 好痛
            target.damage(target.damageSources.mobAttack(user),0.5f)
        }
    }

    override fun consumeItem(
        user: PlayerEntity,
        target: LivingEntity,
        stack: ItemStack,
        hand: Hand
    ) {
        stack.decrement(1)
    }

    override fun getSuccessMessages(
        user: PlayerEntity,
        target: LivingEntity,
        stack: ItemStack
    ): ActionMessages {
        return ActionMessages(
            userSuccessMessage = Text.of(""),
            targetSuccessMessage = Text.of("")
        )
    }
}