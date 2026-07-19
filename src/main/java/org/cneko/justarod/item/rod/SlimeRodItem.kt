package org.cneko.justarod.item.rod

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
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
class SlimeRodItem : SelfUsedItem(Settings().maxCount(1).maxDamage(1000).component(JRComponents.Companion.USED_TIME_MARK, 0)){
    override fun useOnSelf(stack: ItemStack, world: Level?, entity: LivingEntity, slot: Int, selected: Boolean): InteractionResult {
        // 如果成功使用，就1/500的几率生成一只可爱的小史莱姆
        if(super.useOnSelf(stack, world, entity, slot, selected) == InteractionResult.SUCCESS){
            if(world is ServerLevel){
                // 如果有发青效果，则去除
                if(entity.hasEffect(JREffects.ESTRUS_EFFECT.let { BuiltInRegistries.MOB_EFFECT.getOrThrow(it) })){
                    entity.removeEffect(JREffects.ESTRUS_EFFECT.let { BuiltInRegistries.MOB_EFFECT.getOrThrow(it) })
                }
                val serverWorld:ServerLevel = world
                if (Random.create().nextInt(500) == 0) {
                    val nbtCompound = CompoundTag()
                    nbtCompound.putString("id","slime") // id为slime
                    nbtCompound.putInt("Size", 0) // 大小为1
                    nbtCompound.putString("Owner", entity.uuidAsString) // 主人
                    val pos = entity.pos
                    val slime = EntityType.loadEntityWithPassengers(
                        nbtCompound, serverWorld
                    ) { entityx: Entity ->
                        entityx.moveTo(
                            pos.x,
                            pos.y,
                            pos.z,
                            entityx.yaw,
                            entityx.pitch
                        )
                        entityx
                    }

                    serverWorld.tryAddFreshEntityWithPassengers(slime)
                    return InteractionResult.SUCCESS
                }
            }
        }
        return InteractionResult.PASS
    }
}