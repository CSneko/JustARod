package org.cneko.justarod

import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.Enchantments
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.util.Identifier
import net.minecraft.util.math.Box
import net.minecraft.world.World
import org.cneko.justarod.Justarod.MODID
import org.cneko.toneko.common.mod.entities.INeko
import kotlin.jvm.optionals.getOrElse
import kotlin.text.get

// awa
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
            return entities.filter { it is INeko  && it != entity } as List<INeko>
        }

        fun World.getPlayerInRange(entity: Entity, radius: Float): List<PlayerEntity> {
            val box = Box(
                entity.x - radius.toDouble(),
                entity.y - radius.toDouble(),
                entity.z - radius.toDouble(),
                entity.x + radius.toDouble(),
                entity.y + radius.toDouble(),
                entity.z + radius.toDouble()
            )
            val entities = this.getNonSpectatingEntities(PlayerEntity::class.java, box)
            return entities.filter {it != entity}
        }
        fun rodId(path:String): Identifier{
            return Identifier.of(MODID, path)
        }

        fun ItemStack.containsEnchantment(enchantment: RegistryKey<Enchantment>): Boolean{
            return this.hasEnchantments() && this.enchantments.enchantments.any { e ->
                if(e.key.isPresent){
                    return e.key.get().value.equals(enchantment.value)
                }
                return false
            }
        }
        fun ItemStack.getEnchantmentLevel(world : World,enchantment: RegistryKey<Enchantment>): Int {
            val rm = world.registryManager
            val level = this.enchantments.getLevel(rm?.get(RegistryKeys.ENCHANTMENT)?.entryOf(enchantment))

            return 0
        }



    }
}
