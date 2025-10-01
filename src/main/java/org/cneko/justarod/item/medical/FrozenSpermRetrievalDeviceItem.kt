package org.cneko.justarod.item.medical

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundEvents
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.util.UseAction
import net.minecraft.world.World
import org.cneko.justarod.item.JRComponents

class FrozenSpermRetrievalDeviceItem(settings: Settings) : SpermRetrievalDeviceItem(36000, settings) {

    // 食用时间加倍
    override fun getMaxUseTime(stack: ItemStack, user: LivingEntity): Int = 64

    // 食用逻辑（使用方效果）
    override fun finishUsing(stack: ItemStack, world: World, user: LivingEntity): ItemStack {
        val result = super.finishUsing(stack, world, user)

        if (world is ServerWorld) {
            applyFrozenEffectWithParticles(user, world)
        }

        return result
    }

    // 注入/采集精液时被使用方的冰冻效果
    override fun applyEffect(user: PlayerEntity, target: LivingEntity, stack: ItemStack, hand: Hand) {
        super.applyEffect(user, target, stack, hand)

        // 给使用方和被使用方添加冰冻效果及雪粒子
        if (user.world is ServerWorld) {
            applyFrozenEffectWithParticles(user, user.world as ServerWorld)
        }
        if (target.world is ServerWorld) {
            applyFrozenEffectWithParticles(target, target.world as ServerWorld)
        }
    }

    private fun applyFrozenEffectWithParticles(entity: LivingEntity, world: ServerWorld) {
        // 添加冰冻状态
        val iceEffect = StatusEffectInstance(StatusEffects.SLOWNESS, 20 * 10, 1)
        val frostEffect = StatusEffectInstance(StatusEffects.MINING_FATIGUE, 20 * 10, 0)
        entity.addStatusEffect(iceEffect)
        entity.addStatusEffect(frostEffect)

        // 持续生成雪粒子（模拟雪地环绕）
        repeat(5) { // 重复5次，每次稍有随机偏移
            world.spawnParticles(
                ParticleTypes.SNOWFLAKE,
                entity.x, entity.y + 0.5, entity.z,
                10, // 每次生成10个粒子
                0.5, 0.5, 0.5, // x, y, z 偏移量
                0.1 // 粒子速度
            )
        }
    }

    override fun getUseAction(stack: ItemStack): UseAction = UseAction.DRINK

    override fun use(world: World, user: PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
        val stack = user.getStackInHand(hand)
        // 只有有精液时才能饮用
        return if (stack.contains(JRComponents.ENTITY_TYPE)) {
            super.use(world, user, hand)
        } else {
            TypedActionResult.pass(stack)
        }
    }
}
