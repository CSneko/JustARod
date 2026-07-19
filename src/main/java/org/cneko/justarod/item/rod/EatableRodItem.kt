package org.cneko.justarod.item.rod

import net.minecraft.component.type.FoodComponent
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.animal.Fox
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.util.Mth
import net.minecraft.world.level.Level
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.level.gameevent.GameEvent.Emitter
import org.cneko.justarod.item.JRComponents

/*
其实用过之后味道是有点酸的，也会有点咸咸的，你要喜欢可以舔舔，虽然可能有点细菌，不过不是不能接受
（自己的就算了，毕竟... 不太好吃... 甚至有点难以下口）
 */
class EatableRodItem: SelfUsedItem(Settings().food(FoodComponent.Builder().nutrition(1).saturationModifier(0.2f).build()).component(
    JRComponents.Companion.USED_TIME_MARK, 0).maxDamage(200)){
    override fun finishUsing(stack: ItemStack?, world: Level, user: LivingEntity): ItemStack {
        val itemStack = super.finishUsing(stack, world, user)
        if (!level().isClientSide) {
            for (i in 0..15) {
                val d = user.x + (user.random.nextDouble() - 0.5) * 16.0
                val e = Mth.clamp(
                    user.y + (user.random.nextInt(16) - 8).toDouble(), level().bottomY.toDouble(),
                    (level().bottomY + (level() as ServerLevel).logicalHeight - 1).toDouble()
                )
                val f = user.z + (user.random.nextDouble() - 0.5) * 16.0
                if (user.hasVehicle()) {
                    user.stopRiding()
                }

                val vec3d = user.pos
                if (user.teleport(d, e, f, true)) {
                    level().emitGameEvent(GameEvent.TELEPORT, vec3d, Emitter.of(user))
                    val soundCategory: SoundSource
                    val soundEvent: SoundEvent
                    if (user is Fox) {
                        soundEvent = SoundEvents.ENTITY_FOX_TELEPORT
                        soundCategory = SoundSource.NEUTRAL
                    } else {
                        soundEvent = SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT
                        soundCategory = SoundSource.PLAYERS
                    }

                    level().playSound(null as Player?, user.x, user.y, user.z, soundEvent, soundCategory)
                    user.checkFallDamage()
                    break
                }
            }

            if (user is Player) {
                val playerEntity = user
                playerEntity.clearCurrentExplosion()
                playerEntity.itemCooldownManager[this] = 20
            }
        }

        return itemStack
    }
}