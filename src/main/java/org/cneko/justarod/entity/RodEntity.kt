package org.cneko.justarod.entity

import net.minecraft.entity.Entity
import net.minecraft.entity.EntityStatuses
import net.minecraft.entity.EntityType
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.ai.goal.*
import net.minecraft.entity.attribute.DefaultAttributeContainer
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.data.DataTracker
import net.minecraft.entity.data.TrackedDataHandlerRegistry
import net.minecraft.entity.mob.Angerable
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.entity.mob.Monster
import net.minecraft.entity.passive.PassiveEntity
import net.minecraft.entity.passive.TameableEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.TimeHelper
import net.minecraft.world.World
import org.cneko.justarod.block.JRBlocks
import org.cneko.justarod.effect.JREffects
import org.cneko.justarod.item.addEffect
import org.cneko.toneko.common.mod.entities.NekoEntity
import org.cneko.toneko.common.mod.items.ToNekoItems
import org.cneko.toneko.common.mod.misc.mixininterface.SlowTickable
import software.bernie.geckolib.animatable.GeoEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil
import java.util.*

/*
我不敢想象，如果它真的活了过来
哇哦哇哦，那可得太爽了呀~
 */
class RodEntity(private val entityType:EntityType<RodEntity>, world: World):TameableEntity(entityType,world),GeoEntity,Angerable,
    Monster, SlowTickable {
    private val animCache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)
    private val defSpeed:Double = 0.8
    private val slowSpeed:Double = 0.6
    private var slowTickCount = 0
    override fun createChild(world: ServerWorld?, entity: PassiveEntity?): PassiveEntity {
        val baby = RodEntity(entityType, world!!)
        return baby
    }

    override fun isBreedingItem(stack: ItemStack?): Boolean {
        return stack?.isOf(Items.END_ROD) == true
    }

    override fun initGoals() {
        super.initGoals()
        goalSelector.add(3, AnimalMateGoal(this, slowSpeed))
        goalSelector.add(5, FollowParentGoal(this, slowSpeed))
        goalSelector.add(6, WanderAroundFarGoal(this, 0.1))
        goalSelector.add(
            7, LookAtEntityGoal(
                this,
                PlayerEntity::class.java, 6.0f
            )
        )
        goalSelector.add(2, MeleeAttackGoal(this, defSpeed, true))
        goalSelector.add(8, LookAroundGoal(this))
        goalSelector.add(2,TemptGoal(this, defSpeed, {stack->stack.isOf(Items.END_ROD)||stack.isOf(JRBlocks.GOLDEN_LEAVES.asItem())},false))
        goalSelector.add(1,FollowOwnerGoal(this, defSpeed, 10.0f, 2.0f))


        targetSelector.add(2, AttackWithOwnerGoal(this))

        targetSelector.apply {
            add(1, TrackOwnerAttackerGoal(this@RodEntity)) // 跟踪攻击主人的目标
            add(2, AttackWithOwnerGoal(this@RodEntity))    // 攻击主人攻击的目标
            add(3, RevengeGoal(this@RodEntity).setGroupRevenge()) // 被攻击时复仇
            add(10, ActiveTargetGoal(
                this@RodEntity,
                HostileEntity::class.java,  // 主动攻击所有敌对生物
                true
            ) { _ -> true })

            // 修改后的玩家目标选择条件
            add(1, ActiveTargetGoal(
                this@RodEntity,
                PlayerEntity::class.java,
                false
            ) { entity ->
                shouldAngerAt(entity as LivingEntity) // 移除!isTamed条件
            })

            add(5, UniversalAngerGoal(this@RodEntity, true)) // 通用愤怒机制
        }
    }

    override fun tryAttack(target: Entity?): Boolean {
        if (target is LivingEntity){
            target.addEffect(JREffects.ORGASM_EFFECT, 100, 0)
        }
        return super.tryAttack(target)
    }

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar?) {
    }

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache {
        return animCache
    }

    override fun tick() {
        super.tick()
        if (random.nextInt(10) == 0) {
            world.addParticle(
                ParticleTypes.END_ROD,
                this.x + (random.nextDouble() - 0.5) * 0.5,
                this.y + random.nextDouble() * 0.5,
                this.z + (random.nextDouble() - 0.5) * 0.5,
                0.0,
                0.0,
                0.0
            )
        }
        tickAnger()
        // 如果头上有生物，给予orgasm
        if (firstPassenger is LivingEntity) {
            (firstPassenger as LivingEntity).addEffect(JREffects.ORGASM_EFFECT, 100, 0)
        }
        if (slowTickCount++> 20) {
            slowTickCount = 0
            `toneko$slowTick`()
        }
    }

    override fun `toneko$slowTick`() {
        // 如果周围有猫娘，则缓慢回血
        if (world.isClient) return
        var nekoCount = 0
        if (world.getEntitiesByClass(NekoEntity::class.java, this.boundingBox.expand(10.0)) {
            nekoCount ++
            true
        }.isNotEmpty()  ) {
            if (health < maxHealth) {
                heal(nekoCount.toFloat())
            }
        }
    }

    override fun interactMob(player: PlayerEntity?, hand: Hand?): ActionResult {
        val stack = player?.getStackInHand(hand!!)
        if (!isTamed && stack?.isOf(Items.END_ROD) == true) {
            if (!world.isClient) {
                if (random.nextInt(3) == 0) {
                    tryTame(player)
                    world.sendEntityStatus(this, EntityStatuses.ADD_POSITIVE_PLAYER_REACTION_PARTICLES)
                    if (!player.isCreative) {
                        stack.decrement(1)
                    }
                    return ActionResult.SUCCESS
                } else {
                    world.sendEntityStatus(this, EntityStatuses.ADD_NEGATIVE_PLAYER_REACTION_PARTICLES)
                }
            }
            return ActionResult.success(world.isClient)
        }
        if (isTamed && isOwner(player)) {
            if (stack?.isOf(JRBlocks.GOLDEN_LEAVES.asItem()) == true && health < maxHealth) {
                if (!world.isClient) {
                    heal(4.0f)
                    if (!player.isCreative) {
                        stack.decrement(1)
                    }
                }
                return ActionResult.success(world.isClient)
            }
        }
        if (player?.isHolding{
                    i -> i.item == ToNekoItems.NEKO_POTION
            } == true){
            if (!player.isCreative) {
                player.getStackInHand(hand!!).decrement(1)
                player.giveItemStack(ItemStack(Items.GLASS_BOTTLE))
            }
            if (world is ServerWorld) {
                world as ServerWorld
                this.remove(RemovalReason.DISCARDED)
                val neko = SeeeeexNekoEntity(JREntities.SEEEEEX_NEKO, world)
                neko.setPos(this.x, this.y, this.z)
                neko.sexualDesire = 200
                world.spawnEntity(neko)
                if (this.hasCustomName()){
                    neko.customName = this.customName
                }
            }
        }
        return super.interactMob(player, hand)
    }

    override fun onDamaged(damageSource: DamageSource?) {
        super.onDamaged(damageSource)
        if (damageSource?.attacker is PlayerEntity && !isOwner((damageSource.attacker as PlayerEntity))) {
            angryAt = (damageSource.attacker as PlayerEntity).uuid
            chooseRandomAngerTime()
        }
        if (damageSource?.attacker is NekoEntity){
            // 变大
            this.getAttributeInstance(EntityAttributes.GENERIC_SCALE)?.let {
                it.baseValue = it.baseValue + 0.2
            }
            this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH)?.let {
                it.baseValue = it.baseValue + 2.0
            }
            this.health = this.health + 2.0f
            angryAt = damageSource.attacker?.uuid
        }

    }


    private fun tryTame(player: PlayerEntity) {
        ownerUuid = player.uuid
        setTamed(true,true)
        setOwner(player)
        target = null
        isSitting = false
    }

    override fun updateAttributesForTamed() {
        super.updateAttributesForTamed()
        getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH)?.baseValue = 40.0
        // 添加移动速度调整
        getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED)?.baseValue = defSpeed
    }
    override fun initDataTracker(builder: DataTracker.Builder?) {
        super.initDataTracker(builder)
        builder?.add(ANGER_TIME,0)
    }

    override fun writeCustomDataToNbt(nbt: NbtCompound) {
        super.writeCustomDataToNbt(nbt)
        writeAngerToNbt(nbt)
    }

    override fun readCustomDataFromNbt(nbt: NbtCompound) {
        super.readCustomDataFromNbt(nbt)
        readAngerFromNbt(world, nbt)
    }

    private var angerTime = 0
    private var angryAt: UUID? = null

    override fun getAngerTime(): Int = dataTracker.get(ANGER_TIME)
    override fun setAngerTime(time: Int) = dataTracker.set(ANGER_TIME, time)

    override fun getAngryAt() = angryAt
    override fun setAngryAt(uuid: UUID?) { angryAt = uuid }

    override fun chooseRandomAngerTime() {
        setAngerTime(ANGER_TIME_RANGE.get(random))
    }

    override fun shouldAngerAt(entity: LivingEntity?): Boolean {
        return this.hasAngerTime() && this.angryAt?.equals(entity?.uuid) == true
    }

    fun tickAnger() {
        if (world.isClient) return
        if (hasAngerTime()) {
            setAngerTime(getAngerTime() - 1)
            if (!hasAngerTime()) {
                onAngerRemoved()
            }
        }
    }
    private fun onAngerRemoved() {
        angryAt = null
        target = null
    }

    companion object{
        fun createRodAttribute():DefaultAttributeContainer.Builder{
            return createMobAttributes().add(EntityAttributes.GENERIC_ATTACK_DAMAGE,4.0)
        }
        private val ANGER_TIME = DataTracker.registerData(RodEntity::class.java, TrackedDataHandlerRegistry.INTEGER)
        private val ANGER_TIME_RANGE = TimeHelper.betweenSeconds(20, 39)
    }
}