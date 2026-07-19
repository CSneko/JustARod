package org.cneko.justarod.entity

import net.minecraft.world.entity.*
import net.minecraft.world.entity.ai.goal.*
import net.minecraft.world.entity.ai.goal.target.*
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.world.entity.NeutralMob
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.entity.monster.Enemy
import net.minecraft.world.entity.AgeableMob
import net.minecraft.world.entity.TamableAnimal
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.nbt.CompoundTag
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.tags.FluidTags
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.util.TimeUtil
import net.minecraft.world.DifficultyInstance
import net.minecraft.world.level.ServerLevelAccessor
import net.minecraft.world.level.Level
import org.cneko.justarod.genetics.RodGenetics
import org.cneko.toneko.common.mod.genetics.api.*
import org.cneko.justarod.block.JRBlocks
import org.cneko.justarod.effect.JREffects
import org.cneko.justarod.item.rod.addEffect
import org.cneko.toneko.common.mod.entities.NekoEntity
import org.cneko.toneko.common.mod.items.ToNekoItems
import org.cneko.toneko.common.mod.misc.mixininterface.SlowTickable
import software.bernie.geckolib.animatable.GeoEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.animation.AnimationController
import software.bernie.geckolib.animation.AnimationController.AnimationStateHandler
import software.bernie.geckolib.animation.AnimationState
import software.bernie.geckolib.animation.RawAnimation
import software.bernie.geckolib.constant.DefaultAnimations
import software.bernie.geckolib.util.GeckoLibUtil
import java.util.*

/*
我不敢想象，如果它真的活了过来
哇哦哇哦，那可得太爽了呀~
 */
