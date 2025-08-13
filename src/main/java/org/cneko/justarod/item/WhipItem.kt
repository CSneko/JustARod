package org.cneko.justarod.item

import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.particle.DustParticleEffect
import net.minecraft.particle.ParticleEffect
import net.minecraft.particle.ParticleTypes
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.util.UseAction
import net.minecraft.world.World
import net.minecraft.util.math.Vec3d
import org.joml.Vector3f
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

open class WhipItem(settings: Settings) : Item(settings.maxCount(1).maxDamage(1000)) {

    override fun postHit(stack: ItemStack, target: LivingEntity, attacker: LivingEntity): Boolean {
        // 左键普通攻击：主目标满伤，其他目标半伤
        applyWhipEffect(attacker, 4.0, 0.4, 40, 1, fullDamage = false, charged = false)
        return super.postHit(stack, target, attacker)
    }
    override fun inventoryTick(stack: ItemStack, world: World, entity: net.minecraft.entity.Entity, slot: Int, selected: Boolean) {
        super.inventoryTick(stack, world, entity, slot, selected)

        if (world.isClient && selected && entity is PlayerEntity && entity.isUsingItem && entity.activeItemStack == stack) {
            val useTicks = entity.itemUseTime
            val chargeRatio =
                min((getMaxUseTime(stack, entity) - useTicks).toDouble() / getMaxUseTime(stack, entity), 1.0)

            // 粒子密度随蓄力增加
            val particleCount = (2 + 4 * chargeRatio).toInt()

            val yawRad = Math.toRadians(entity.yaw.toDouble())

            for (i in 0 until particleCount) {
                val offsetX = (-0.5 + Math.random()) * 0.3
                val offsetY = (-0.5 + Math.random()) * 0.3
                val offsetZ = (-0.5 + Math.random()) * 0.3

                val x = entity.x - sin(yawRad) * 0.5 + offsetX
                val y = entity.eyeY - 0.3 + offsetY
                val z = entity.z + cos(yawRad) * 0.5 + offsetZ

                // 粒子颜色从淡蓝到亮蓝
                val r = 0.2f * (1 - chargeRatio).toFloat()
                val g = 0.6f * (1 - chargeRatio).toFloat()
                val b = (0.5 + 0.5 * chargeRatio).toFloat()

                // 粒子大小随蓄力增加
                val size = 0.1f + 0.2f * chargeRatio.toFloat()

                world.addParticle(
                    DustParticleEffect(Vector3f(r, g, b), size),
                    x, y, z,
                    0.0, 0.02, 0.0
                )
            }
        }
    }

    override fun use(world: World, user: PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
        user.setCurrentHand(hand)
        return TypedActionResult.consume(user.getStackInHand(hand))
    }

    override fun finishUsing(stack: ItemStack, world: World, user: LivingEntity): ItemStack {
        return stack
    }

    override fun getUseAction(stack: ItemStack): UseAction {
        return UseAction.BOW // 播放拉弓动作
    }

    override fun getMaxUseTime(stack: ItemStack?, user: LivingEntity?): Int {
        return 30 // 蓄力最长 1.5 秒（30 tick）
    }

    override fun onStoppedUsing(stack: ItemStack, world: World, user: LivingEntity, remainingUseTicks: Int) {
        if (user !is PlayerEntity) return

        val chargedTicks = getMaxUseTime(stack, user) - remainingUseTicks
        val chargeRatio = min(chargedTicks / getMaxUseTime(stack, user).toDouble(), 1.0) // 0 ~ 1

        // 蓄力效果计算
        val range = 4.0 + 4.0 * chargeRatio // 4~8格
        val knockback = 0.4 + 0.6 * chargeRatio // 0.4~1.0
        val duration = 40 + (20 * chargeRatio).toInt() // 2~3秒缓慢

        applyWhipEffect(user, range, knockback, duration, 2, fullDamage = true, charged = true)

        // 播放甩鞭音效（音调随蓄力变化）
        world.playSound(
            null,
            user.blockPos,
            SoundEvents.ENTITY_FISHING_BOBBER_RETRIEVE,
            SoundCategory.PLAYERS,
            1.0f,
            0.8f + 0.4f * chargeRatio.toFloat()
        )
        world.playSound(
            null,
            user.blockPos,
            SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
            SoundCategory.PLAYERS,
            0.8f,
            1.0f
        )

        // 耐久消耗（蓄力用多一点）
        stack.damage(2 + (chargeRatio * 2).toInt(), user, EquipmentSlot.MAINHAND)
    }

