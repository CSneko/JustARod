package org.cneko.justarod.item.armor

import net.minecraft.core.Holder
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.item.equipment.ArmorMaterial
import net.minecraft.world.item.equipment.ArmorType
import net.minecraft.world.item.equipment.EquipmentAsset
import net.minecraft.world.item.equipment.EquipmentAssets
import org.cneko.justarod.JRUtil.Companion.rodId

/**
 * 26.x 迁移说明：ArmorMaterial 变为 record（不再注册），ArmorItem 移除后
 * 由 Item.Properties#humanoidArmor 装配，装备资源使用 custom EquipmentAsset。
 */
class JRArmorMaterials {
    companion object{
        val FIREWORKS_ROD_MATERIAL: ArmorMaterial = create(
            "fireworks_rod",
            mapOf(
                ArmorType.BOOTS to 1,
                ArmorType.LEGGINGS to 1,
                ArmorType.CHESTPLATE to 1,
                ArmorType.HELMET to 1
            ),
            durability = 15,
            enchantability = 1,
            equipSound = SoundEvents.ARMOR_EQUIP_NETHERITE,
            repairTag = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "paper")),
            toughness = 0f,
            knockbackResistance = 0f
        )

        val PANTSU_MATERIAL: ArmorMaterial = create(
            "pantsu",
            mapOf(
                ArmorType.BOOTS to 1,
                ArmorType.LEGGINGS to 3,
                ArmorType.CHESTPLATE to 2,
                ArmorType.HELMET to 1
            ),
            durability = 15,
            enchantability = 15,
            equipSound = SoundEvents.ARMOR_EQUIP_LEATHER,
            repairTag = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "wool")),
            toughness = 0f,
            knockbackResistance = 0f
        )

        val DIAPER_MATERIAL: ArmorMaterial = create(
            "diaper",
            mapOf(
                ArmorType.BOOTS to 1,
                ArmorType.LEGGINGS to 4,
                ArmorType.CHESTPLATE to 3,
                ArmorType.HELMET to 1
            ),
            durability = 15,
            enchantability = 10,
            equipSound = SoundEvents.ARMOR_EQUIP_LEATHER,
            repairTag = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "wool")),
            toughness = 0f,
            knockbackResistance = 0f
        )

        private fun create(
            id: String,
            defense: Map<ArmorType, Int>,
            durability: Int,
            enchantability: Int,
            equipSound: Holder<SoundEvent>?,
            repairTag: TagKey<Item>,
            toughness: Float,
            knockbackResistance: Float
        ): ArmorMaterial {
            val asset = ResourceKey.create(EquipmentAssets.ROOT_ID, rodId(id))
            return ArmorMaterial(
                durability,
                defense,
                enchantability,
                equipSound ?: SoundEvents.ARMOR_EQUIP_GENERIC, // 26.x：SoundEvents 字段本身即 Holder.Reference
                toughness,
                knockbackResistance,
                repairTag,
                asset
            )
        }
    }
}
