package org.cneko.justarod.item.bdsm

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.level.Level
import org.cneko.justarod.entity.BDSMable

/*
被电击，齁哦哦哦♡ 好爽~~♡
哒... 哒咩！不...不要提高挡数了喵♡ 要受不了惹♡
 */
class ElectricShockController(properties: Properties): Item(properties) {
    override fun use(world: Level?, user: Player?, hand: InteractionHand?): InteractionResultHolder<ItemStack?>? {
        if (user == null || world !is ServerLevel){
            return super.use(world, user, hand)
        }
        // 寻找附近16格的实体
        val nearbyEntities = user.level().getEntitiesOfClass(
            LivingEntity::class.java,
            user.boundingBox.inflate(16.0, 16.0, 16.0)
        ) {it is BDSMable && it.electricShock > 0}
        for (entity in nearbyEntities ?: emptyList()) {
            entity as BDSMable
            // 触发电击
            entity.hurt(user.damageSources.magic(),0.2f)
            // 跳起来
            entity.addDeltaMovement(0.0, 0.5, 0.0)
            // 播放声音
            entity.level().playSound(
                null,
                entity.x,
                entity.y,
                entity.z,
                SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER,
                entity.soundCategory,
                1.0f,
                1.0f
            )
            // 播放粒子
            level().sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                entity.x,
                entity.y + entity.height / 2.0,
                entity.z,
                10,
                0.0, 0.0, 0.0,0.1
            )
            return InteractionResultHolder.success(user.getItemInHand(hand))

        }

        return super.use(world, user, hand)
    }
}