package org.cneko.justarod.item.medical

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.Hand
import org.cneko.justarod.entity.Pregnant

class TestosteroneItem(settings: Settings): MedicalItem(settings) {
    override fun canApply(user: PlayerEntity, target: LivingEntity, stack: ItemStack, hand: Hand): Boolean {
        return target is Pregnant
    }

    override fun getFailureMessage(user: PlayerEntity, target: LivingEntity, stack: ItemStack): Text {
        return Text.empty()
    }

    override fun applyEffect(user: PlayerEntity, target: LivingEntity, stack: ItemStack, hand: Hand) {
        target as Pregnant

        // 1. 激素机制：直接注入大量外源睾酮
        target.exoT += 200.0f

        // 2. 药效生理反应 (类固醇狂热)
        // 瞬间获得少量力量增益，呼应激素系统长期的 +攻击力 Buff
        target.addStatusEffect(StatusEffectInstance(StatusEffects.STRENGTH, 20 * 30, 0))

        if (target.random.nextBoolean()) {
            // 雄性激素导致代谢加快，容易饥饿
            target.addStatusEffect(StatusEffectInstance(StatusEffects.HUNGER, 20 * 60, 0))
            target.sendMessage(Text.of("§c一股暴躁的力量在体内横冲直撞，你感觉异常亢奋！"))
        } else {
            target.sendMessage(Text.of("§6肌肉微微发热，你感觉充满力量。"))
        }

    }

    override fun consumeItem(user: PlayerEntity, target: LivingEntity, stack: ItemStack, hand: Hand) {
        if (!user.abilities.creativeMode) stack.decrement(1)
    }

    override fun getSuccessMessages(user: PlayerEntity, target: LivingEntity, stack: ItemStack): ActionMessages {
        val isSelf = user == target
        return ActionMessages(
            userSuccessMessage = if (isSelf) Text.of("§a你使用了雄激素制剂。")
            else Text.of("§a你给 ${target.displayName?.string} 摄入了雄激素。"),
            targetSuccessMessage = if (isSelf) null else Text.of("§6${user.displayName?.string} 给你摄入了雄激素，你感到烦躁且充满力量...")
        )
    }
}