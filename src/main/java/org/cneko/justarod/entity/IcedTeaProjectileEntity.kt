package org.cneko.justarod.entity

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.BossEvent
import net.minecraft.world.entity.boss.wither.WitherBoss
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.monster.Ghast
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.monster.Phantom
import net.minecraft.world.entity.animal.allay.Allay
import net.minecraft.world.entity.ambient.Bat
import net.minecraft.world.entity.animal.Bee
import net.minecraft.world.entity.animal.Parrot
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.ThrowableItemProjectile
import net.minecraft.world.item.Item
import net.minecraft.core.particles.ItemParticleOption
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.boss.EnderDragonPart
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3
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
class IcedTeaProjectileEntity : ThrowableItemProjectile, GeoEntity {
    val cache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)

    // 忽略初始碰撞的计时器
    private var ignoreCollisionTicks = 2

    // 实体所有者构造器
    constructor(entityType: EntityType<out ThrowableItemProjectile>, world: Level) : super(entityType, world)
    constructor(world: Level, owner: LivingEntity?) : super(ICED_TEA_PROJECTILE, owner, world) {
        // 将实体生成在玩家前方
        owner?.let {
            val lookVec = it.rotationVector
            val offset = 0.5 // 向前偏移0.5格
            setPos(
                it.x + lookVec.x * offset,
                it.eyeY + lookVec.y * offset,
                it.z + lookVec.length() * offset
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
    override fun onHit(hitResult: HitResult) {
        // 忽略初始碰撞
        if (ignoreCollisionTicks > 0) return

        super.onHit(hitResult)
        if (!level().isClientSide) {
            // 在服务器端生成粒子效果和声音
            level().broadcastEntityEvent(this, 3.toByte())
            discard()
        }
    }

    /**
     * 处理与实体的碰撞。
     */
    override fun onHitEntity(entityHitResult: EntityHitResult) {
        // 忽略初始碰撞
        if (ignoreCollisionTicks > 0) return

        var entity = entityHitResult.entity
        // 兼容末影龙部件
        if (entity is EnderDragonPart) {
            entity = entity.parentMob
        }
        // 忽略投掷者
        if (entity == owner) return

        // 造成初始伤害
        entity.hurt(entity.damageSources().thrown(this, this.owner), 0.5f)
        // 移除飞行相关效果
        if (entity is LivingEntity) {
            entity.removeEffect(MobEffects.SLOW_FALLING)
            entity.removeEffect(MobEffects.LEVITATION)
        }

        // 强制实体坠机
        entity.deltaMovement = entity.getDeltaMovement().multiply(1.0, 0.0, 1.0)
        entity.addDeltaMovement(Vec3(0.0, -4.0, 0.0))

        entity.`justARod$setFallenBy`(owner)
    }

    override fun handleEntityEvent(status: Byte) {
        if (status.toInt() == 3) {
            val particleEffect = ItemParticleOption(ParticleTypes.ITEM, this.item)
            for (i in 0..7) {
                this.level().addParticle(particleEffect, this.x, this.y, this.z, 0.0, 0.0, 0.0)
            }
            this.level().playLocalSound(this.x, this.y, this.z, SoundEvents.GENERIC_SPLASH, this.soundSource, 1.0f, 1.0f, false)
        }
    }

    private val Entity.isFlying: Boolean
        get() = when (this) {
            is Player -> this.abilities.flying || this.isFallFlying
            is Bat -> true
            is Parrot -> !this.onGround()
            is Phantom -> true
            is Ghast -> true
            is Bee -> this.isFlying
            is Allay -> !this.onGround()
            is EnderDragon -> true
            is WitherBoss -> true
            is Mob -> !this.onGround()
            else -> false
        }





    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar?) {
    }

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache {
        return cache
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder?) {
        super.defineSynchedData(builder)
    }

}