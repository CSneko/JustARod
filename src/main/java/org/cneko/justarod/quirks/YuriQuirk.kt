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
        val entity = other?.entity

        if (entity == null || player == null || level == null || hand == null) {
            return super.onInteractionOther(player, level, hand, other, hitResult)
        }

        // 1. 如果是副手，直接给父类处理，不触发贴贴
        if (hand != Hand.MAIN_HAND) {
            return super.onInteractionOther(player, level, hand, other, hitResult)
        }

        // 2. 如果手里拿着物品，也直接交给父类，保证物品交互能正常触发
        if (!player.getStackInHand(hand).isEmpty) {
            return super.onInteractionOther(player, level, hand, other, hitResult)
        }

        // —— 走到这里，说明是【主手】且是【空手】 ——

        val playerPregnant = player as? Pregnant
        val nekoPregnant = entity as? Pregnant

        // 3. 核心判定：双方必须都是百合
        if (playerPregnant != null && nekoPregnant != null && playerPregnant.isYuri() && nekoPregnant.isYuri()) {

            // 仅在服务端执行核心逻辑
            if (!level.isClient && level is ServerWorld) {
                // 播粒子
                level.spawnParticles(ParticleTypes.HEART, entity.x, entity.y + 1.0, entity.z, 5, 0.5, 0.5, 0.5, 0.1)
                level.spawnParticles(ParticleTypes.HEART, player.x, player.y + 1.0, player.z, 5, 0.5, 0.5, 0.5, 0.1)

                // 给BUFF
                entity.addEffect(JREffects.LILY_PHEROMONE_EFFECT, 20 * 5, 0)
                player.addEffect(JREffects.LILY_PHEROMONE_EFFECT, 20 * 5, 0)

                // 发送提示文本
                if (player is ServerPlayerEntity) {
                    val msgKey = "你与 ${entity.name.string} 贴贴了!"
                    player.sendMessage(Text.literal(msgKey).formatted(Formatting.LIGHT_PURPLE), true)
                }
                if (entity is ServerPlayerEntity) {
                    val msgKey = "你与 ${player.name.string} 贴贴了!"
                    entity.sendMessage(Text.literal(msgKey).formatted(Formatting.LIGHT_PURPLE), true)
                }
            }

            return ActionResult.SUCCESS
        }

        // 4. 不满足百合条件，交给父类兜底
        return super.onInteractionOther(player, level, hand, other, hitResult)
    }
}