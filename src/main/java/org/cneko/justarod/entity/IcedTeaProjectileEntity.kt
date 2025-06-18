package org.cneko.justarod.entity

import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.boss.BossBar
import net.minecraft.entity.boss.WitherEntity
import net.minecraft.entity.boss.dragon.EnderDragonEntity
import net.minecraft.entity.boss.dragon.EnderDragonPart
import net.minecraft.entity.data.DataTracker
import net.minecraft.entity.data.TrackedDataHandlerRegistry
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.mob.GhastEntity
import net.minecraft.entity.mob.MobEntity
import net.minecraft.entity.mob.PhantomEntity
import net.minecraft.entity.passive.AllayEntity
import net.minecraft.entity.passive.BatEntity
import net.minecraft.entity.passive.BeeEntity
import net.minecraft.entity.passive.ParrotEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.projectile.thrown.ThrownItemEntity
import net.minecraft.item.Item
import net.minecraft.particle.ItemStackParticleEffect
import net.minecraft.particle.ParticleTypes
import net.minecraft.sound.SoundEvents
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.world.World
import org.cneko.justarod.damage.JRDamageTypes
import org.cneko.justarod.entity.JREntities.ICED_TEA_PROJECTILE
import org.cneko.justarod.item.JRItems.Companion.ICED_TEA
import software.bernie.geckolib.animatable.GeoEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil

/*
So let the light guide your way~ Hold every memory as you go~
And every road you take will always lead you home~
 */
class IcedTeaProjectileEntity : ThrownItemEntity, GeoEntity {
    val cache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

    // 忽略初始碰撞的计时器
    private var ignoreCollisionTicks = 2

    // 实体所有者构造器
    constructor(entityType: EntityType<out ThrownItemEntity>, world: World) : super(entityType, world)
    constructor(world: World, owner: LivingEntity?) : super(ICED_TEA_PROJECTILE, owner, world) {
        // 将实体生成在玩家前方
        owner?.let {
            val lookVec = it.rotationVector
            val offset = 0.5 // 向前偏移0.5格
            setPosition(
                it.x + lookVec.x * offset,
                it.eyeY + lookVec.y * offset,
                it.z + lookVec.z * offset
            )
        }
    }

    override fun getDefaultItem(): Item = ICED_TEA

    //tick方法处理初始碰撞忽略
    override fun tick() {
        super.tick()
        if (ignoreCollisionTicks > 0) {
            ignoreCollisionTicks--
        }
    }

    /**
     * 处理所有类型的碰撞（方块、实体等）。
     */
    override fun onCollision(hitResult: HitResult) {
        // 忽略初始碰撞
        if (ignoreCollisionTicks > 0) return

        super.onCollision(hitResult)
        if (!world.isClient) {
            // 在服务器端生成粒子效果和声音
            world.sendEntityStatus(this, 3.toByte())
            discard()
        }
    }

    /**
     * 处理与实体的碰撞。
     */
    override fun onEntityHit(entityHitResult: EntityHitResult) {
        // 忽略初始碰撞
        if (ignoreCollisionTicks > 0) return

        var entity = entityHitResult.entity
        // 兼容末影龙部件
        if (entity is EnderDragonPart) {
            entity = entity.owner
        }
        // 忽略投掷者
        if (entity == owner) return

        // 造成初始伤害
        entity.damage(entity.damageSources.thrown(this, this.owner), 0.5f)
        // 移除飞行相关效果
        if (entity is LivingEntity) {
            entity.removeStatusEffect(StatusEffects.SLOW_FALLING)
            entity.removeStatusEffect(StatusEffects.LEVITATION)
        }

        // 强制实体坠机
        entity.velocity = entity.velocity.multiply(1.0, 0.0, 1.0)
        entity.addVelocity(0.0, -4.0, 0.0)

        entity.`justARod$setFallenBy`(owner)
    }

    override fun handleStatus(status: Byte) {
        if (status.toInt() == 3) {
            val particleEffect = ItemStackParticleEffect(ParticleTypes.ITEM, this.stack)
            for (i in 0..7) {
                this.world.addParticle(particleEffect, this.x, this.y, this.z, 0.0, 0.0, 0.0)
            }
            this.world.playSound(this.x, this.y, this.z, SoundEvents.ENTITY_GENERIC_SPLASH, this.soundCategory, 1.0f, 1.0f, false)
        }
    }

    private val Entity.isFlying: Boolean
        get() = when (this) {
            is PlayerEntity -> this.abilities.flying || this.isFallFlying
            is BatEntity -> true
            is ParrotEntity -> !this.isOnGround
            is PhantomEntity -> true
            is GhastEntity -> true
            is BeeEntity -> this.isInAir
            is AllayEntity -> !this.isOnGround
            is EnderDragonEntity -> true
            is WitherEntity -> true
            is MobEntity -> !this.isOnGround
            else -> false
        }





    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar?) {
    }

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache {
        return cache
    }

    override fun initDataTracker(builder: DataTracker.Builder?) {
        super.initDataTracker(builder)
    }

}