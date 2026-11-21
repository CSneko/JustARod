package org.cneko.justarod.item.medical

import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import org.cneko.justarod.ascaris.AscarisEntity
import org.cneko.justarod.ascaris.AscarisHostController
import org.cneko.justarod.ascaris.AscarisPoseState
import org.cneko.justarod.sound.JRSounds

/**
 * 蛔虫小金铃：
 * - 空地右键：摇响清脆金铃，召回 64 格内自己驯服的粉白蛔虫娘并触发挥手打招呼（若附近无体外个体且体内有寄宿，直接唤出）；
 * - 空地 Shift+右键：温柔将体内寄宿的粉白蛔虫娘唤出体外；
 * - 对着已驯服的粉白蛔虫娘右键：触发/取消双人尾巴缠绕；Shift+右键：让她钻回你的体内共生寄宿！
 */
class AscarisBellItem(properties: Properties) : Item(properties) {

    override fun use(world: Level, user: Player, hand: InteractionHand): InteractionResult {
        if (!world.isClientSide && world is ServerLevel) {
            if (user is ServerPlayer && user.isShiftKeyDown) {
                if (AscarisHostController.hasHostedAscaris(user)) {
                    AscarisHostController.summonFromHostBody(user, false, false)
                    user.cooldowns.addCooldown(user.getItemInHand(hand), 20)
                    return InteractionResult.SUCCESS
                }
            }
            world.playSound(null, user.blockPosition(), JRSounds.ASCARIS_BELL, SoundSource.PLAYERS, 0.9f, 1.15f)
            val nearby = world.getEntitiesOfClass(AscarisEntity::class.java, user.boundingBox.inflate(64.0)) {
                it.isAlive && it.isTame && it.isOwnedBy(user)
            }
            if (nearby.isEmpty()) {
                if (user is ServerPlayer && AscarisHostController.hasHostedAscaris(user)) {
                    AscarisHostController.summonFromHostBody(user, false, false)
                } else if (user is ServerPlayer) {
                    user.sendOverlayMessage(Component.literal("§7叮铃~ 附近 64 格内没有回应你的粉白蛔虫娘。"))
                }
            } else {
                var called = 0
                for (a in nearby) {
                    if (a.poseState == AscarisPoseState.TAIL_WRAP) {
                        a.releaseTailWrap(false)
                    }
                    a.isOrderedToSit = false
                    if (a.distanceToSqr(user) > 100.0) {
                        a.teleportTo(user.x + 1.1, user.y, user.z + 1.1)
                    }
                    a.playTransientPose(AscarisPoseState.WAVE, 56)
                    if (called == 0) {
                        a.playVoice(JRSounds.ASCARIS_VOICE_GREET, 0.9f, 36, true)
                    }
                    world.sendParticles(ParticleTypes.HEART, a.x, a.y + 1.2, a.z, 5, 0.3, 0.2, 0.3, 0.02)
                    called++
                }
                if (user is ServerPlayer) {
                    user.sendOverlayMessage(Component.literal("§d叮铃~ 召回了 $called 只粉白蛔虫娘！（对她右键可发动贴贴缠绕）"))
                }
            }
            user.cooldowns.addCooldown(user.getItemInHand(hand), 20)
        }
        return InteractionResult.SUCCESS
    }
}
