package org.cneko.justarod.item.electric

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.world.World
import org.cneko.justarod.packet.FrictionPayload

class TribochargingRod(settings: Settings) : Item(settings){
    override fun use(world: World?, user: PlayerEntity?, hand: Hand?): TypedActionResult<ItemStack> {
        // 打开屏幕
        if (user is ServerPlayerEntity)
        ServerPlayNetworking.send(user, FrictionPayload("a"))
        return super.use(world, user, hand)
    }
}