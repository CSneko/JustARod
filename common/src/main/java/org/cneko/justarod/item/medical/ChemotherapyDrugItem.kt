package org.cneko.justarod.item.medical

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import org.cneko.justarod.config.JRConfig
import org.cneko.justarod.entity.Pregnant
import kotlin.math.max

/**
 * 化疗药物 (Chemotherapy Drug)
 * 用于治疗卵巢癌的疗程制药物，同时辅助抑制乳腺癌。
 *
 * 药理设定：
 * - 每粒为一个疗程，可削减卵巢癌 3 天病程（天数可配置）/ 抑制乳腺癌 1 天病程
 * - 化疗是"杀敌一千自损八百"的疗法：服药后会有明显的副作用
 *   （恶心、虚弱、少量掉血、脱发），癌症越到晚期越需要多个疗程
 */
class ChemotherapyDrugItem(properties: Properties) : MedicalItem(properties) {

    companion object {
        // 辅助抑制乳腺癌 = 1个Minecraft天 病程
        const val BREAST_CANCER_SUPPRESS = 20 * 60 * 20
    }

    // 一个疗程可削减的卵巢癌病程（天数可配置）
    private val ovarianCancerCourse: Int
        get() = 20 * 60 * 20 * JRConfig.getChemoReductionDays()

    override fun canApply(user: Player, target: LivingEntity, stack: ItemStack, hand: InteractionHand): Boolean {
        if (target !is Pregnant) return false
        // 患有卵巢癌或乳腺癌时才能使用，避免浪费
        return target.ovarianCancer > 0 || target.breastCancer > 0
    }

    override fun getFailureMessage(user: Player, target: LivingEntity, stack: ItemStack): Component? {
        return if (user == target) {
            Component.literal("§7你目前没有需要化疗的肿瘤，是药三分毒，别乱吃。")
        } else {
            Component.literal("§7目标看起来没有患癌，不需要化疗。")
        }
    }

    override fun applyEffect(user: Player, target: LivingEntity, stack: ItemStack, hand: InteractionHand) {
        if (target !is Pregnant) return

        var curedOvarianCancer = false

        // ---------------- 1. 主要疗效：卵巢癌化疗 ----------------
        if (target.ovarianCancer > 0) {
            val newCourse = max(0, target.ovarianCancer - ovarianCancerCourse)
            target.ovarianCancer = newCourse
            if (newCourse <= 0) {
                curedOvarianCancer = true
            }
        }

        // ---------------- 2. 辅助疗效：抑制乳腺癌 ----------------
        if (target.breastCancer > 0) {
            target.breastCancer = max(0, target.breastCancer - BREAST_CANCER_SUPPRESS)
        }

        // ---------------- 3. 化疗副作用 (杀敌一千自损八百) ----------------
        // 细胞毒性药物不分敌我，正常细胞也遭殃
        target.hurt(target.level().damageSources().magic(), 2.0f)
        target.addEffect(MobEffectInstance(MobEffects.NAUSEA, 20 * 30, 1))   // 剧烈恶心呕吐
        target.addEffect(MobEffectInstance(MobEffects.WEAKNESS, 20 * 60, 1))    // 免疫力下降
        target.addEffect(MobEffectInstance(MobEffects.HUNGER, 20 * 30, 0))      // 食欲不振
        // "脱发"
        (target as? net.minecraft.world.entity.player.Player)?.sendSystemMessage(Component.literal("§7你感觉头皮传来一阵刺痒，几缕头发悄然飘落..."))

        // ---------------- 4. 治愈反馈 ----------------
        if (curedOvarianCancer) {
            (target as? net.minecraft.world.entity.player.Player)?.sendSystemMessage(Component.literal("§a最后一个疗程结束，影像检查显示卵巢上的肿瘤完全消失了！"))
            target.addEffect(MobEffectInstance(MobEffects.REGENERATION, 20 * 10, 1))
        }
    }

    override fun consumeItem(user: Player, target: LivingEntity, stack: ItemStack, hand: InteractionHand) {
        if (!user.isCreative) {
            stack.shrink(1)
        }
    }

    override fun getSuccessMessages(user: Player, target: LivingEntity, stack: ItemStack): ActionMessages? {
        // 核心反馈已在 applyEffect 中通过 sendSystemMessage 发送（包含疗程/治愈/副作用信息），此处不再重复
        return null
    }
}
