package org.cneko.justarod.item.rod

import net.minecraft.world.food.FoodProperties
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
import org.cneko.justarod.item.JRComponents

/*
其实用过之后味道是有点酸的，也会有点咸咸的，你要喜欢可以舔舔，虽然可能有点细菌，不过不是不能接受
（自己的就算了，毕竟... 不太好吃... 甚至有点难以下口）
 */
class EatableRodItem: SelfUsedItem(Properties().food(FoodProperties.Builder().nutrition(1).saturationModifier(0.2f).build()).component(
    JRComponents.Companion.USED_TIME_MARK, 0).durability(200)){
    override fun finishUsingItem(stack: ItemStack?, world: Level, user: LivingEntity): ItemStack {
        val itemStack = super.finishUsingItem(stack, world, user)
        if (!world.isClientSide) {
            for (i in 0..15) {
                val d = user.x + (user.random.nextDouble() - 0.5) * 16.0
                val e = Mth.clamp(
                    user.y + (user.random.nextInt(16) - 8).toDouble(), world.minBuildHeight.toDouble(),
                    (world.minBuildHeight + (world as ServerLevel).logicalHeight - 1).toDouble()
                )
                val f = user.z + (user.random.nextDouble() - 0.5) * 16.0
                if (user.isPassenger()) {
                    user.stopRiding()
                }

                val vec3d = user.position()
                if (user.randomTeleport(d, e, f, true)) {
                    val soundCategory: SoundSource
                    val soundEvent: SoundEvent
                    if (user is Fox) {
                        soundEvent = SoundEvents.FOX_TELEPORT
                        soundCategory = SoundSource.NEUTRAL
                    } else {
                        soundEvent = SoundEvents.CHORUS_FRUIT_TELEPORT
                        soundCategory = SoundSource.PLAYERS
                    }

                    world.playSound(null as Player?, user.x, user.y, user.z, soundEvent, soundCategory)
                    user.resetFallDistance()
                    break
                }
            }

            if (user is Player) {
                val playerEntity = user
                playerEntity.cooldowns.addCooldown(this, 20)
            }
        }

        return itemStack
    }
}