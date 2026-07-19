package org.cneko.justarod.item

import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.sounds.SoundSource
import net.minecraft.sounds.SoundEvents
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.item.UseAnim
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/*
啊~♡ 主人别打了喵~
 */
open class WhipItem(properties: Properties) : Item(settings.maxCount(1).maxDamage(1000)) {

    override fun postHit(stack: ItemStack, target: LivingEntity, attacker: LivingEntity): Boolean {
        // 左键普通攻击：主目标满伤（4点），其他目标半伤（2点）
        applyWhipEffect(attacker, 4.0, 0.4, 40, 1, 2.0f, charged = false)
        return super.postHit(stack, target, attacker)
    }

    override fun inventoryTick(stack: ItemStack, world: Level, entity: net.minecraft.world.entity.Entity, slot: Int, selected: Boolean) {
        super.inventoryTick(stack, world, entity, slot, selected)

        if (level().isClientSide && selected && entity is Player && entity.isUsingItem && entity.useItem == stack) {
            val useTicks = entity.itemUseTime
            val chargeRatio =
                min((getMaxUseTime(stack, entity) - useTicks).toDouble() / getMaxUseTime(stack, entity), 1.0)

            val particleCount = (2 + 4 * chargeRatio).toInt()
            val yawRad = Math.toRadians(entity.yaw.toDouble())

            for (i in 0 until particleCount) {
                val offsetX = (-0.5 + Math.random()) * 0.3
                val offsetY = (-0.5 + Math.random()) * 0.3
                val offsetZ = (-0.5 + Math.random()) * 0.3

                val x = entity.x - sin(yawRad) * 0.5 + offsetX
                val y = entity.eyeY - 0.3 + offsetY
                val z = entity.z + cos(yawRad) * 0.5 + offsetZ

                val r = 0.2f * (1 - chargeRatio).toFloat()
                val g = 0.6f * (1 - chargeRatio).toFloat()
                val b = (0.5 + 0.5 * chargeRatio).toFloat()
                val size = 0.1f + 0.2f * chargeRatio.toFloat()

                level().addParticle(
                    DustParticleOptions(Vector3f(r, g, b), size),
                    x, y, z,
                    0.0, 0.02, 0.0
                )
            }
        }
    }

    override fun use(world: Level, user: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        user.setCurrentHand(hand)
        return InteractionResultHolder.consume(user.getItemInHand(hand))
    }

    override fun finishUsing(stack: ItemStack, world: Level, user: LivingEntity): ItemStack {
        return stack
    }

    override fun getUseAction(stack: ItemStack): UseAnim {
        return UseAnim.BOW
    }

    override fun getMaxUseTime(stack: ItemStack?, user: LivingEntity?): Int {
        return 30
    }

    override fun onStoppedUsing(stack: ItemStack, world: Level, user: LivingEntity, remainingUseTicks: Int) {
        if (user !is Player) return

        val chargedTicks = getMaxUseTime(stack, user) - remainingUseTicks
        val chargeRatio = min(chargedTicks / getMaxUseTime(stack, user).toDouble(), 1.0)

        val range = 4.0 + 4.0 * chargeRatio
        val knockback = 0.4 + 0.6 * chargeRatio
        val duration = 40 + (20 * chargeRatio).toInt()

        // 伤害随蓄力增加：4.0 ~ 8.0
        val damage = (4.0f + 4.0f * chargeRatio).toFloat()

        applyWhipEffect(user, range, knockback, duration, 2, damage, charged = true)

        level().playSound(null, user.blockPosition(), SoundEvents.ENTITY_FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 1.0f, 0.8f + 0.4f * chargeRatio.toFloat())
        level().playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 1.0f)

        stack.hurt(2 + (chargeRatio * 2).toInt(), user, EquipmentSlot.MAINHAND)
    }

    private fun applyWhipEffect(
        attacker: LivingEntity,
        range: Double,
        knockbackStrength: Double,
        slownessDuration: Int,
        slownessLevel: Int,
        damage: Float,
        charged: Boolean
    ) {
        val world = attacker.world
        val angleRange = Math.toRadians(60.0)
        val attackerYaw = Math.toRadians(attacker.yaw.toDouble())

        val arcParticle = if (charged) ParticleTypes.ELECTRIC_SPARK else ParticleTypes.SWEEP_ATTACK
        spawnWhipArcParticles(world, attacker, range, angleRange, arcParticle)

        val entities = level().getEntitiesOfClass(
            LivingEntity::class.java,
            attacker.boundingBox.inflate(range),
        ) { it != attacker && attacker.isInRange(it, range) }

        for (entity in entities) {
            val directionToEntity = entity.pos.subtract(attacker.pos).normalize()
            val attackerLookVec = Vec3(-sin(attackerYaw), 0.0, cos(attackerYaw))
            val dot = attackerLookVec.dotProduct(directionToEntity)
            val angle = acos(dot.coerceIn(-1.0, 1.0))

            if (angle <= angleRange / 2) {
                world.addParticle(ParticleTypes.CRIT, entity.x, entity.eyeY, entity.z, 0.0, 0.0, 0.0)

                entity.addEffect(MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slownessDuration, slownessLevel))
                entity.addDeltaMovement(-sin(attackerYaw) * knockbackStrength, 0.1, cos(attackerYaw) * knockbackStrength)
                entity.velocityModified = true

                hitTarget(attacker, entity, damage)
            }
        }
    }

    protected open fun hitTarget(attacker: LivingEntity, target: LivingEntity, amount: Float) {
        val damageSource = attacker.damageSources.mobAttack(attacker)
        target.hurt(damageSource, amount)
    }

    private fun spawnWhipArcParticles(world: Level, attacker: LivingEntity, range: Double, angleRange: Double, particle: ParticleOptions) {
        val yawRad = Math.toRadians(attacker.yaw.toDouble())
        val steps = 10
        val arcSteps = 6

        for (r in 1..steps) {
            val radius = (r / steps.toDouble()) * range
            for (a in -arcSteps..arcSteps) {
                val angleOffset = (a / arcSteps.toDouble()) * (angleRange / 2)
                val x = attacker.x - sin(yawRad + angleOffset) * radius
                val z = attacker.z + cos(yawRad + angleOffset) * radius
                val y = attacker.eyeY - 0.3

                level().addParticle(particle, x, y, z, 0.0, 0.0, 0.0)
            }
        }
    }
}
