package org.cneko.justarod.item.medical

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.Hand
import org.cneko.justarod.entity.Pregnant
import org.cneko.justarod.item.JRItems
import org.cneko.justarod.item.rod.addEffect
import org.cneko.toneko.common.mod.util.TickTaskQueue

class AbortionPillItem(settings: Settings) : MedicalItem(settings) {

    private val LATE_TERM_PREGNANCY_TICKS = 20 * 60 * 20 * 7 // 7个Minecraft日

    override fun canApply(user: PlayerEntity, target: LivingEntity, stack: ItemStack, hand: Hand): Boolean {
        return target is Pregnant && target.isFemale() && target.pregnant > 0
    }

    override fun getFailureMessage(user: PlayerEntity, target: LivingEntity, stack: ItemStack): Text {
        if (target !is Pregnant || !target.isFemale()) {
            return Text.of("§c此物品只能对怀孕的女性玩家使用。")
        }
        if (target.pregnant <= 0) {
            val message = if (user == target) "§c你没有怀孕，不需要使用这个。" else "§c对方没有怀孕。"
            return Text.of(message)
        }
        return Text.of("§c现在无法使用。")
    }

    override fun applyEffect(user: PlayerEntity, target: LivingEntity, stack: ItemStack, hand: Hand) {
        target as Pregnant

        if (target.pregnant >= LATE_TERM_PREGNANCY_TICKS) {
            target.damage(target.world.damageSources.generic(), 1f)
            target.sendMessage(Text.of("§c手术过程似乎比较顺利，但你仍然感到一阵剧痛。"))
        } else {
            val task = TickTaskQueue()
            for (i in 1..10) {
                task.addTask(20 * i) {
                    if (!target.isDead) {
                        target.damage(target.world.damageSources.generic(), 2f)
                    }
                }
            }
            if (target.random.nextInt(5) == 0) {
                target.isSterilization = true
                target.addEffect(StatusEffects.NAUSEA, 0, 20 * 15)
                if (user != target) {
                    user.sendMessage(Text.of("§e并发症发生了..."), false)
                }
                target.sendMessage(Text.of("§c并发症！对你造成了永久性损伤！"))
            }
        }

        target.pregnant = 0
        target.dropStack(JRItems.MOLE.defaultStack)
    }

    override fun consumeItem(user: PlayerEntity, target: LivingEntity, stack: ItemStack, hand: Hand) {
        if (!user.abilities.creativeMode) {
            stack.decrement(1)
        }
    }

    override fun getSuccessMessages(user: PlayerEntity, target: LivingEntity, stack: ItemStack): ActionMessages {
        val isSelf = user == target
        return ActionMessages(
            userSuccessMessage = if (isSelf) Text.of("§e你服下了药丸，终止了妊娠...") else Text.of("§e你帮助 ${target.displayName?.string} 终止了妊娠。"),
            targetSuccessMessage = if (isSelf) null else Text.of("§c${user.displayName?.string} 给你服用了堕胎药，终止了你的妊娠！")
        )
    }
}