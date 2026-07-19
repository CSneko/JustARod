package org.cneko.justarod.item.medical

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import org.cneko.justarod.entity.Pregnant
import org.cneko.justarod.item.JRItems
import org.cneko.justarod.item.rod.addEffect
import org.cneko.toneko.common.mod.util.TickTaskQueue

class AbortionPillItem(properties: Properties) : MedicalItem(properties) {

    private val LATE_TERM_PREGNANCY_TICKS = 20 * 60 * 20 * 7 // 7个Minecraft日

    override fun canApply(user: Player, target: LivingEntity, stack: ItemStack, hand: InteractionHand): Boolean {
        return target is Pregnant && target.isFemale() && target.pregnant > 0
    }

    override fun getFailureMessage(user: Player, target: LivingEntity, stack: ItemStack): Component {
        if (target !is Pregnant || !target.isFemale()) {
            return Component.literal("§c此物品只能对怀孕的女性玩家使用。")
        }
        if (target.pregnant <= 0) {
            val message = if (user == target) "§c你没有怀孕，不需要使用这个。" else "§c对方没有怀孕。"
            return Component.literal(message)
        }
        return Component.literal("§c现在无法使用。")
    }

    override fun applyEffect(user: Player, target: LivingEntity, stack: ItemStack, hand: InteractionHand) {
        target as Pregnant

        if (target.pregnant >= LATE_TERM_PREGNANCY_TICKS) {
            target.hurt(target.level().damageSources.generic(), 1f)
            target.sendSystemMessage(Component.literal("§c手术过程似乎比较顺利，但你仍然感到一阵剧痛。"))
        } else {
            val task = TickTaskQueue()
            for (i in 1..10) {
                task.addTask(20 * i) {
                    if (!target.isDead) {
                        target.hurt(target.level().damageSources.generic(), 2f)
                    }
                }
            }
            if (target.random.nextInt(5) == 0) {
                target.isSterilization = true
                target.addEffect(MobEffects.CONFUSION, 0, 20 * 15)
                if (user != target) {
                    user.sendSystemMessage(Component.literal("§e并发症发生了..."), false)
                }
                target.sendSystemMessage(Component.literal("§c并发症！对你造成了永久性损伤！"))
            }
        }

        target.pregnant = 0
        target.spawnAtLocation(JRItems.MOLE.getDefaultInstance)
    }

    override fun consumeItem(user: Player, target: LivingEntity, stack: ItemStack, hand: InteractionHand) {
        if (!user.abilities.isCreative()) {
            stack.shrink(1)
        }
    }

    override fun getSuccessMessages(user: Player, target: LivingEntity, stack: ItemStack): ActionMessages {
        val isSelf = user == target
        return ActionMessages(
            userSuccessMessage = if (isSelf) Component.literal("§e你服下了药丸，终止了妊娠...") else Component.literal("§e你帮助 ${target.displayName?.string} 终止了妊娠。"),
            targetSuccessMessage = if (isSelf) null else Component.literal("§c${user.displayName?.string} 给你服用了堕胎药，终止了你的妊娠！")
        )
    }
}