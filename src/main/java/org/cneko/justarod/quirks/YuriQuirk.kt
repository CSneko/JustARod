package org.cneko.justarod.quirks

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.world.World
import org.cneko.justarod.effect.JREffects
import org.cneko.justarod.entity.Pregnant
import org.cneko.justarod.item.rod.addEffect
import org.cneko.toneko.common.mod.entities.INeko

class YuriQuirk : JRDefaultQuirk("yuri") {

    // 贴贴羁绊值稍微给高一点
    override fun getInteractionValue(): Int {
        return 2
    }

    override fun onInteractionOther(
        player: PlayerEntity?,
        level: World?,
        hand: Hand?,
        other: INeko?,
        hitResult: EntityHitResult?
    ): ActionResult {
        val entity = other?.entity ?: return ActionResult.FAIL
        if (player == null || level == null) return ActionResult.FAIL

        // 1. 检查双方是否都实现了 Pregnant 接口
        val playerPregnant = player as? Pregnant ?: return ActionResult.PASS
        val nekoPregnant = entity as? Pregnant ?: return ActionResult.PASS

        // 2. 核心判定：双方必须都是百合
        if (!playerPregnant.isYuri() || !nekoPregnant.isYuri()) {
            return ActionResult.FAIL
        }

        // 仅在服务端执行核心逻辑
        if (!level.isClient && level is ServerWorld) {

            // 3. 生成爱心粒子，表现百合贴贴的气氛
            level.spawnParticles(
                ParticleTypes.HEART,
                entity.x, entity.y + 1.0, entity.z,
                5, 0.5, 0.5, 0.5, 0.1
            )
            level.spawnParticles(
                ParticleTypes.HEART,
                player.x, player.y + 1.0, player.z,
                5, 0.5, 0.5, 0.5, 0.1
            )

            // 4. 赋予百合花香效果 (Lily Pheromone) - 持续 5 秒
            entity.addEffect(JREffects.LILY_PHEROMONE_EFFECT, 20 * 5, 0)
            player.addEffect(JREffects.LILY_PHEROMONE_EFFECT, 20 * 5, 0)


            // 6. 发送 ActionBar 提示文本
            if (player is ServerPlayerEntity) {
                val msgKey = "你与 ${entity.name.string} 贴贴了!"
                player.sendMessage(Text.literal(msgKey).formatted(Formatting.LIGHT_PURPLE), true)
            }

            if (entity is ServerPlayerEntity) {
                val msgKey = "你与 ${player.name.string} 贴贴了!"
                entity.sendMessage(Text.literal(msgKey).formatted(Formatting.LIGHT_PURPLE), true)
            }
        }

        return super.onInteractionOther(player, level, hand, other, hitResult)
    }
}