package org.cneko.justarod.entity

import net.minecraft.entity.EntityType
import net.minecraft.entity.ai.goal.*
import net.minecraft.entity.attribute.DefaultAttributeContainer
import net.minecraft.entity.mob.MobEntity
import net.minecraft.entity.passive.AnimalEntity
import net.minecraft.entity.passive.PassiveEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.world.ServerWorld
import net.minecraft.world.World
import software.bernie.geckolib.animatable.GeoEntity
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache
import software.bernie.geckolib.animation.AnimatableManager
import software.bernie.geckolib.util.GeckoLibUtil

class RodEntity(private val entityType:EntityType<RodEntity>, world: World):AnimalEntity(entityType,world),GeoEntity {
    private val animCache: AnimatableInstanceCache = GeckoLibUtil.createInstanceCache(this)
    override fun createChild(world: ServerWorld?, entity: PassiveEntity?): PassiveEntity {
        val baby = RodEntity(entityType, world!!)
        return baby
    }

    override fun isBreedingItem(stack: ItemStack?): Boolean {
        return stack?.isOf(Items.END_ROD) == true
    }

    override fun initGoals() {
        super.initGoals()
        goalSelector.add(3, AnimalMateGoal(this, 0.1))
        goalSelector.add(5, FollowParentGoal(this, 0.1))
        goalSelector.add(6, WanderAroundFarGoal(this, 0.1))
        goalSelector.add(
            7, LookAtEntityGoal(
                this,
                PlayerEntity::class.java, 6.0f
            )
        )
        goalSelector.add(8, LookAroundGoal(this))
    }

    companion object{
        fun createRodAttribute():DefaultAttributeContainer.Builder{
            return MobEntity.createMobAttributes()
        }
    }

    override fun registerControllers(controllers: AnimatableManager.ControllerRegistrar?) {
    }

    override fun getAnimatableInstanceCache(): AnimatableInstanceCache {
        return animCache
    }

    override fun tick() {
        super.tick()
        // 10%的概率生成末地烛粒子
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
    }
}