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

class TestosteroneItem(properties: Properties): MedicalItem(properties) {
    override fun canApply(user: Player, target: LivingEntity, stack: ItemStack, hand: InteractionHand): Boolean {
        return target is Pregnant
    }

    override fun getFailureMessage(user: Player, target: LivingEntity, stack: ItemStack): Component {
        return Component.empty()
    }

    override fun applyEffect(user: Player, target: LivingEntity, stack: ItemStack, hand: InteractionHand) {
        target as Pregnant

        // 1. 激素机制：直接注入大量外源睾酮
        target.exoT += 200.0f

        // 2. 药效生理反应 (类固醇狂热)
        // 瞬间获得少量力量增益，呼应激素系统长期的 +攻击力 Buff
        target.addEffect(MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 30, 0))

        if (target.random.nextBoolean()) {
            // 雄性激素导致代谢加快，容易饥饿
            target.addEffect(MobEffectInstance(MobEffects.HUNGER, 20 * 60, 0))
            target.sendSystemMessage(Component.literal("§c一股暴躁的力量在体内横冲直撞，你感觉异常亢奋！"))
        } else {
            target.sendSystemMessage(Component.literal("§6肌肉微微发热，你感觉充满力量。"))
        }

    }

    override fun getTooltipData(stack: ItemStack): Optional<TooltipData> {
        return Optional.of(
            ChemicalStructureTooltipData(ResourceLocation.fromNamespaceAndPath("justarod", "textures/tooltip/testosterone.png"))
        )
    }

    override fun consumeItem(user: Player, target: LivingEntity, stack: ItemStack, hand: InteractionHand) {
        if (!user.abilities.isCreative()) stack.shrink(1)
    }

    override fun getSuccessMessages(user: Player, target: LivingEntity, stack: ItemStack): ActionMessages {
        val isSelf = user == target
        return ActionMessages(
            userSuccessMessage = if (isSelf) Component.literal("§a你使用了雄激素制剂。")
            else Component.literal("§a你给 ${target.displayName?.string} 摄入了雄激素。"),
            targetSuccessMessage = if (isSelf) null else Component.literal("§6${user.displayName?.string} 给你摄入了雄激素，你感到烦躁且充满力量...")
        )
    }
}