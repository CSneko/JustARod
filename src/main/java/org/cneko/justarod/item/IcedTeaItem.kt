package org.cneko.justarod.item

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent
import net.minecraft.sound.SoundEvents
import net.minecraft.stat.Stats
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.TypedActionResult
import net.minecraft.world.World
import org.cneko.justarod.JRUtil.Companion.rodId
import org.cneko.justarod.entity.IcedTeaProjectileEntity

/*
劳大，我想你了[哭泣][哭泣]
 */
class IcedTeaItem(settings: Settings) : Item(settings) {
    companion object{
        val MAN_SOUND: SoundEvent = SoundEvent.of(rodId("man"))
    }

    /**
     * 当玩家使用（右键点击）这个物品时调用。
     */
    override fun use(world: World, user: PlayerEntity, hand: Hand): TypedActionResult<ItemStack> {
        val itemStack = user.getStackInHand(hand)

        // 播放投掷声音
        world.playSound(
            null,
            user.x,
            user.y,
            user.z,
            MAN_SOUND,
            SoundCategory.NEUTRAL,
            1f,
            1f
        )

        // 为物品设置冷却时间，防止玩家连续投掷
        user.itemCooldownManager.set(this, 20) // 20 ticks = 1 秒冷却

        // 只在服务器端生成实体
        if (!world.isClient) {
            val icedTeaProjectile = IcedTeaProjectileEntity(world, user)
            icedTeaProjectile.setItem(itemStack)
            // 设置投掷物的速度和方向
            icedTeaProjectile.setVelocity(user, user.pitch, user.yaw, 0.0f, 1.5f, 1.0f)
            world.spawnEntity(icedTeaProjectile)
        }

        // 增加玩家的“使用物品”统计
        user.incrementStat(Stats.USED.getOrCreateStat(this))

        // 如果不是创造模式，消耗一个物品
        if (!user.abilities.creativeMode) {
            itemStack.decrement(1)
        }

        return TypedActionResult.success(itemStack, world.isClient())
    }
}