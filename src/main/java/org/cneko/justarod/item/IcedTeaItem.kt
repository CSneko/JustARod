package org.cneko.justarod.item

import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.sounds.SoundSource
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.stats.Stats
import net.minecraft.world.InteractionHand
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.level.Level
import org.cneko.justarod.JRUtil.Companion.rodId
import org.cneko.justarod.entity.IcedTeaProjectileEntity

/*
劳大，我想你了[哭泣][哭泣]
 */
class IcedTeaItem(properties: Properties) : Item(properties) {
    companion object{
        val MAN_SOUND: SoundEvent = SoundEvent.of(rodId("man"))
    }

    /**
     * 当玩家使用（右键点击）这个物品时调用。
     */
    override fun use(world: Level, user: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val itemStack = user.getItemInHand(hand)

        // 播放投掷声音
        level().playSound(
            null,
            user.x,
            user.y,
            user.z,
            MAN_SOUND,
            SoundSource.NEUTRAL,
            1f,
            1f
        )

        // 为物品设置冷却时间，防止玩家连续投掷
        user.itemCooldownManager.set(this, 20) // 20 ticks = 1 秒冷却

        // 只在服务器端生成实体
        if (!level().isClientSide) {
            val icedTeaProjectile = IcedTeaProjectileEntity(world, user)
            icedTeaProjectile.setItem(itemStack)
            // 设置投掷物的速度和方向
            icedTeaProjectile.setVelocity(user, user.pitch, user.yaw, 0.0f, 1.5f, 1.0f)
            level().addFreshEntity(icedTeaProjectile)
        }

        // 增加玩家的“使用物品”统计
        user.incrementStat(Stats.USED.getOrCreateStat(this))

        // 如果不是创造模式，消耗一个物品
        if (!user.abilities.isCreative()) {
            itemStack.shrink(1)
        }

        return InteractionResultHolder.success(itemStack, level().isClientSide())
    }
}