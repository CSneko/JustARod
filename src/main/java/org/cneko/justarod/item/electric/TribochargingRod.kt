package org.cneko.justarod.item.electric

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.level.Level
import org.cneko.justarod.packet.FrictionPayload

class TribochargingRod(properties: Properties) : Item(properties){
    override fun use(world: Level?, user: Player?, hand: InteractionHand?): InteractionResultHolder<ItemStack> {
        // 打开屏幕
        if (user is ServerPlayer)
        ServerPlayNetworking.send(user, FrictionPayload("a"))
        return super.use(world, user, hand)
    }
}