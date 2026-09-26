package org.cneko.justarod.item.armor

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.equipment.ArmorMaterial
import net.minecraft.world.item.equipment.ArmorType
import net.minecraft.server.level.ServerLevel
import org.cneko.justarod.client.renderer.armor.RodArmorRenderer
import org.cneko.toneko.common.mod.items.NekoArmor

/*
好玩嘿嘿
 */
abstract class RodArmorItem<T : RodArmorItem<T>>(material: ArmorMaterial, settings: Properties) : NekoArmor<T>(settings.humanoidArmor(material, ArmorType.CHESTPLATE)) {

    override fun getRenderer(): Any {
        return RodArmorRenderer<T>(getId())
    }

    abstract fun getId(): String

    override fun inventoryTick(
        stack: ItemStack,
        world: net.minecraft.server.level.ServerLevel,
        entity: net.minecraft.world.entity.Entity,
        slot: EquipmentSlot?
) {
        // 降低耐久（胸口槽位）
        if (slot == EquipmentSlot.CHEST) {
            if (entity is LivingEntity) {
                stack.hurtAndBreak(1, entity, EquipmentSlot.CHEST)
                onUse(stack, world, entity, slot)
            }
        }
    }
    open fun onUse(stack: ItemStack, world: ServerLevel, entity: Entity, slot: EquipmentSlot?){

    }
}
