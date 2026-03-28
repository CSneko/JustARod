package org.cneko.justarod.event

import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.world.World
import org.cneko.justarod.entity.Pregnant

class LactationInteractionEvents {
    companion object {
        fun init() {
            // 1. 对着其他实体右键挤奶 (例如玩家拿着桶对猫娘右键)
            UseEntityCallback.EVENT.register(UseEntityCallback { player: PlayerEntity?, world: World?, hand: Hand?, entity: Entity?, hitResult: EntityHitResult? ->
                if (player!!.getStackInHand(hand).isOf(Items.BUCKET) && entity is Pregnant) {
                    // 需要至少有相当于一桶的量（比如 250f）
                    if (entity.milk >= 250.0f) {
                        if (!world!!.isClient()) {
                            entity.extractMilk(250.0f) // 排空奶水，给予如释重负的反馈


                            // 扣除空桶，给予奶桶
                            player.getStackInHand(hand).decrement(1)
                            player.getInventory().offerOrDrop(ItemStack(Items.MILK_BUCKET))


                            // 播放挤奶音效 (复用原版挤牛奶声音)
                            world.playSound(
                                null,
                                entity.blockPos,
                                SoundEvents.ENTITY_COW_MILK,
                                SoundCategory.PLAYERS,
                                1.0f,
                                1.0f
                            )
                        }
                        return@UseEntityCallback ActionResult.SUCCESS
                    }
                }
                ActionResult.PASS
            })

            // 2. 玩家对自己潜行右键挤奶 (Shift + 空桶右键空气)
            UseItemCallback.EVENT.register(UseItemCallback { player: PlayerEntity?, world: World?, hand: Hand? ->
                if (player!!.isSneaking && player.getStackInHand(hand).isOf(Items.BUCKET)) {
                    if (player.milk >= 250.0f) {
                        if (!world!!.isClient()) {
                            player.extractMilk(250.0f)

                            player.getStackInHand(hand).decrement(1)
                            player.getInventory().offerOrDrop(ItemStack(Items.MILK_BUCKET))

                            world.playSound(
                                null,
                                player.blockPos,
                                SoundEvents.ENTITY_COW_MILK,
                                SoundCategory.PLAYERS,
                                1.0f,
                                1.0f
                            )
                        }
                        return@UseItemCallback TypedActionResult.success(player.getStackInHand(hand))
                    }
                }
                TypedActionResult.pass(player.getStackInHand(hand))
            })
        }
    }
}