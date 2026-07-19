package org.cneko.justarod.quirks

import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Player
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerPlayer
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.ChatFormatting
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.level.Level
import org.cneko.justarod.effect.JREffects
import org.cneko.justarod.item.rod.addEffect
import org.cneko.toneko.common.mod.entities.INeko

// 不好吃
class CCBQuirk() : JRDefaultQuirk("ccb") {
    override fun getInteractionValue(): Int {
        return 1
    }


    override fun onInteractionOther(
        player: Player?,
        level: Level?,
        hand: InteractionHand?,
        other: INeko?,
        hitResult: EntityHitResult?
    ): InteractionResult {
        val entity = other?.entity ?: return InteractionResult.FAIL
        if (entity.getEquippedStack(EquipmentSlot.LEGS)?.isEmpty == false){
            return InteractionResult.FAIL
        }else{
            // 创建水滴粒子
            val random = level?.random
            if (random?.nextInt(5) == 0) {
                level.addParticle(
                    ParticleTypes.RAIN,
                    entity.x,
                    entity.y,
                    entity.z,
                    0.0,
                    0.01,
                    0.0
                )
            }
            // 1/10给予对方1s的高超效果
            if (random?.nextInt(10) == 0) {
                entity.addEffect(JREffects.ORGASM_EFFECT, 20, 0)
            }
            // 显示提示
            if (player is ServerPlayer){
                player.sendSystemMessage(Component.translatable("quirk.toneko.ccb.user.success", entity.name).withStyle(ChatFormatting.LIGHT_PURPLE),true)
            }
            if (entity is ServerPlayer){
                entity.sendSystemMessage(Component.translatable("quirk.toneko.ccb.neko.success", player?.name).withStyle(ChatFormatting.LIGHT_PURPLE),true)
            }
        }
        return super.onInteractionOther(player, level, hand, other, hitResult)
    }


}