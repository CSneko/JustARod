package org.cneko.justarod.item.rod
import org.cneko.justarod.JRIds

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionResult
import net.minecraft.util.RandomSource
import net.minecraft.world.level.Level
import org.cneko.justarod.effect.JREffects
import org.cneko.justarod.item.JRComponents

/*
黏黏糊糊的呢
 */
class SlimeRodItem : SelfUsedItem(JRIds.itemProps("slime_rod").stacksTo(1).durability(1000).component(JRComponents.Companion.USED_TIME_MARK, 0)){
    override fun useOnSelf(stack: ItemStack, world: Level?, entity: LivingEntity, slot: Int, selected: Boolean): InteractionResult {
        // 如果成功使用，就1/500的几率生成一只可爱的小史莱姆
        if(super.useOnSelf(stack, world, entity, slot, selected) == InteractionResult.SUCCESS){
            if(world is ServerLevel){
                // 如果有发青效果，则去除
                if(entity.hasEffect(JREffects.ESTRUS_EFFECT)){
                    entity.removeEffect(JREffects.ESTRUS_EFFECT)
                }
                val serverWorld:ServerLevel = world
                if (RandomSource.create().nextInt(500) == 0) {
                    val pos = entity.position()
                    // 26.x：EntityType#create(CompoundTag, Level) 移除，直接用类型实例创建
                    val slime = EntityType.SLIME.create(serverWorld, EntitySpawnReason.EVENT)
                    if (slime != null) {
                        // 26.x：Entity#moveTo(x,y,z,yaw,pitch) 移除，用 setPos
                        slime.setPos(pos.x, pos.y, pos.z)
                        serverWorld.addFreshEntity(slime)
                        return InteractionResult.SUCCESS
                    }
                }
            }
        }
        return InteractionResult.PASS
    }
}