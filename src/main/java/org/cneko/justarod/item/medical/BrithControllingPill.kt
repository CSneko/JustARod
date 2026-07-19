package org.cneko.justarod.item.medical

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import org.cneko.justarod.entity.Pregnant
import org.cneko.justarod.item.rod.addEffect

private const val BIRTH_CONTROL_DURATION_TICKS = 20 * 60 * 20 // 1个MC天

class BrithControllingPill(properties: Properties) : MedicalItem(properties) {

    override fun canApply(user: Player, target: LivingEntity, stack: ItemStack, hand: InteractionHand): Boolean {
        return target is Pregnant && target.isFemale
    }

    override fun getFailureMessage(user: Player, target: LivingEntity, stack: ItemStack): Component {
        if (target !is Pregnant || !target.isFemale) {
            return Component.literal("§c此物品只能对女性体质使用。")
        }
        return Component.literal("§c无法使用。")
    }

    override fun applyEffect(user: Player, target: LivingEntity, stack: ItemStack, hand: InteractionHand) {
        target as Pregnant

        // 1. 激素机制：补充外源雌孕激素 (强制锁死黄体期，不排卵，长内膜)
        target.exoP += 15.0f  // 孕酮大幅增加，大于5就会被底层判定为黄体期
        target.exoE2 += 30.0f // 补充适量雌激素维持内膜

        // 2. 物理锁定机制
        val wasTaking = target.brithControlling > 0
        target.brithControlling = BIRTH_CONTROL_DURATION_TICKS

        // 3. 过量/重复服药的副作用 (激素超标导致恶心)
        if (wasTaking || target.exoP > 30.0f) {
            target.addEffect(MobEffects.CONFUSION, 20 * 15, 0)
            target.sendSystemMessage(Component.literal("§c短时间内重复服药导致体内激素紊乱，你感到一阵恶心。"))
        }

        // 4. 医学治疗：长效抑制雄激素，治疗多囊卵巢综合征 (PCOS)
        if (target.isPCOS && target.random.nextInt(3) == 0) {
            target.isPCOS = false
            target.sendSystemMessage(Component.literal("§a在药物调节下，多囊卵巢的症状得到了缓解！"))
        }
    }

    override fun consumeItem(user: Player, target: LivingEntity, stack: ItemStack, hand: InteractionHand) {
        if (!user.abilities.isCreative()) {
            stack.shrink(1)
        }
    }

    override fun getSuccessMessages(user: Player, target: LivingEntity, stack: ItemStack): ActionMessages {
        val isSelf = user == target
        return ActionMessages(
            userSuccessMessage = if (isSelf) Component.literal("§a你服下了一粒避孕药。(停药将导致撤退性出血)")
            else Component.literal("§a你给 ${target.displayName?.string} 服用了一粒避孕药。"),
            targetSuccessMessage = if (isSelf) null else Component.literal("§e${user.displayName?.string} 给你服用了一粒避孕药。")
        )
    }
}