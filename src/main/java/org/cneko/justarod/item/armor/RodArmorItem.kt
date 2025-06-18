package org.cneko.justarod.item.armor

import net.minecraft.entity.Entity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.item.ArmorMaterial
import net.minecraft.item.ItemStack
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.world.World
import org.cneko.justarod.client.renderer.armor.RodArmorRenderer
import org.cneko.toneko.common.mod.items.NekoArmor

/*
好玩嘿嘿
 */
abstract class RodArmorItem<T : RodArmorItem<T>>(material: RegistryEntry<ArmorMaterial>?,settings: Settings) : NekoArmor<T>(material,
    Type.CHESTPLATE,
    settings) {

    override fun getRenderer(): Any {
        return RodArmorRenderer<T>(getId())
    }

    abstract fun getId(): String

    override fun inventoryTick(stack: ItemStack?, world: World?, entity: Entity?, slot: Int, selected: Boolean) {
        // 降低耐久
        if (slot == 2){
            if (entity is LivingEntity) {
                stack?.damage(1, entity, EquipmentSlot.CHEST)
                onUse(stack, world, entity, slot, selected)
            }
        }
    }
    open fun onUse(stack: ItemStack?, world: World?, entity: Entity?, slot: Int, selected: Boolean){

    }
}
