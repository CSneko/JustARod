package org.cneko.justarod.quirks

import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.world.World
import org.cneko.justarod.effect.JREffects
import org.cneko.justarod.item.addEffect
import org.cneko.toneko.common.mod.entities.INeko
import org.cneko.toneko.common.mod.quirks.Quirk

// 不好吃
class CCBQuirk() : JRDefaultQuirk("ccb") {
    override fun getInteractionValue(): Int {
        return 1
    }


    override fun onInteractionOther(
        player: PlayerEntity?,
        level: World?,
        hand: Hand?,
        other: INeko?,
        hitResult: EntityHitResult?
    ): ActionResult {
        val entity = other?.entity ?: return ActionResult.FAIL
        if (entity.getEquippedStack(EquipmentSlot.LEGS)?.isEmpty == false){
            return ActionResult.FAIL
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
            if (player is ServerPlayerEntity){
                player.sendMessage(Text.translatable("quirk.toneko.ccb.user.success", entity.name).formatted(Formatting.LIGHT_PURPLE),true)
            }
            if (entity is ServerPlayerEntity){
                entity.sendMessage(Text.translatable("quirk.toneko.ccb.neko.success", player?.name).formatted(Formatting.LIGHT_PURPLE),true)
            }
        }
        return super.onInteractionOther(player, level, hand, other, hitResult)
    }


}