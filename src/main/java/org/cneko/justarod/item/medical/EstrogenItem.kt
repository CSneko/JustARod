package org.cneko.justarod.item.medical

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.tooltip.TooltipData
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.text.Text
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import org.cneko.justarod.entity.Pregnant
import org.cneko.justarod.item.tooltip.ChemicalStructureTooltipData
import java.util.Optional

class EstrogenItem(settings: Settings): MedicalItem(settings) {
    override fun canApply(user: PlayerEntity, target: LivingEntity, stack: ItemStack, hand: Hand): Boolean {
        return target is Pregnant
    }

    override fun getFailureMessage(user: PlayerEntity, target: LivingEntity, stack: ItemStack): Text {
        return Text.empty()
    }

    override fun applyEffect(user: PlayerEntity, target: LivingEntity, stack: ItemStack, hand: Hand) {
        target as Pregnant

        // 1. 激素机制：直接注入大量外源雌二醇
        target.exoE2 += 150.0f

        // 2. 药效生理反应
        if (target.random.nextBoolean()) {
            // 激素冲击带来的晕眩/恶心
            target.addStatusEffect(StatusEffectInstance(StatusEffects.NAUSEA, 120, 0))
        }
        if (target.random.nextBoolean()) {
            // 胸部发育/胀痛感
            target.damage(target.damageSources.magic(), 1.0f)
            target.sendMessage(Text.of("§d你感觉胸口传来一阵胀痛，身体变得异常敏感..."))
        } else {
            target.sendMessage(Text.of("§d一股温热的感觉流遍全身..."))
        }

    }

    override fun getTooltipData(stack: ItemStack): Optional<TooltipData> {
        return Optional.of(
            ChemicalStructureTooltipData(Identifier.of("justarod", "textures/tooltip/estradiol.png"))
        )
    }


    override fun consumeItem(user: PlayerEntity, target: LivingEntity, stack: ItemStack, hand: Hand) {
        if (!user.abilities.creativeMode) stack.decrement(1)
    }

    override fun getSuccessMessages(user: PlayerEntity, target: LivingEntity, stack: ItemStack): ActionMessages {
        val isSelf = user == target
        return ActionMessages(
            userSuccessMessage = if (isSelf) Text.of("§a你使用了雌激素制剂。")
            else Text.of("§a你给 ${target.displayName?.string} 摄入了雌激素。"),
            targetSuccessMessage = if (isSelf) null else Text.of("§d${user.displayName?.string} 给你摄入了雌激素，你感觉体温在升高...")
        )
    }
}