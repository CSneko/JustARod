package org.cneko.justarod.item.medical

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.Hand
import org.cneko.justarod.entity.Pregnant
import kotlin.random.Random

class GenderChangePotionItem(settings: Settings, val gender: Gender): MedicalItem(settings) {
    override fun canApply(
        user: PlayerEntity,
        target: LivingEntity,
        stack: ItemStack,
        hand: Hand
    ): Boolean {
        // 不需要条件，任何目标都能使用
        return target is Pregnant
    }

    override fun getFailureMessage(
        user: PlayerEntity,
        target: LivingEntity,
        stack: ItemStack
    ): Text? {
        return null
    }

    override fun applyEffect(
        user: PlayerEntity,
        target: LivingEntity,
        stack: ItemStack,
        hand: Hand
    ) {
        if (target is Pregnant) {
            // 20% 概率出现性别异常
            if (Random.nextDouble() < 0.2) {
                if (Random.nextBoolean()) {
                    // 同时为 male 和 female
                    target.forceToMaleAndFemale()
                } else {
                    // 同时不是 male 和 female
                    target.forceToNoSex()
                }
            } else {
                when (gender) {
                    Gender.MALE -> {
                        target.forceToMale()
                    }
                    Gender.FEMALE -> {
                        target.forceToFemale()
                    }
                }
            }
        }
    }

    override fun consumeItem(
        user: PlayerEntity,
        target: LivingEntity,
        stack: ItemStack,
        hand: Hand
    ) {
        // 使用一次后减少数量
        stack.decrement(1)
    }

    override fun getSuccessMessages(
        user: PlayerEntity,
        target: LivingEntity,
        stack: ItemStack
    ): ActionMessages {
        return ActionMessages(
            userSuccessMessage = Text.literal("你使用了变性药水，成功改变了 ${target.name.string} 的性别！"),
            targetSuccessMessage = Text.literal("你的性别被改变了！")
        )
    }

    enum class Gender {
        MALE,
        FEMALE
    }
}
