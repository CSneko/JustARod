package org.cneko.justarod.item.medical

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.inventory.tooltip.TooltipData
import net.minecraft.world.item.TooltipFlag
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.resources.ResourceLocation
import org.cneko.justarod.entity.Pregnant
import org.cneko.justarod.item.tooltip.ChemicalStructureTooltipData
import java.util.Optional

class EstrogenItem(properties: Properties): MedicalItem(properties) {
    override fun canApply(user: Player, target: LivingEntity, stack: ItemStack, hand: InteractionHand): Boolean {
        return target is Pregnant
    }

    override fun getFailureMessage(user: Player, target: LivingEntity, stack: ItemStack): Component {
        return Component.empty()
    }

    override fun applyEffect(user: Player, target: LivingEntity, stack: ItemStack, hand: InteractionHand) {
        target as Pregnant

        // 1. 激素机制：直接注入大量外源雌二醇
        target.exoE2 += 150.0f

        // 2. 药效生理反应
        if (target.random.nextBoolean()) {
            // 激素冲击带来的晕眩/恶心
            target.addEffect(MobEffectInstance(MobEffects.CONFUSION, 120, 0))
        }
        if (target.random.nextBoolean()) {
            // 胸部发育/胀痛感
            target.hurt(target.damageSources.magic(), 1.0f)
            target.sendSystemMessage(Component.literal("§d你感觉胸口传来一阵胀痛，身体变得异常敏感..."))
        } else {
            target.sendSystemMessage(Component.literal("§d一股温热的感觉流遍全身..."))
        }

    }

    override fun getTooltipData(stack: ItemStack): Optional<TooltipData> {
        return Optional.of(
            ChemicalStructureTooltipData(ResourceLocation.fromNamespaceAndPath("justarod", "textures/tooltip/estradiol.png"))
        )
    }


    override fun consumeItem(user: Player, target: LivingEntity, stack: ItemStack, hand: InteractionHand) {
        if (!user.abilities.isCreative()) stack.shrink(1)
    }

    override fun getSuccessMessages(user: Player, target: LivingEntity, stack: ItemStack): ActionMessages {
        val isSelf = user == target
        return ActionMessages(
            userSuccessMessage = if (isSelf) Component.literal("§a你使用了雌激素制剂。")
            else Component.literal("§a你给 ${target.displayName?.string} 摄入了雌激素。"),
            targetSuccessMessage = if (isSelf) null else Component.literal("§d${user.displayName?.string} 给你摄入了雌激素，你感觉体温在升高...")
        )
    }
}