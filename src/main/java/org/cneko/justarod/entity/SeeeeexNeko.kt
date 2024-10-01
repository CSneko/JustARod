package org.cneko.justarod.entity

import net.minecraft.entity.EntityPose
import net.minecraft.entity.EntityType
import net.minecraft.registry.tag.FluidTags
import net.minecraft.server.world.ServerWorld
import net.minecraft.world.World
import org.cneko.toneko.common.mod.entities.INeko
import org.cneko.toneko.fabric.entities.NekoEntity
import software.bernie.geckolib.animation.*
import software.bernie.geckolib.animation.AnimationController.AnimationStateHandler
import software.bernie.geckolib.constant.DefaultAnimations

open class SeeeeexNeko(private val type: EntityType<SeeeeexNeko>, val world: World): NekoEntity(type, world) {
    companion object{

    }
    var isMasturbation = false
    override fun getBreedOffspring(world: ServerWorld?, neko: INeko?): NekoEntity? {
        return world?.let { SeeeeexNeko(this.type, it) }
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

    override fun getSkin(): String {
        return "" // TODO You
    }

}