package org.cneko.justarod.item.medical

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.tooltip.TooltipData
import net.minecraft.text.Text
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import org.cneko.justarod.entity.Pregnant
import org.cneko.justarod.item.tooltip.ChemicalStructureTooltipData
import java.util.Optional

class AntiAndrogenItem(settings: Settings): MedicalItem(settings) {
    override fun canApply(user: PlayerEntity, target: LivingEntity, stack: ItemStack, hand: Hand): Boolean {
        return target is Pregnant
    }

    override fun getFailureMessage(user: PlayerEntity, target: LivingEntity, stack: ItemStack): Text {
        return Text.empty()
    }

    override fun applyEffect(user: PlayerEntity, target: LivingEntity, stack: ItemStack, hand: Hand) {
        target as Pregnant

        // 1. 激素机制：增加外源阻断剂浓度，压制原生睾酮分泌
        // 按照之前 SlowTick 每次衰减 0.02 的设定，增加 50.0f 大约可以维持压制效果一整个 Minecraft 天 (24000 tick)
        target.exoBlocker += 50.0f

        // 2. 药效生理反应
        if (target.random.nextBoolean()) {
            // 抗雄会导致肌肉无力、疲惫，以及雄性冲动消退
            target.addStatusEffect(StatusEffectInstance(StatusEffects.WEAKNESS, 20 * 60, 0))
            target.sendMessage(Text.of("§7你感觉到体内的某种躁动正在消退，取而代之的是一阵虚弱感..."))
        }

        if (target.random.nextBoolean()) {
            // 联动排尿系统：现实中的抗雄药（螺内酯）是强效利尿剂
            // 瞬间增加极多的尿意（约 8 分钟的尿量积累）
            target.urination += 20 * 60 * 8
            target.sendMessage(Text.of("§e药效发作了，抗雄药的利尿副作用让你突然很想去洗手间..."))
        } else {
            target.sendMessage(Text.of("§b大脑深处的燥热感被压制了，身体变得异常平静..."))
        }
    }

    override fun consumeItem(user: PlayerEntity, target: LivingEntity, stack: ItemStack, hand: Hand) {
        if (!user.abilities.creativeMode) stack.decrement(1)
    }
    override fun getTooltipData(stack: ItemStack): Optional<TooltipData> {
        return Optional.of(
            ChemicalStructureTooltipData(Identifier.of("justarod", "textures/tooltip/spironolacton.png"))
        )
    }


    override fun getSuccessMessages(user: PlayerEntity, target: LivingEntity, stack: ItemStack): ActionMessages {
        val isSelf = user == target
        return ActionMessages(
            userSuccessMessage = if (isSelf) Text.of("§a你服用了抗雄激素（阻断剂）。")
            else Text.of("§a你给 ${target.displayName?.string} 摄入了抗雄激素。"),
            targetSuccessMessage = if (isSelf) null else Text.of("§b${user.displayName?.string} 给你摄入了阻断剂，你感觉身上的力气正在流失...")
        )
    }
}