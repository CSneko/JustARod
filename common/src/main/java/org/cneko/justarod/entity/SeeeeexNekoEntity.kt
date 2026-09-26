package org.cneko.justarod.entity

import net.minecraft.world.entity.Pose
import net.minecraft.world.entity.EntityType
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.tags.FluidTags
import net.minecraft.server.level.ServerLevel
import net.minecraft.network.chat.Component
import net.minecraft.world.level.Level
import org.cneko.justarod.effect.JREffects
import org.cneko.justarod.entity.ai.SexualIntercourseGoal
import org.cneko.justarod.entity.ai.SuckMilkGoal
import org.cneko.justarod.item.JRItems.Companion.BYT
import org.cneko.toneko.common.mod.api.NekoLevelRegistry
import org.cneko.toneko.common.mod.entities.INeko
import org.cneko.toneko.common.mod.entities.NekoEntity
import com.geckolib.animatable.manager.AnimatableManager
import com.geckolib.animation.AnimationController
import com.geckolib.animation.`object`.PlayState
import com.geckolib.animation.RawAnimation
import com.geckolib.animation.state.AnimationTest
import com.geckolib.constant.DefaultAnimations
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import java.util.function.Predicate

/*
小猫娘~~ 可可爱爱
 */
open class SeeeeexNekoEntity(private val type: EntityType<SeeeeexNekoEntity>, world: Level): NekoEntity(type, world),Sexual {
    companion object{
        const val SKIN:String = "shiuri_neko"
        val SEXUAL_DESIRE_ID:EntityDataAccessor<Int> = SynchedEntityData.defineId(SeeeeexNekoEntity::class.java, EntityDataSerializers.INT)

        // 全局开关：是否允许自动和其他Neko交配
        var AUTO_MATE_WITH_NEKO = false
    }
    var isMasturbation = false
    var sexualIntercourseGoal: SexualIntercourseGoal? = null
    var suckMilkGoal: SuckMilkGoal? = null

    override fun getBreedOffspring(world: ServerLevel, neko: INeko): NekoEntity? {
        return SeeeeexNekoEntity(this.type, world)
    }

    override fun registerGoals() {
        super.registerGoals()
        sexualIntercourseGoal = SexualIntercourseGoal(this)
        this.goalSelector.addGoal(5, sexualIntercourseGoal!!)
        suckMilkGoal = SuckMilkGoal(this)
        this.goalSelector.addGoal(5, suckMilkGoal!!)
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(SEXUAL_DESIRE_ID, 20)
    }

    override fun canMate(other: INeko?): Boolean {
        if (other is Pregnant && !other.canPregnant()){
            return false
        }
        return super.canMate(other) && this.sexualDesire >= 40
    }

    override fun breed(level: ServerLevel, mate: INeko) {
        if (mate is Pregnant){
            if (mate.entity is net.minecraft.world.entity.player.Player){
                val player = mate.entity as net.minecraft.world.entity.player.Player
                // 26.x：Inventory#offhand 字段移除，改用 getOffhandItem()
                if (player.offhandItem.`is`(BYT)
                ){
                    NekoLevelRegistry.base().addRaw(this, 0.1)
                    NekoLevelRegistry.base().addRaw(mate, 0.1)
                    this.addEffect(MobEffectInstance(MobEffects.WEAKNESS, 3000, 0))
                    player.addEffect(MobEffectInstance(MobEffects.WEAKNESS, 3000, 0))
                    player.sendSystemMessage(Component.literal("§b你没有怀孕！"))
                    player.sendSystemMessage(Component.literal("§d好感度+10，等级+0.1"))
                    return
                }
            }
            // 怀孕10天
            mate.tryPregnant()
            mate.setBabyCount(mate.calculateBabyCount(this))
            NekoLevelRegistry.base().addRaw(this, 0.1)
            NekoLevelRegistry.base().addRaw(mate, 0.1)
            this.addEffect(MobEffectInstance(MobEffects.WEAKNESS, 3000, 0))
            (mate.entity as? net.minecraft.world.entity.player.Player)?.sendSystemMessage(Component.literal("§a你怀孕了！"))
            // 获取自己的效果
            val effects = this.activeEffects.filter { !it.effect.value().isBeneficial }
            if (effects.isNotEmpty()) {
                // 添加到对方的
                for (effect in effects) {
                    mate.entity.addEffect(effect)
                }
            }
            // 如果自己有艾滋，就添加到对方
            if (effects.stream().anyMatch { it -> it.effect.value().equals(JREffects.AIDS_EFFECT) }){
                if (mate.getAids() <= 0){
                    mate.setAids(1)
                }
            }
            // 如果有HPV，对方无免疫就添加
            if (effects.stream().anyMatch { it -> it.effect.value().equals(JREffects.HPV_EFFECT) }){
                if (!mate.isImmune2HPV()){
                    mate.setImmune2HPV(true)
                }
            }
        }else{
            super.breed(level, mate)
        }
    }


    // 26.x：实体存档改为 ValueInput/ValueOutput
    override fun addAdditionalSaveData(out: ValueOutput) {
        super.addAdditionalSaveData(out)
        if (this.sexualDesire > 0) {
            out.putInt("SexualDesire", this.sexualDesire)
        }
    }
    override fun readAdditionalSaveData(input: ValueInput) {
        super.readAdditionalSaveData(input)
        input.getInt("SexualDesire").ifPresent { this.sexualDesire = it }
    }

    override fun afterMate() {
        super.afterMate()
        this.decreaseSexualDesire(30)
    }

    // GeckoLib 5：AnimationController 构造不再接收 animatable，状态类型为 AnimationTest
    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar) {
        controllers.add(AnimationController<SeeeeexNekoEntity>("main", 20) { state: AnimationTest<SeeeeexNekoEntity> ->
            when {
                this.isMasturbation -> state.setAndContinue(RawAnimation.begin().thenLoop("jr.mb"))
                this.pose == Pose.SWIMMING && !this.isInLiquid -> state.setAndContinue(DefaultAnimations.CRAWL)
                this.isInLiquid && this.isEyeInFluid(FluidTags.WATER) ->
                    if (state.isMoving) state.setAndContinue(DefaultAnimations.SWIM)
                    else state.setAndContinue(DefaultAnimations.CRAWL)
                !state.isMoving ->
                    if (this.isSitting) state.setAndContinue(RawAnimation.begin().thenLoop("misc.sit"))
                    else state.setAndContinue(DefaultAnimations.IDLE)
                state.isMoving ->
                    if (this.getDeltaMovement().length() > 0.2) state.setAndContinue(DefaultAnimations.RUN)
                    else state.setAndContinue(DefaultAnimations.WALK)
                else -> PlayState.CONTINUE
            }
        })
    }

    override fun getSexualDesire(): Int {
        return this.entityData.get(SEXUAL_DESIRE_ID)
    }
    override fun setSexualDesire(desire: Int) {
        this.entityData.set(SEXUAL_DESIRE_ID, desire)
    }

    override fun slowTick(){
        super.slowTick()
        if (!level().isClientSide()) {
            sexualDesire = sexualDesire
            sexualIntercourseGoal?.slowTick()
            this.sexualSlowTick(this)
        }
    }

    override fun getSkin(): String {
        return SKIN
    }

    override fun getRandomSkin(): String {
        return SKIN
    }
}