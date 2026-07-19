package org.cneko.justarod.item.medical

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.inventory.tooltip.TooltipData
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.resources.ResourceLocation
import org.cneko.justarod.entity.Pregnant
import org.cneko.justarod.item.tooltip.ChemicalStructureTooltipData
import java.util.Optional

class AntiAndrogenItem(properties: Properties): MedicalItem(properties) {
    override fun canApply(user: Player, target: LivingEntity, stack: ItemStack, hand: InteractionHand): Boolean {
        return target is Pregnant
    }

    override fun getFailureMessage(user: Player, target: LivingEntity, stack: ItemStack): Component {
        return Component.empty()
    }

    override fun applyEffect(user: Player, target: LivingEntity, stack: ItemStack, hand: InteractionHand) {
        target as Pregnant

        // 1. 激素机制：增加外源阻断剂浓度，压制原生睾酮分泌
        // 按照之前 SlowTick 每次衰减 0.02 的设定，增加 50.0f 大约可以维持压制效果一整个 Minecraft 天 (24000 tick)
        target.exoBlocker += 50.0f

        // 2. 药效生理反应
        if (target.random.nextBoolean()) {
            // 抗雄会导致肌肉无力、疲惫，以及雄性冲动消退
            target.addEffect(MobEffectInstance(MobEffects.WEAKNESS, 20 * 60, 0))
            target.sendSystemMessage(Component.literal("§7你感觉到体内的某种躁动正在消退，取而代之的是一阵虚弱感..."))
        }

        if (target.random.nextBoolean()) {
            // 联动排尿系统：现实中的抗雄药（螺内酯）是强效利尿剂
            // 瞬间增加极多的尿意（约 8 分钟的尿量积累）
            target.urination += 20 * 60 * 8
            target.sendSystemMessage(Component.literal("§e药效发作了，抗雄药的利尿副作用让你突然很想去洗手间..."))
        } else {
            target.sendSystemMessage(Component.literal("§b大脑深处的燥热感被压制了，身体变得异常平静..."))
        }
    }

    override fun consumeItem(user: Player, target: LivingEntity, stack: ItemStack, hand: InteractionHand) {
        if (!user.abilities.isCreative()) stack.shrink(1)
    }
    override fun getTooltipData(stack: ItemStack): Optional<TooltipData> {
        return Optional.of(
            ChemicalStructureTooltipData(ResourceLocation.fromNamespaceAndPath("justarod", "textures/tooltip/spironolacton.png"))
        )
    }


    override fun getSuccessMessages(user: Player, target: LivingEntity, stack: ItemStack): ActionMessages {
        val isSelf = user == target
        return ActionMessages(
            userSuccessMessage = if (isSelf) Component.literal("§a你服用了抗雄激素（阻断剂）。")
            else Component.literal("§a你给 ${target.displayName?.string} 摄入了抗雄激素。"),
            targetSuccessMessage = if (isSelf) null else Component.literal("§b${user.displayName?.string} 给你摄入了阻断剂，你感觉身上的力气正在流失...")
        )
    }
}