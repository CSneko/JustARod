package org.cneko.justarod.item.medical

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import org.cneko.justarod.client.gui.ScanType
import org.cneko.justarod.entity.Pregnant
import org.cneko.justarod.packet.XRayScanScreenPayload

/**
 * 腹部超声仪：右键扫描自己或对其他玩家右键，打开实时动态腹部 B 超界面。
 */
class AbdomenScannerItem(properties: Properties) : Item(properties) {

    override fun use(world: Level, user: Player, hand: InteractionHand): InteractionResult {
        if (!world.isClientSide && user is ServerPlayer) {
            world.playSound(null, user.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5f, 1.4f)
            ServerPlayNetworking.send(user, XRayScanScreenPayload(ScanType.ABDOMEN, user.id))
        }
        return InteractionResult.SUCCESS
    }

    override fun interactLivingEntity(stack: ItemStack, user: Player, entity: LivingEntity, hand: InteractionHand): InteractionResult {
        if (!user.level().isClientSide && user is ServerPlayer && entity is Pregnant) {
            user.level().playSound(null, entity.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5f, 1.4f)
            ServerPlayNetworking.send(user, XRayScanScreenPayload(ScanType.ABDOMEN, entity.id))
            return InteractionResult.SUCCESS
        }
        return InteractionResult.PASS
    }
}
