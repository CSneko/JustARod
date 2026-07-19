package org.cneko.justarod.item.armor

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ArmorMaterial
import net.minecraft.world.item.ItemStack
import net.minecraft.core.Holder
import net.minecraft.world.level.Level
import org.cneko.justarod.client.renderer.armor.RodArmorRenderer
import org.cneko.toneko.common.mod.items.NekoArmor

/*
好玩嘿嘿
 */
abstract class RodArmorItem<T : RodArmorItem<T>>(material: Holder<ArmorMaterial>?,settings: Properties) : NekoArmor<T>(material,
    Type.CHESTPLATE,
    settings) {

    override fun getRenderer(): Any {
        return RodArmorRenderer<T>(getId())
    }

    abstract fun getId(): String

    override fun inventoryTick(stack: ItemStack?, world: Level?, entity: Entity?, slot: Int, selected: Boolean) {
        // 降低耐久
        if (slot == 2){
            if (entity is LivingEntity) {
                stack?.hurt(1, entity, EquipmentSlot.CHEST)
                onUse(stack, world, entity, slot, selected)
            }
        }
    }
    open fun onUse(stack: ItemStack?, world: Level?, entity: Entity?, slot: Int, selected: Boolean){

    }
}
