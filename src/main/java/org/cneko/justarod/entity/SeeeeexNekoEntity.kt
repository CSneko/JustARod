package org.cneko.justarod.entity

import net.minecraft.entity.EntityPose
import net.minecraft.entity.EntityType
import net.minecraft.entity.data.DataTracker
import net.minecraft.entity.data.TrackedData
import net.minecraft.entity.data.TrackedDataHandlerRegistry
import net.minecraft.registry.tag.FluidTags
import net.minecraft.server.world.ServerWorld
import net.minecraft.world.World
import org.cneko.justarod.entity.ai.SexualIntercourseGoal
import org.cneko.toneko.common.mod.entities.INeko
import org.cneko.toneko.common.mod.entities.NekoEntity
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.animation.AnimationController
import software.bernie.geckolib.animation.PlayState
import software.bernie.geckolib.animation.RawAnimation
import software.bernie.geckolib.constant.DefaultAnimations

/*
小猫娘~~ 可可爱爱
 */
open class SeeeeexNekoEntity(private val type: EntityType<SeeeeexNekoEntity>, world: World): NekoEntity(type, world) {
    companion object{
        val SKIN:String = "shiuri_neko"
        val SEXUAL_DESIRE_ID:TrackedData<Int> = DataTracker.registerData(SeeeeexNekoEntity::class.java, TrackedDataHandlerRegistry.INTEGER)
    }
    var isMasturbation = false

    override fun getBreedOffspring(world: ServerWorld?, neko: INeko?): NekoEntity? {
        return world?.let { SeeeeexNekoEntity(this.type, it) }
    }

    override fun initGoals() {
        super.initGoals()
        this.goalSelector.add(5, SexualIntercourseGoal(this))
    }

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar?) {
        controllers?.add(AnimationController(this, 20) { state ->
            when {
                this.isMasturbation -> state.setAndContinue(RawAnimation.begin().thenLoop("jr.mb"))
                this.pose == EntityPose.SWIMMING && !this.isInFluid -> state.setAndContinue(DefaultAnimations.CRAWL)
                this.isInFluid && this.isSubmergedIn(FluidTags.WATER) ->
                    if (state.isMoving) state.setAndContinue(DefaultAnimations.SWIM)
                    else state.setAndContinue(DefaultAnimations.CRAWL)
                !state.isMoving ->
                    if (this.isSitting) state.setAndContinue(RawAnimation.begin().thenLoop("misc.sit"))
                    else state.setAndContinue(DefaultAnimations.IDLE)
                state.isMoving ->
                    if (this.velocity.length() > 0.2) state.setAndContinue(DefaultAnimations.RUN)
                    else state.setAndContinue(DefaultAnimations.WALK)
                else -> PlayState.CONTINUE
            }
        })
    }

    fun getSexualDesire(): Int {
        return this.dataTracker.get(SEXUAL_DESIRE_ID)
    }
    fun setSexualDesire(desire: Int) {
        this.dataTracker.set(SEXUAL_DESIRE_ID, desire)
    }

    override fun baseTick() {
        super.baseTick()
        if (!world.isClient()) {
            setSexualDesire(getSexualDesire())
        }
    }

    override fun getSkin(): String {
        return SKIN
    }

    override fun getRandomSkin(): String {
        return SKIN
    }

}