class RodEntity(private val entityType:EntityType<RodEntity>, world: Level):TamableAnimal(entityType,world),GeoEntity,NeutralMob,
    Enemy, SlowTickable, IGeneticEntity {
    private val animCache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)
    private val defSpeed:Double = 0.8
    private val slowSpeed:Double = 0.6
    private var slowTickCount = 0

    // ========== 遗传学相关 ==========
    private var genome: Genome = Genome()
    private val geneticData: CompoundTag = CompoundTag()
    private val activeTraits: MutableList<IGeneticEntity.ExpressedTrait> = ArrayList()
    private val activeGeneticGoals: MutableList<net.minecraft.world.entity.ai.goal.Goal> = ArrayList()

    override fun getGenome(): Genome = genome
    override fun setGenome(genome: Genome) { this.genome = genome }
    override fun getGeneticData(): CompoundTag = geneticData
    override fun getActiveTraits(): MutableList<IGeneticEntity.ExpressedTrait> = activeTraits
    override fun getActiveGeneticGoals(): MutableList<net.minecraft.world.entity.ai.goal.Goal> = activeGeneticGoals
    override fun expressTraits() {
        if (!level().isClientSide) {
            genome.express(this)
            // 同步计算值到客户端，方便渲染
            entityData.set(LENGTH_BONUS, RodGenetics.getTotalLengthBonus(geneticData).toFloat())
            entityData.set(WIDTH_BONUS, RodGenetics.getTotalWidthBonus(geneticData).toFloat())
            entityData.set(ORGASM_INTENSITY, RodGenetics.getOrgasmMultiplier(geneticData).toFloat())
        }
    }

    override fun getBreedOffspring(world: ServerLevel?, entity: AgeableMob?): AgeableMob {
        val baby = RodEntity(entityType, world!!)
        if (entity is IGeneticEntity) {
            val paternal = genome.createGamete(random)
            val maternal = entity.genome.createGamete(entity.random)
            baby.setGenome(Genome.combine(paternal, maternal, RodGenetics.KARYOTYPE))
        } else {
            val g1 = Genome.generateFallbackGamete(random, RodGenetics.KARYOTYPE)
            val g2 = Genome.generateFallbackGamete(random, RodGenetics.KARYOTYPE)
            baby.setGenome(Genome.combine(g1, g2, RodGenetics.KARYOTYPE))
        }
        baby.expressTraits()
        return baby
    }

    override fun isFood(stack: ItemStack?): Boolean {
        return stack?.`is`(Items.END_ROD) == true
    }

    override fun registerGoals() {
        super.registerGoals()
        goalSelector.addGoal(3, BreedGoal(this, slowSpeed))
        goalSelector.addGoal(5, FollowParentGoal(this, slowSpeed))
        goalSelector.addGoal(6, WaterAvoidingRandomStrollGoal(this, 0.1))
        goalSelector.addGoal(
            7, LookAtPlayerGoal(
                this,
                Player::class.java, 6.0f
            )
        )
        goalSelector.addGoal(2, MeleeAttackGoal(this, defSpeed, true))
        goalSelector.addGoal(8, RandomLookAroundGoal(this))
        goalSelector.addGoal(2,TemptGoal(this, defSpeed, {stack->stack.`is`(Items.END_ROD)||stack.`is`(JRBlocks.GOLDEN_LEAVES.asItem())},false))
        goalSelector.addGoal(1,FollowOwnerGoal(this, defSpeed, 10.0f, 2.0f))


        targetSelector.addGoal(2, OwnerHurtByTargetGoal(this))

        targetSelector.apply {
            addGoal(1, OwnerHurtTargetGoal(this@RodEntity)) // 跟踪攻击主人的目标
            addGoal(2, OwnerHurtByTargetGoal(this@RodEntity))    // 攻击主人攻击的目标
            addGoal(3, HurtByTargetGoal(this@RodEntity).setAlertOthers()) // 被攻击时复仇
            addGoal(10, NearestAttackableTargetGoal(
                this@RodEntity,
                Monster::class.java,  // 主动攻击所有敌对生物
                true
            ) { _ -> true })

            // 修改后的玩家目标选择条件
            addGoal(1, NearestAttackableTargetGoal(
                this@RodEntity,
                Player::class.java,
                false
            ) { entity ->
                isAngryAt(entity as LivingEntity) // 移除!isTamed条件
            })

            addGoal(5, ResetUniversalAngerTargetGoal(this@RodEntity, true)) // 通用愤怒机制
        }
    }

    override fun doHurtTarget(target: Entity?): Boolean {
        if (target is LivingEntity){
            val intensity = getOrgasmIntensity()
            target.addEffect(JREffects.ORGASM_EFFECT, (100 * intensity).toInt(), 0)
        }
        return super.doHurtTarget(target)
    }

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar?) {
        controllers!!.add(AnimationController<RodEntity>(this, 20, AnimationStateHandler { state: AnimationState<*>? ->
            if (this.pose == Pose.SWIMMING && !this.isInLiquid) {
                return@AnimationStateHandler state!!.setAndContinue(DefaultAnimations.CRAWL)
            } else if (this.isInLiquid && this.isEyeInFluid(FluidTags.WATER)) {
                return@AnimationStateHandler if (state!!.isMoving) state.setAndContinue(DefaultAnimations.SWIM) else state.setAndContinue(
                    DefaultAnimations.CRAWL
                )
            } else if (!state!!.isMoving) {
                return@AnimationStateHandler if (this.isInSittingPose) state.setAndContinue(
                    RawAnimation.begin().thenLoop("misc.sit")
                ) else state.setAndContinue(DefaultAnimations.IDLE)
            } else {
                return@AnimationStateHandler if (this.getDeltaMovement().length() > 0.2) state.setAndContinue(
                    DefaultAnimations.RUN
                ) else state.setAndContinue(DefaultAnimations.WALK)
            }
        }))
    }

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache {
        return animCache
    }

    override fun tick() {
        super.tick()
        if (random.nextInt(10) == 0) {
            level().addParticle(
                ParticleTypes.END_ROD,
                this.x + (random.nextDouble() - 0.5) * 0.5,
                this.y + random.nextDouble() * 0.5,
                this.z + (random.nextDouble() - 0.5) * 0.5,
                0.0,
                0.0,
                0.0
            )
        }
        tickPersistentAnger()
        // 如果头上有生物，给予orgasm（强度受基因影响）
        if (firstPassenger is LivingEntity) {
            val intensity = getOrgasmIntensity()
            (firstPassenger as LivingEntity).addEffect(JREffects.ORGASM_EFFECT, (100 * intensity).toInt(), 0)
        }
        if (slowTickCount++> 20) {
            slowTickCount = 0
            `toneko$slowTick`()
        }
    }

    override fun `toneko$slowTick`() {
        // 如果周围有猫娘，则缓慢回血
        if (level().isClientSide) return
        var nekoCount = 0
        if (level().getEntitiesOfClass(NekoEntity::class.java, this.boundingBox.inflate(10.0)) {
            nekoCount ++
            true
        }.isNotEmpty()  ) {
            if (health < maxHealth) {
                heal(nekoCount.toFloat())
            }
        }
    }

    override fun mobInteract(player: Player?, hand: InteractionHand?): InteractionResult {
        val stack = player?.getItemInHand(hand!!)
        if (!isTame && stack?.`is`(Items.END_ROD) == true) {
            if (!level().isClientSide) {
                if (random.nextInt(3) == 0) {
                    tryTame(player)
                    level().broadcastEntityEvent(this, EntityEvent.TAMING_SUCCEEDED)
                    if (!player.isCreative) {
                        stack.shrink(1)
                    }
                    return InteractionResult.SUCCESS
                } else {
                    level().broadcastEntityEvent(this, EntityEvent.TAMING_FAILED)
                }
            }
            return InteractionResult.SUCCESS
        }
        if (isTame && isOwnedBy(player)) {
            if (stack?.`is`(JRBlocks.GOLDEN_LEAVES.asItem()) == true && health < maxHealth) {
                if (!level().isClientSide) {
                    heal(4.0f)
                    if (!player.isCreative) {
                        stack.shrink(1)
                    }
                }
                return InteractionResult.SUCCESS
            }
        }
        if (player?.isHolding{
                    i -> i.item == ToNekoItems.NEKO_POTION
            } == true){
            if (!player.isCreative) {
                player.getItemInHand(hand!!).shrink(1)
                player.addItem(ItemStack(Items.GLASS_BOTTLE))
            }
            if (level() is ServerLevel) {
                level() as ServerLevel
                this.remove(RemovalReason.DISCARDED)
                val neko = SeeeeexNekoEntity(JREntities.SEEEEEX_NEKO, level())
                neko.setPos(this.x, this.y, this.z)
                neko.sexualDesire = 200
                level().addFreshEntity(neko)
                if (this.hasCustomName()){
                    neko.customName = this.customName
                }
            }
        }
        return super.mobInteract(player, hand)
    }

    override fun handleDamageEvent(damageSource: DamageSource?) {
        super.handleDamageEvent(damageSource)
        if (damageSource?.entity is Player && !isOwnedBy((damageSource.entity as Player))) {
            angryAt = (damageSource.entity as Player).uuid
            startPersistentAngerTimer()
        }
        if (damageSource?.entity is NekoEntity){
            // 变大
            this.getAttribute(Attributes.SCALE)?.let {
                it.baseValue = it.baseValue + 0.2
            }
            this.getAttribute(Attributes.MAX_HEALTH)?.let {
                it.baseValue = it.baseValue + 2.0
            }
            this.health = this.health + 2.0f
            angryAt = damageSource.entity?.uuid
        }

    }

    // ========== 遗传学初始化（自然生成时分配随机基因） ==========
    override fun finalizeSpawn(
        world: ServerLevelAccessor,
        difficulty: DifficultyInstance,
        reason: MobSpawnType,
        entityData: SpawnGroupData?
    ): SpawnGroupData? {
        val g1 = Genome.generateFallbackGamete(random, RodGenetics.KARYOTYPE)
        val g2 = Genome.generateFallbackGamete(random, RodGenetics.KARYOTYPE)
        genome = Genome.combine(g1, g2, RodGenetics.KARYOTYPE)
        expressTraits()
        return super.finalizeSpawn(world, difficulty, reason, entityData)
    }

    // ========== 基因值查询（供渲染/效果使用） ==========

    /** 总长度加成 */
    fun getLengthBonus(): Float = entityData.get(LENGTH_BONUS)
    /** 总宽度加成 */
    fun getWidthBonus(): Float = entityData.get(WIDTH_BONUS)
    /** 高潮强度倍率 (1.0 = 普通) */
    fun getOrgasmIntensity(): Float = entityData.get(ORGASM_INTENSITY)

    private fun tryTame(player: Player) {
        ownerUUID = player.uuid
        setTame(true,true)
        tame(player)
        target = null
        isInSittingPose = false
    }

    override fun applyTamingSideEffects() {
        super.applyTamingSideEffects()
        getAttribute(Attributes.MAX_HEALTH)?.baseValue = 40.0
        // 添加移动速度调整
        getAttribute(Attributes.MOVEMENT_SPEED)?.baseValue = defSpeed
    }
    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(ANGER_TIME,0)
        builder.define(LENGTH_BONUS, 0.0f)
        builder.define(WIDTH_BONUS, 0.0f)
        builder.define(ORGASM_INTENSITY, 1.0f)
    }

    override fun addAdditionalSaveData(nbt: CompoundTag) {
        super.addAdditionalSaveData(nbt)
        addPersistentAngerSaveData(nbt)
        nbt.put("Genome", genome.save())
        nbt.put("GeneticData", geneticData)
    }

    override fun readAdditionalSaveData(nbt: CompoundTag) {
        super.readAdditionalSaveData(nbt)
        readPersistentAngerSaveData(level(), nbt)
        if (nbt.contains("Genome")) {
            genome.load(nbt.getCompound("Genome"))
        }
        if (nbt.contains("GeneticData")) {
            val loaded = nbt.getCompound("GeneticData")
            for (key in loaded.allKeys) {
                geneticData.put(key, loaded.get(key))
            }
        }
        expressTraits()
    }

    private var angerTime = 0
    private var angryAt: UUID? = null

    override fun getRemainingPersistentAngerTime(): Int = entityData.get(ANGER_TIME)
    override fun setRemainingPersistentAngerTime(time: Int) = entityData.set(ANGER_TIME, time)

    override fun getPersistentAngerTarget() = angryAt
    override fun setPersistentAngerTarget(uuid: UUID?) { angryAt = uuid }

    override fun startPersistentAngerTimer() {
        remainingPersistentAngerTime = ANGER_TIME_RANGE.sample(this.random)
    }

    override fun isAngryAt(entity: LivingEntity?): Boolean {
        return this.isAngry && this.angryAt?.equals(entity?.uuid) == true
    }

    fun tickPersistentAnger() {
        if (level().isClientSide) return
        if (isAngry) {
            remainingPersistentAngerTime = (remainingPersistentAngerTime - 1)
            if (!isAngry) {
                onAngerRemoved()
            }
        }
    }
    private fun onAngerRemoved() {
        angryAt = null
        target = null
    }


    companion object{
        fun createRodAttribute():AttributeSupplier.Builder{
            return createMobAttributes().add(Attributes.ATTACK_DAMAGE,4.0)
        }
        private val ANGER_TIME = SynchedEntityData.defineId(RodEntity::class.java, EntityDataSerializers.INT)
        private val ANGER_TIME_RANGE = TimeUtil.rangeOfSeconds(20, 39)

        // 遗传学同步数据
        private val LENGTH_BONUS = SynchedEntityData.defineId(RodEntity::class.java, EntityDataSerializers.FLOAT)
        private val WIDTH_BONUS = SynchedEntityData.defineId(RodEntity::class.java, EntityDataSerializers.FLOAT)
        private val ORGASM_INTENSITY = SynchedEntityData.defineId(RodEntity::class.java, EntityDataSerializers.FLOAT)
    }
}