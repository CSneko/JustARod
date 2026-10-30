package org.cneko.justarod.effect

import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.world.entity.LivingEntity
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * 注入式末地烛喷入水/牛奶后的「印记」。
 * 减速由注入时一并施加，这里只负责图标、持续时间和屁股上的粒子。
 *
 * @param milk 牛奶用白色粒子，水用原版水花
 */
class InjectedFluidEffect(private val milk: Boolean) :
    MobEffect(MobEffectCategory.HARMFUL, if (milk) 0xFFFFFF else 0x3FA9F5) {

    private val particle: ParticleOptions = if (milk) ParticleTypes.SNOWFLAKE else ParticleTypes.SPLASH

    override fun shouldApplyEffectTickThisTick(duration: Int, amplifier: Int): Boolean = duration % 2 == 0

    override fun applyEffectTick(world: ServerLevel, entity: LivingEntity, amplifier: Int): Boolean {
        val yaw = Math.toRadians(entity.yRot.toDouble())
        // 屁股：脚上方约 0.55 格、朝向的反方向稍微偏后
        val x = entity.x + sin(yaw) * 0.18
        val y = entity.y + 0.55
        val z = entity.z - cos(yaw) * 0.18
        world.sendParticles(particle, x, y, z, 2, 0.08, 0.04, 0.08, 0.0)
        return true
    }
}

/**
 * 岩浆：和原来「点燃 15 秒」的效果一致，只是换成了图标。
 * 每 tick 把火焰补满，让原版按 20 tick 一次结算烧伤；跳进水里立刻熄火并移除。
 */
class InjectedLavaEffect : MobEffect(MobEffectCategory.HARMFUL, 0xFF4A00) {

    override fun shouldApplyEffectTickThisTick(duration: Int, amplifier: Int): Boolean = true

    override fun onEffectAdded(entity: LivingEntity, amplifier: Int) {
        super.onEffectAdded(entity, amplifier)
        entity.remainingFireTicks = max(entity.remainingFireTicks, BURN_TICKS)
    }

    override fun applyEffectTick(world: ServerLevel, entity: LivingEntity, amplifier: Int): Boolean {
        if (entity.isInWater) {
            entity.remainingFireTicks = 0
            entity.removeEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(this))
            return false
        }
        entity.remainingFireTicks = max(entity.remainingFireTicks, BURN_TICKS)
        return true
    }

    companion object {
        /** 维持燃烧所需的火焰 tick（原版每 20 tick 结算一次伤害）。 */
        const val BURN_TICKS = 20
    }
}
