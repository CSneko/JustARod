package org.cneko.justarod.item.medical

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.item.UseAnim
import net.minecraft.world.level.Level
import org.cneko.justarod.item.JRComponents

class FrozenSpermRetrievalDeviceItem(properties: Properties) : SpermRetrievalDeviceItem(36000, properties) {

    // 食用时间加倍
    override fun getMaxUseTime(stack: ItemStack, user: LivingEntity): Int = 64

    // 食用逻辑（使用方效果）
    override fun finishUsing(stack: ItemStack, world: Level, user: LivingEntity): ItemStack {
        val result = super.finishUsing(stack, world, user)

        if (level() is ServerLevel) {
            applyFrozenEffectWithParticles(user, world)
        }

        return result
    }

    // 注入/采集精液时被使用方的冰冻效果
    override fun applyEffect(user: Player, target: LivingEntity, stack: ItemStack, hand: InteractionHand) {
        super.applyEffect(user, target, stack, hand)

        // 给使用方和被使用方添加冰冻效果及雪粒子
        if (user.level() is ServerLevel) {
            applyFrozenEffectWithParticles(user, user.level() as ServerLevel)
        }
        if (target.level() is ServerLevel) {
            applyFrozenEffectWithParticles(target, target.level() as ServerLevel)
        }
    }

    private fun applyFrozenEffectWithParticles(entity: LivingEntity, world: ServerLevel) {
        // 添加冰冻状态
        val iceEffect = MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 10, 1)
        val frostEffect = MobEffectInstance(MobEffects.DIG_SLOWDOWN, 20 * 10, 0)
        entity.addEffect(iceEffect)
        entity.addEffect(frostEffect)

        // 持续生成雪粒子（模拟雪地环绕）
        repeat(5) { // 重复5次，每次稍有随机偏移
            level().sendParticles(
                ParticleTypes.SNOWFLAKE,
                entity.x, entity.y + 0.5, entity.z,
                10, // 每次生成10个粒子
                0.5, 0.5, 0.5, // x, y, z 偏移量
                0.1 // 粒子速度
            )
        }
    }

    override fun getUseAction(stack: ItemStack): UseAnim = UseAnim.DRINK

    override fun use(world: Level, user: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val stack = user.getItemInHand(hand)
        // 只有有精液时才能饮用
        return if (stack.contains(JRComponents.ENTITY_TYPE)) {
            super.use(world, user, hand)
        } else {
            InteractionResultHolder.pass(stack)
        }
    }
}
