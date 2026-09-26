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
import net.minecraft.resources.Identifier
import net.minecraft.world.phys.AABB
import net.minecraft.world.level.Level
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.permissions.PermissionLevel
import net.minecraft.server.permissions.PermissionSet
import net.minecraft.server.permissions.LevelBasedPermissionSet
import org.cneko.justarod.Justarod.MODID
import org.cneko.toneko.common.mod.entities.INeko
import kotlin.jvm.optionals.getOrDefault
import kotlin.jvm.optionals.getOrElse
import kotlin.text.get


// 26.x：权限系统改为 PermissionSet/LevelBasedPermissionSet，
// 这里提供旧的 hasPermission(int level) 语义（0=ALL 1=MOD 2=GM 3=ADMIN 4=OWNER）。
fun CommandSourceStack.hasPermissionLevel(level: Int): Boolean {
    val set: PermissionSet = permissions()
    if (set === PermissionSet.ALL_PERMISSIONS) return true
    val lb = set as? LevelBasedPermissionSet ?: return false
    return lb.level().isEqualOrHigherThan(PermissionLevel.byId(level))
}


// 26.x：Entity#spawnAtLocation 需要 ServerLevel，Level#addFreshEntity 也被移除；
// 这里统一封装：仅服务端生成 ItemEntity，客户端调用安全忽略。
fun LivingEntity.spawnItemAtLocation(item: net.minecraft.world.level.ItemLike) {
    spawnItemAtLocation(ItemStack(item))
}

fun LivingEntity.spawnItemAtLocation(stack: ItemStack) {
    val lvl = level()
    if (lvl is net.minecraft.server.level.ServerLevel) {
        lvl.addFreshEntity(net.minecraft.world.entity.item.ItemEntity(lvl, x, y, z, stack))
    }
}

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
        fun rodId(path:String): Identifier{
            return Identifier.fromNamespaceAndPath(MODID, path)
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
            // 26.x：RegistryAccess#registry -> lookupOrThrow，HolderGetter#getOrThrow(ResourceKey)
            val holder = world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantment)
            return this.enchantments.getLevel(holder)
        }



    }
}
