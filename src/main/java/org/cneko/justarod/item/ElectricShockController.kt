package org.cneko.justarod.item

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Hand
import net.minecraft.util.TypedActionResult
import net.minecraft.world.World
import org.cneko.justarod.entity.BDSMable

class ElectricShockController(settings: Item.Settings): Item(settings) {
    override fun use(world: World?, user: PlayerEntity?, hand: Hand?): TypedActionResult<ItemStack?>? {
        if (user == null || world !is ServerWorld){
            return super.use(world, user, hand)
        }
        // 寻找附近16格的实体
        val nearbyEntities = user.world.getEntitiesByClass(
            LivingEntity::class.java,
            user.boundingBox.expand(16.0, 16.0, 16.0)
        ) {it is BDSMable && it.electricShock > 0}
        for (entity in nearbyEntities ?: emptyList()) {
            entity as BDSMable
            // 触发电击
            entity.damage(user.damageSources.magic(),0.2f)
            // 跳起来
            entity.addVelocity(0.0, 0.5, 0.0)
            // 播放声音
            entity.world.playSound(
                null,
                entity.x,
                entity.y,
                entity.z,
                net.minecraft.sound.SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER,
                entity.soundCategory,
                1.0f,
                1.0f
            )
            // 播放粒子
            world.spawnParticles(
                net.minecraft.particle.ParticleTypes.ELECTRIC_SPARK,
                entity.x,
                entity.y + entity.height / 2.0,
                entity.z,
                10,
                0.0, 0.0, 0.0,0.1
            )
            return TypedActionResult.success(user.getStackInHand(hand))

        }

        return super.use(world, user, hand)
    }
}