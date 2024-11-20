package org.cneko.justarod.item

import net.minecraft.component.type.FoodComponent
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.passive.FoxEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent
import net.minecraft.sound.SoundEvents
import net.minecraft.util.math.MathHelper
import net.minecraft.world.World
import net.minecraft.world.event.GameEvent
import net.minecraft.world.event.GameEvent.Emitter

/*
其实用过之后味道是有点酸的，也会有点咸咸的，你要喜欢可以舔舔，虽然可能有点细菌，不过不是不能接受
 */
class EatableRodItem: SelfUsedItem(Settings().food(FoodComponent.Builder().nutrition(1).saturationModifier(0.2f).build()).component(JRComponents.USED_TIME_MARK, 0).maxDamage(200)){
    override fun finishUsing(stack: ItemStack?, world: World, user: LivingEntity): ItemStack {
        val itemStack = super.finishUsing(stack, world, user)
        if (!world.isClient) {
            for (i in 0..15) {
                val d = user.x + (user.random.nextDouble() - 0.5) * 16.0
                val e = MathHelper.clamp(
                    user.y + (user.random.nextInt(16) - 8).toDouble(), world.bottomY.toDouble(),
                    (world.bottomY + (world as ServerWorld).logicalHeight - 1).toDouble()
                )
                val f = user.z + (user.random.nextDouble() - 0.5) * 16.0
                if (user.hasVehicle()) {
                    user.stopRiding()
                }

                val vec3d = user.pos
                if (user.teleport(d, e, f, true)) {
                    world.emitGameEvent(GameEvent.TELEPORT, vec3d, Emitter.of(user))
                    val soundCategory: SoundCategory
                    val soundEvent: SoundEvent
                    if (user is FoxEntity) {
                        soundEvent = SoundEvents.ENTITY_FOX_TELEPORT
                        soundCategory = SoundCategory.NEUTRAL
                    } else {
                        soundEvent = SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT
                        soundCategory = SoundCategory.PLAYERS
                    }

                    world.playSound(null as PlayerEntity?, user.x, user.y, user.z, soundEvent, soundCategory)
                    user.onLanding()
                    break
                }
            }

            if (user is PlayerEntity) {
                val playerEntity = user
                playerEntity.clearCurrentExplosion()
                playerEntity.itemCooldownManager[this] = 20
            }
        }

        return itemStack
    }
}