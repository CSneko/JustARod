package org.cneko.justarod.event

import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.sounds.SoundSource
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.level.Level
import org.cneko.justarod.entity.Pregnant

class LactationInteractionEvents {
    companion object {
        fun init() {
            // 1. 对着其他实体右键挤奶 (例如玩家拿着桶对猫娘右键)
            UseEntityCallback.EVENT.register(UseEntityCallback { player: Player?, world: Level?, hand: InteractionHand?, entity: Entity?, hitResult: EntityHitResult? ->
                if (player!!.getItemInHand(hand).is(Items.BUCKET) && entity is Pregnant) {
                    // 需要至少有相当于一桶的量（比如 250f）
                    if (entity.milk >= 250.0f) {
                        if (!world!!.isClientSide()) {
                            entity.extractMilk(250.0f) // 排空奶水，给予如释重负的反馈


                            // 扣除空桶，给予奶桶
                            player.getItemInHand(hand).shrink(1)
                            player.getInventory().spawnAtLocation(ItemStack(Items.MILK_BUCKET))


                            // 播放挤奶音效 (复用原版挤牛奶声音)
                            level().playSound(
                                null,
                                entity.blockPosition(),
                                SoundEvents.COW_MILK,
                                SoundSource.PLAYERS,
                                1.0f,
                                1.0f
                            )
                        }
                        return@UseEntityCallback InteractionResult.SUCCESS
                    }
                }
                InteractionResult.PASS
            })

            // 2. 玩家对自己潜行右键挤奶 (Shift + 空桶右键空气)
            UseItemCallback.EVENT.register(UseItemCallback { player: Player?, world: Level?, hand: InteractionHand? ->
                if (player!!.isShiftKeyDown() && player.getItemInHand(hand).is(Items.BUCKET)) {
                    if (player.milk >= 250.0f) {
                        if (!world!!.isClientSide()) {
                            player.extractMilk(250.0f)

                            player.getItemInHand(hand).shrink(1)
                            player.getInventory().spawnAtLocation(ItemStack(Items.MILK_BUCKET))

                            level().playSound(
                                null,
                                player.blockPosition(),
                                SoundEvents.COW_MILK,
                                SoundSource.PLAYERS,
                                1.0f,
                                1.0f
                            )
                        }
                        return@UseItemCallback InteractionResultHolder.success(player.getItemInHand(hand))
                    }
                }
                InteractionResultHolder.pass(player.getItemInHand(hand))
            })
        }
    }
}