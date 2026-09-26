package org.cneko.justarod.entity

import net.minecraft.world.entity.*
import net.minecraft.world.entity.ai.goal.*
import net.minecraft.world.entity.ai.goal.target.*
import net.minecraft.world.entity.ai.targeting.TargetingConditions
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
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import org.cneko.justarod.genetics.RodGenetics
import org.cneko.justarod.api.VisualSized
import org.cneko.toneko.common.mod.genetics.api.*
import org.cneko.justarod.block.JRBlocks
import org.cneko.justarod.effect.JREffects
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.core.registries.BuiltInRegistries
import org.cneko.toneko.common.mod.entities.NekoEntity
import org.cneko.toneko.common.mod.items.ToNekoItems
import org.cneko.toneko.common.mod.misc.mixininterface.SlowTickable
import com.geckolib.animatable.GeoEntity
import com.geckolib.animatable.instance.AnimatableInstanceCache
import com.geckolib.animatable.manager.AnimatableManager
import com.geckolib.animation.AnimationController
import com.geckolib.animation.AnimationController.AnimationStateHandler
import com.geckolib.animation.state.AnimationTest
import com.geckolib.animation.RawAnimation
import com.geckolib.constant.DefaultAnimations
import com.geckolib.util.GeckoLibUtil
import java.util.*

/*
我不敢想象，如果它真的活了过来
哇哦哇哦，那可得太爽了呀~
 */
