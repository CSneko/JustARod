package org.cneko.justarod

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.resources.ResourceKey
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.AABB
import net.minecraft.world.level.Level
import org.cneko.justarod.Justarod.MODID
import org.cneko.toneko.common.mod.entities.INeko
import kotlin.jvm.optionals.getOrDefault
import kotlin.jvm.optionals.getOrElse
import kotlin.text.get

// awa
class JRUtil {
    companion object {
        fun Level.getNekoInRange(entity: Entity, radius: Float): List<INeko> {
            val box = AABB(
                entity.x - radius.toDouble(),
                entity.y - radius.toDouble(),
                entity.z - radius.toDouble(),
                entity.x + radius.toDouble(),
                entity.y + radius.toDouble(),
                entity.z + radius.toDouble()
            )
            val entities = this.getEntitiesOfClass(LivingEntity::class.java, box)
            return entities.filter { it is INeko  && it != entity } as List<INeko>
        }

        fun Level.getPlayerInRange(entity: Entity, radius: Float): List<Player> {
            val box = AABB(
                entity.x - radius.toDouble(),
                entity.y - radius.toDouble(),
                entity.z - radius.toDouble(),
                entity.x + radius.toDouble(),
                entity.y + radius.toDouble(),
                entity.z + radius.toDouble()
            )
            val entities = this.getEntitiesOfClass(Player::class.java, box)
            return entities.filter {it != entity}
        }
        fun rodId(path:String): ResourceLocation{
            return ResourceLocation.fromNamespaceAndPath(MODID, path)
        }

        fun ItemStack.containsEnchantment(enchantment: ResourceKey<Enchantment>): Boolean{
            return this.isEnchanted && this.enchantments.entrySet().any { e ->
                if(e.key.isBound){
                    return e.key.value().equals(enchantment.registryKey())
                }
                return false
            }
        }
        fun ItemStack.getEnchantmentLevel(world: Level, enchantment: ResourceKey<Enchantment>): Int {
            val rm = world.registryAccess()
            val level = this.enchantments.getLevel(rm?.registry(Registries.ENCHANTMENT)?.get()?.getHolderOrThrow(enchantment))

            return level
        }



    }
}
