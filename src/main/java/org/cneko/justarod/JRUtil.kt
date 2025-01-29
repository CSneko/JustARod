package org.cneko.justarod

import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.util.Identifier
import net.minecraft.util.math.Box
import net.minecraft.world.World
import org.cneko.justarod.Justarod.MODID
import org.cneko.toneko.common.mod.entities.INeko

class JRUtil {
    companion object {
        fun World.getNekoInRange(entity: Entity, radius: Float): List<INeko> {
            val box = Box(
                entity.x - radius.toDouble(),
                entity.y - radius.toDouble(),
                entity.z - radius.toDouble(),
                entity.x + radius.toDouble(),
                entity.y + radius.toDouble(),
                entity.z + radius.toDouble()
            )
            val entities = this.getNonSpectatingEntities(LivingEntity::class.java, box)
            return entities.filter { it is INeko && !it.entity.hasStatusEffect(StatusEffects.WEAKNESS) && it != entity } as List<INeko>
        }
        fun rodId(path:String): Identifier{
            return Identifier.of(MODID, path)
        }
    }
}
