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
import net.minecraft.nbt.NbtCompound
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.world.World
import org.cneko.toneko.common.mod.items.ToNekoItems
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

    override fun interactMob(player: PlayerEntity?, hand: Hand?): ActionResult {
        // 如果是猫娘药水
        if (player?.isHolding{
                i -> i.item == ToNekoItems.NEKO_POTION
            } == true){
            // 减少一瓶并给予空瓶
            if (!player.isCreative) {
                player.getStackInHand(hand!!).decrement(1)
                player.giveItemStack(ItemStack(Items.GLASS_BOTTLE))
            }
            // 把末地烛remove并生成一只猫娘
            if (world is ServerWorld) {
                world as ServerWorld
                this.remove(RemovalReason.DISCARDED)
                val neko = SeeeeexNekoEntity(JREntities.SEEEEEX_NEKO, world)
                neko.setPos(this.x, this.y, this.z)
                neko.sexualDesire = 200
                world.spawnEntity(neko)
                // 如果有名字的话
                if (this.hasCustomName()){
                    neko.customName = this.customName
                }
            }
        }
        return super.interactMob(player, hand)
    }
}