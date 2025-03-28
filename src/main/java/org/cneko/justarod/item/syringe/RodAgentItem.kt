package org.cneko.justarod.item.syringe

import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.server.world.ServerWorld
import org.cneko.justarod.effect.JREffects
import org.cneko.justarod.entity.JREntities
import org.cneko.justarod.entity.RodEntity
import org.cneko.justarod.item.addEffect
import org.cneko.toneko.common.mod.entities.NekoEntity

class RodAgentItem:BaseSyringeItem(Settings()) {
    override fun applyEffect(target: LivingEntity) {
        // 如果生物是Neko
        if (target is NekoEntity) {
            // 变成Rod
            val world = target.world
            if (world is ServerWorld){
                target.remove(Entity.RemovalReason.DISCARDED)
                val rod = RodEntity(JREntities.ROD,world)
                rod.setPos(target.x, target.y+1, target.z)
                world.spawnEntity(rod)
                // 如果有名字的话
                if (target.hasCustomName()){
                    rod.customName = target.customName
                }
            }
        }else{
            // 否则给发情效果
            target.addEffect(JREffects.ESTRUS_EFFECT, 3600, 0)
        }
    }
}