class RodEntity(private val entityType:EntityType<RodEntity>, world: Level):TamableAnimal(entityType,world),GeoEntity,NeutralMob,
    Enemy, SlowTickable, IGeneticEntity, VisualSized {
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

    override fun getBreedOffspring(level: ServerLevel, partner: AgeableMob): AgeableMob {
        val baby = RodEntity(entityType, level)
        if (partner is IGeneticEntity) {
            val paternal = genome.createGamete(random)
            val maternal = partner.genome.createGamete(partner.random)
            baby.setGenome(Genome.combine(paternal, maternal, RodGenetics.KARYOTYPE))
        } else {
            val g1 = Genome.generateFallbackGamete(random, RodGenetics.KARYOTYPE)
            val g2 = Genome.generateFallbackGamete(random, RodGenetics.KARYOTYPE)
            baby.setGenome(Genome.combine(g1, g2, RodGenetics.KARYOTYPE))
        }
        baby.expressTraits()
        return baby
    }

    override fun isFood(stack: ItemStack): Boolean {
        return stack.`is`(Items.END_ROD)
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
            // 26.x：第四参数改为 TargetingConditions.Selector (test(LivingEntity, ServerLevel))
            addGoal(10, NearestAttackableTargetGoal(
                this@RodEntity,
                Monster::class.java,  // 主动攻击所有敌对生物
                true,
                TargetingConditions.Selector { _, _ -> true }
            ))

            // 修改后的玩家目标选择条件
            addGoal(1, NearestAttackableTargetGoal(
                this@RodEntity,
                Player::class.java,
                false,
                TargetingConditions.Selector { entity, _ ->
                    // 愤怒中的玩家作为目标
                    this@RodEntity.isAngry && this@RodEntity.getPersistentAngerTarget()?.getUUID() == entity.uuid
                }
            ))

            addGoal(5, ResetUniversalAngerTargetGoal(this@RodEntity, true)) // 通用愤怒机制
        }
    }

    // 26.x：doHurtTarget 需要 ServerLevel 参数
    override fun doHurtTarget(level: ServerLevel, target: Entity): Boolean {
        if (target is LivingEntity) {
            val intensity = getOrgasmIntensity()
            target.addEffect(MobEffectInstance(JREffects.ORGASM_EFFECT, (100 * intensity).toInt(), 0))
        }
        return super.doHurtTarget(level, target)
    }

    // GeckoLib 5：AnimationController 构造不再接收 animatable，状态类型为 AnimationTest
    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {
        controllers.add(AnimationController<RodEntity>("main", 20, AnimationStateHandler { state: AnimationTest<RodEntity> ->
            if (this.pose == Pose.SWIMMING && !this.isInLiquid) {
                return@AnimationStateHandler state.setAndContinue(DefaultAnimations.CRAWL)
            } else if (this.isInLiquid && this.isEyeInFluid(FluidTags.WATER)) {
                return@AnimationStateHandler if (state.isMoving) state.setAndContinue(DefaultAnimations.SWIM) else state.setAndContinue(
                    DefaultAnimations.CRAWL
                )
            } else if (!state.isMoving) {
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
            (firstPassenger as LivingEntity).addEffect(MobEffectInstance(JREffects.ORGASM_EFFECT, (100 * intensity).toInt(), 0))
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

    override fun mobInteract(player: Player, hand: InteractionHand): InteractionResult {
        val stack = player.getItemInHand(hand)
        if (!isTame && stack.`is`(Items.END_ROD)) {
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
            if (stack.`is`(JRBlocks.GOLDEN_LEAVES.asItem()) && health < maxHealth) {
                if (!level().isClientSide) {
                    heal(4.0f)
                    if (!player.isCreative) {
                        stack.shrink(1)
                    }
                }
                return InteractionResult.SUCCESS
            }
        }
        if (player.isHolding { i: ItemStack -> i.item == ToNekoItems.NEKO_POTION }) {
            if (!player.isCreative) {
                player.getItemInHand(hand).shrink(1)
                player.addItem(ItemStack(Items.GLASS_BOTTLE))
            }
            if (level() is ServerLevel) {
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

    // 26.x：handleDamageEvent 已移除，改在 hurtServer 中处理受击逻辑
    override fun hurtServer(level: ServerLevel, damageSource: DamageSource, amount: Float): Boolean {
        if (damageSource.entity is Player && !isOwnedBy(damageSource.entity as Player)) {
            this.angerTargetRef = EntityReference.of(damageSource.entity as LivingEntity)
            startPersistentAngerTimer()
        }
        if (damageSource.entity is NekoEntity) {
            // 变大
            this.getAttribute(Attributes.SCALE)?.let {
                it.baseValue = it.baseValue + 0.2
            }
            this.getAttribute(Attributes.MAX_HEALTH)?.let {
                it.baseValue = it.baseValue + 2.0
            }
            this.health = this.health + 2.0f
            this.angerTargetRef = EntityReference.of(damageSource.entity as LivingEntity)
        }
        return super.hurtServer(level, damageSource, amount)
    }

    // ========== 遗传学初始化（自然生成时分配随机基因） ==========
    // 26.x：MobSpawnType 更名为 EntitySpawnReason
    override fun finalizeSpawn(
        world: ServerLevelAccessor,
        difficulty: DifficultyInstance,
        reason: EntitySpawnReason,
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

    /**
     * 渲染尺寸（格）：供「缩进史莱姆体内」这类逻辑使用（见 [VisualSized] / SlimeContentSizing）。
     *
     * <p>GeckoLib 的渲染器不是 `LivingEntityRenderer`，量不到模型，而且模型本身还会
     * 按遗传学缩放（[RodRenderer] 里 `widthScale` 走 X/Z、`lengthScale` 走 Y），
     * 幼年再减半。碰撞箱只有 0.5 格，拿它当渲染尺寸会让杆子在体内捅出壳外。
     */
    override fun visualSize(): Float {
        val lengthScale = 1.0f + getLengthBonus()
        val widthScale = 1.0f + getWidthBonus()
        // geo/entity/rod.geo.json：底座 6×2×6 + 杆 2×17×2（像素）→ 高 19 像素 = 1.1875 格
        val height = ROD_MODEL_HEIGHT * lengthScale
        val width = ROD_MODEL_WIDTH * widthScale
        val babyScale = if (isBaby) 0.5f else 1.0f
        return maxOf(height, width) * babyScale
    }

    private fun tryTame(player: Player) {
        // 26.x：TamableAnimal 用 EntityReference 存储主人
        setTame(true, true)
        tame(player)
        this.angerTargetRef = null
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
        builder.define(LENGTH_BONUS, 0.0f)
        builder.define(WIDTH_BONUS, 0.0f)
        builder.define(ORGASM_INTENSITY, 1.0f)
    }

    // 26.x：实体存档改为 ValueInput/ValueOutput，NBT 数据块走 Codec
    override fun addAdditionalSaveData(out: ValueOutput) {
        super.addAdditionalSaveData(out)
        out.store("Genome", CompoundTag.CODEC, genome.save())
        out.store("GeneticData", CompoundTag.CODEC, geneticData)
    }

    override fun readAdditionalSaveData(input: ValueInput) {
        super.readAdditionalSaveData(input)
        input.read("Genome", CompoundTag.CODEC).ifPresent { genome.load(it) }
        input.read("GeneticData", CompoundTag.CODEC).ifPresent { loaded ->
            // 26.x：CompoundTag#getAllKeys -> keySet，get(String) 直接返回 Tag
            for (key in loaded.keySet()) {
                loaded.get(key)?.let { value -> geneticData.put(key, value) }
            }
        }
        expressTraits()
    }

    // 26.x：NeutralMob 不再由 Mob 实现，RodEntity 需自行实现“愤怒结束时间”模型。
    private var angerEndTime: Long = NeutralMob.NO_ANGER_END_TIME
    private var angerTargetRef: EntityReference<LivingEntity>? = null

    override fun getPersistentAngerEndTime(): Long = angerEndTime
    override fun setPersistentAngerEndTime(endTime: Long) { angerEndTime = endTime }
    override fun getPersistentAngerTarget(): EntityReference<LivingEntity>? = angerTargetRef
    override fun setPersistentAngerTarget(persistentAngerTarget: EntityReference<LivingEntity>?) { angerTargetRef = persistentAngerTarget }
    override fun startPersistentAngerTimer() {
        this.setPersistentAngerEndTime(this.level().gameTime + ANGER_TIME_RANGE.sample(this.random))
    }

    /** 愤怒计时推进（原 tickPersistentAnger，改用 NeutralMob 默认逻辑） */
    fun tickPersistentAnger() {
        if (level().isClientSide) return
        val lvl = level()
        if (lvl is ServerLevel) {
            this.updatePersistentAnger(lvl, true)
        }
    }


    companion object{
        /** 渲染尺寸用：geo/entity/rod.geo.json 的杆高 19 像素（底座 2 + 杆 17） */
        private const val ROD_MODEL_HEIGHT = 19.0f / 16.0f
        /** 渲染尺寸用：底座宽 6 像素（杆只有 2 像素，取大者） */
        private const val ROD_MODEL_WIDTH = 6.0f / 16.0f

        fun createRodAttribute():AttributeSupplier.Builder{
            return createMobAttributes().add(Attributes.ATTACK_DAMAGE,4.0)
        }
        private val ANGER_TIME_RANGE = TimeUtil.rangeOfSeconds(20, 39)

        // 遗传学同步数据
        private val LENGTH_BONUS = SynchedEntityData.defineId(RodEntity::class.java, EntityDataSerializers.FLOAT)
        private val WIDTH_BONUS = SynchedEntityData.defineId(RodEntity::class.java, EntityDataSerializers.FLOAT)
        private val ORGASM_INTENSITY = SynchedEntityData.defineId(RodEntity::class.java, EntityDataSerializers.FLOAT)
    }
}