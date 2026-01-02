package org.cneko.justarod.item.medical

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.Hand
import org.cneko.justarod.effect.JREffects
import org.cneko.justarod.entity.Pregnant
import kotlin.math.max

class TamsulosinCapsuleItem(settings: Settings) : MedicalItem(settings) {

    /**
     * 判断是否可以使用药物
     * 只有当目标患有前列腺炎、尿道炎，或者有积尿时才允许使用，避免浪费
     */
    override fun canApply(user: PlayerEntity, target: LivingEntity, stack: ItemStack, hand: Hand): Boolean {
        if (target !is Pregnant) return false

        // 有前列腺炎 OR 有尿意 OR 有尿道炎 都可以吃
        return target.prostatitis > 0 || target.urination > 0 || target.urethritis > 0
    }

    override fun getFailureMessage(user: PlayerEntity, target: LivingEntity, stack: ItemStack): Text? {
        return if (user == target) {
            Text.of("§7你感觉身体状况良好，不需要服用坦索罗辛。")
        } else {
            Text.of("§7目标看起来不需要这种药物。")
        }
    }

    override fun applyEffect(user: PlayerEntity, target: LivingEntity, stack: ItemStack, hand: Hand) {
        if (target is Pregnant) {
            // 1. 治疗前列腺炎 (大幅减轻)
            // 减少 3天 的病程
            if (target.prostatitis > 0) {
                target.removeStatusEffect(Registries.STATUS_EFFECT.getEntry(JREffects.PROSTATITIS_EFFECT))
                target.cureProstatitis(20 * 60 * 20 * 3)
            }

            // 2. 药理作用：松弛平滑肌，帮助排尿
            // 如果玩家有尿意，吃下药后会感到放松，尿意瞬间清空
            if (target.urination > 0) {
                target.urination = 0
            }

            // 3. 辅助治疗尿道炎 (抗炎作用)
            // 稍微缓解一下尿道炎 (减少1天)，防止前列腺炎立刻复发
            if (target.urethritis > 0) {
                target.removeStatusEffect(Registries.STATUS_EFFECT.getEntry(JREffects.URETHRITIS_EFFECT))
                val newUrethritis = max(0, target.urethritis - 20 * 60 * 20)
                target.urethritis = newUrethritis
            }

            // 4. 移除痛苦带来的 Debuff
            target.removeStatusEffect(StatusEffects.MINING_FATIGUE)
            target.removeStatusEffect(StatusEffects.WEAKNESS)
        }
    }

    override fun consumeItem(user: PlayerEntity, target: LivingEntity, stack: ItemStack, hand: Hand) {
        if (!user.isCreative) {
            stack.decrement(1)
        }
    }

    override fun getSuccessMessages(user: PlayerEntity, target: LivingEntity, stack: ItemStack): ActionMessages {
        // 构建反馈消息
        val isSelf = user == target

        val userMsg = if (isSelf) {
            Text.of("§a你吞下了坦索罗辛胶囊。")
        } else {
            Text.of("§a你给${target.name.string}喂下了坦索罗辛胶囊。")
        }

        val targetMsg = if (isSelf) {
            null // 自己已经在 userMsg 里发过了
        } else {
            Text.of("§a你被喂下了坦索罗辛胶囊。")
        }

        // 额外的效果描述 (Flavor Text)
        val effectMsg = Text.of("§a随着药效发挥，下半身的肌肉放松了，积攒的尿液顺畅地排了出来，疼痛也减轻了...")

        return ActionMessages(
            userSuccessMessage = userMsg,
            targetSuccessMessage = targetMsg,
            userExtraMessage = if (isSelf) effectMsg else null,
            targetExtraMessage = if (!isSelf) effectMsg else null
        )
    }
}