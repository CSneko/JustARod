package org.cneko.justarod.effect

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import org.cneko.justarod.JRUtil.Companion.rodId

class SmearyEffect : MobEffect(
    MobEffectCategory.HARMFUL, // 类型为有害
    0xD9D95B // 颜色：淡黄/浑浊色，符合“弄脏/尿液”的感觉
) {

    init {
        this.addAttributeModifier(
            Attributes.MOVEMENT_SPEED,
            rodId("smeary"),
            -0.15, // 速度减少 15% (0级时)，随等级提升
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        )
    }

    // 允许每 tick 运行逻辑 (为了粒子效果和清洗检测)
    override fun shouldApplyEffectTickThisTick(duration: Int, amplifier: Int): Boolean {
        return true
    }

    override fun applyEffectTick(entity: LivingEntity, amplifier: Int): Boolean {
        val world = entity.level()
        val random = entity.random

        // 1. 粒子效果逻辑
        // 生成类似海绵吸水后的滴水效果，或者湿身的效果
        // 频率：约每 2-3 tick 生成一次，看起来像是在不断滴落
        if (world.isClientSide) {
            if (random.nextInt(3) == 0) {
                val width = entity.bbWidth
                val height = entity.bbHeight

                // 随机在实体的碰撞箱范围内生成粒子
                val x = entity.x + (random.nextDouble() - 0.5) * width
                // 高度随机，模拟从身上流下
                val y = entity.y + random.nextDouble() * height * 0.8 + 0.2
                val z = entity.z + (random.nextDouble() - 0.5) * width

                world.addParticle(
                    ParticleTypes.DRIPPING_WATER, // 滴水粒子
                    x, y, z,
                    0.0, 0.0, 0.0 // 速度
                )
            }

            // 如果移动速度较快，偶尔生成溅射粒子
            if (entity.getDeltaMovement().lengthSqr() > 0.01 && random.nextInt(10) == 0) {
                world.addParticle(
                    ParticleTypes.SPLASH,
                    entity.x, entity.y, entity.z,
                    0.0, 0.1, 0.0
                )
            }
        }

        // 2. 清洗机制 (补全逻辑)
        // 如果实体在水中（isTouchingWater）或者在雨中（isWet），效果应该更快消失
        if (!world.isClientSide) {
            // 如果完全浸泡在水中（洗澡）
            if (entity.isUnderWater) {

                if (random.nextInt(40) == 0) { // 约每2秒判定一次
                    entity.removeEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(this))
                }
            }
        }

        return super.applyEffectTick(entity, amplifier)
    }
}