package org.cneko.justarod.item

import net.minecraft.entity.ExperienceOrbEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World

/*
动漫量
 */
class XPGun : Item(Settings().maxCount(1)) {
    override fun use(world: World?, user: PlayerEntity?, hand: Hand?): TypedActionResult<ItemStack> {
        user ?: return super.use(world, user, hand)
        val handStack = user.getStackInHand(hand)

        if (user.totalExperience >= 5) {
            user.addExperience(-5)

            if (world?.isClient == false) {
                val random = user.world.random

                // 发射5个经验球形成散射
                repeat(5) {
                    // 计算随机角度偏移（水平±15度，垂直±5度）
                    val yawOffset = (random.nextFloat() - 0.5f) * 30f
                    val pitchOffset = (random.nextFloat() - 0.5f) * 10f

                    // 计算新方向
                    val newYaw = user.yaw + yawOffset
                    val newPitch = user.pitch + pitchOffset

                    // 将角度转换为方向向量
                    val direction = Vec3d.fromPolar(newPitch, newYaw)

                    // 计算生成位置（玩家眼前方0.5米）
                    val eyePos = user.eyePos
                    val spawnPos = eyePos.add(direction.multiply(0.5))

                    // 创建经验球实体（1点经验）
                    val xpOrb = ExperienceOrbEntity(world, spawnPos.x, spawnPos.y, spawnPos.z, 1)

                    // 设置速度（方向向量 * 速度系数）
                    xpOrb.velocity = direction.multiply(1.2)

                    // 添加击退效果
                    xpOrb.velocity = xpOrb.velocity.add(user.velocity)

                    world.spawnEntity(xpOrb)
                }
            }

            // 播放音效（客户端）
            if (world?.isClient == true) {
                world.playSound(user, user.blockPos, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 1.0f)
            }

            return TypedActionResult.success(handStack)
        }

        return TypedActionResult.pass(handStack)
    }
}