    private fun applyWhipEffect(
        attacker: LivingEntity,
        range: Double,
        knockbackStrength: Double,
        slownessDuration: Int,
        slownessLevel: Int,
        fullDamage: Boolean,
        charged: Boolean
    ) {
        val world = attacker.world
        val angleRange = Math.toRadians(60.0)
        val attackerYaw = Math.toRadians(attacker.yaw.toDouble())

        val arcParticle = if (charged) ParticleTypes.ELECTRIC_SPARK else ParticleTypes.SWEEP_ATTACK
        spawnWhipArcParticles(world, attacker, range, angleRange, arcParticle)

        val entities = world.getEntitiesByClass(
            LivingEntity::class.java,
            attacker.boundingBox.expand(range),
        ) { it != attacker && attacker.isInRange(it, range) }

        for (entity in entities) {
            val directionToEntity = entity.pos.subtract(attacker.pos).normalize()
            val attackerLookVec = Vec3d(-sin(attackerYaw), 0.0, cos(attackerYaw))
            val dot = attackerLookVec.dotProduct(directionToEntity)
            val angle = acos(dot.coerceIn(-1.0, 1.0))

            if (angle <= angleRange / 2) {
                world.addParticle(
                    ParticleTypes.CRIT,
                    entity.x,
                    entity.eyeY,
                    entity.z,
                    0.0, 0.0, 0.0
                )

                entity.addStatusEffect(StatusEffectInstance(StatusEffects.SLOWNESS, slownessDuration, slownessLevel))
                entity.addVelocity(
                    -sin(attackerYaw) * knockbackStrength,
                    0.1,
                    cos(attackerYaw) * knockbackStrength
                )
                entity.velocityModified = true

                // 调用抽象方法
                val damageAmount = if (fullDamage) 4.0f else 2.0f
                hitTarget(attacker, entity, damageAmount)
            }
        }
    }

    protected open fun hitTarget(attacker: LivingEntity, target: LivingEntity, amount: Float) {
        val damageSource = attacker.damageSources.mobAttack(attacker)
        target.damage(damageSource, amount)
    }



    private fun spawnWhipArcParticles(world: World, attacker: LivingEntity, range: Double, angleRange: Double, particle: ParticleEffect) {
        val yawRad = Math.toRadians(attacker.yaw.toDouble())
        val steps = 10 // 粒子密度
        val arcSteps = 6 // 横向分段

        for (r in 1..steps) {
            val radius = (r / steps.toDouble()) * range
            for (a in -arcSteps..arcSteps) {
                val angleOffset = (a / arcSteps.toDouble()) * (angleRange / 2)
                val x = attacker.x - sin(yawRad + angleOffset) * radius
                val z = attacker.z + cos(yawRad + angleOffset) * radius
                val y = attacker.eyeY - 0.3

                world.addParticle(
                    particle,
                    x,
                    y,
                    z,
                    0.0, 0.0, 0.0
                )
            }
        }
    }




    override fun appendTooltip(
        stack: ItemStack?,
        context: TooltipContext?,
        tooltip: MutableList<Text?>?,
        type: TooltipType?
    ) {
        tooltip?.add(Text.literal("§7左键：4格横扫，缓慢II（2秒）"))
        tooltip?.add(Text.literal("§7右键蓄力：最大8格，缓慢III（3秒），击退更强"))
        tooltip?.add(Text.literal("§7命中有粒子与音效反馈"))
        super.appendTooltip(stack, context, tooltip, type)
    }
}
