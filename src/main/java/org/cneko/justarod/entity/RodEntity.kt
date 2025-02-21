package org.cneko.justarod.entity

import net.minecraft.entity.EntityType
import net.minecraft.entity.attribute.DefaultAttributeContainer
import net.minecraft.entity.mob.MobEntity
import net.minecraft.entity.passive.AnimalEntity
import net.minecraft.entity.passive.PassiveEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
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
}