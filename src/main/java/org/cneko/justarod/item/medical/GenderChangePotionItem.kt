package org.cneko.justarod.item.medical

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import org.cneko.justarod.entity.Pregnant
import kotlin.random.Random

class GenderChangePotionItem(properties: Properties, val gender: Gender): MedicalItem(properties) {
    override fun canApply(
        user: Player,
        target: LivingEntity,
        stack: ItemStack,
        hand: InteractionHand
    ): Boolean {
        // 不需要条件，任何目标都能使用
        return target is Pregnant
    }

    override fun getFailureMessage(
        user: Player,
        target: LivingEntity,
        stack: ItemStack
    ): Component? {
        return null
    }

    override fun applyEffect(
        user: Player,
        target: LivingEntity,
        stack: ItemStack,
        hand: InteractionHand
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
        user: Player,
        target: LivingEntity,
        stack: ItemStack,
        hand: InteractionHand
    ) {
        // 使用一次后减少数量
        stack.shrink(1)
    }

    override fun getSuccessMessages(
        user: Player,
        target: LivingEntity,
        stack: ItemStack
    ): ActionMessages {
        return ActionMessages(
            userSuccessMessage = Component.literal("你使用了变性药水，成功改变了 ${target.name.string} 的性别！"),
            targetSuccessMessage = Component.literal("你的性别被改变了！")
        )
    }

    enum class Gender {
        MALE,
        FEMALE
    }
}
