package org.cneko.justarod.item
import org.cneko.justarod.JRIds

import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.sounds.SoundSource
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.level.Level

/*
动漫量
 */

/*
嗯~啊~出来了...
 */
class XPGun : Item(JRIds.itemProps("xp_gun").stacksTo(1)) {
    override fun use(world: Level, user: Player, hand: InteractionHand): InteractionResult {
        user ?: return super.use(world, user, hand)
        val handStack = user.getItemInHand(hand)

        if (user.totalExperience >= 5) {
            user.giveExperiencePoints(-5)

            if (world?.isClientSide == false) {
                val random = user.level().random

                // 发射5个经验球形成散射
                repeat(5) {
                    // 计算随机角度偏移（水平±15度，垂直±5度）
                    val yawOffset = (random.nextFloat() - 0.5f) * 30f
                    val pitchOffset = (random.nextFloat() - 0.5f) * 10f

                    // 计算新方向
                    val newYaw = user.yRot + yawOffset
                    val newPitch = user.xRot + pitchOffset

                    // 将角度转换为方向向量
                    val direction = Vec3.directionFromRotation(newPitch, newYaw)

                    // 计算生成位置（玩家眼前方0.5米）
                    val eyePos = user.getEyePosition()
                    val spawnPos = eyePos.add(direction.scale(0.5))

                    // 创建经验球实体（1点经验）
                    val xpOrb = ExperienceOrb(world, spawnPos.x, spawnPos.y, spawnPos.z, 1)

                    // 设置速度（方向向量 * 速度系数）
                    xpOrb.setDeltaMovement(direction.scale(1.2))

                    // 添加击退效果
                    xpOrb.setDeltaMovement(xpOrb.deltaMovement.add(user.deltaMovement))

                    world.addFreshEntity(xpOrb)
                }
            }

            // 播放音效（客户端）
            if (world?.isClientSide == true) {
                world!!.playSound(user, user.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.0f)
            }

            return InteractionResult.SUCCESS
        }

        return InteractionResult.PASS
    }
}