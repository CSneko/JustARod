package org.cneko.justarod.item.armor

import net.minecraft.world.item.ArmorItem
import net.minecraft.world.item.ArmorMaterial
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.Registry
import net.minecraft.core.Holder
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import java.util.function.Supplier
import org.cneko.justarod.JRUtil.Companion.rodId

class JRArmorMaterials {
    companion object{
        val FIREWORKS_ROD_MATERIAL:Holder<ArmorMaterial> = register(
            "fireworks_rod",
            mapOf(
                ArmorItem.Type.BOOTS to 1,
                ArmorItem.Type.LEGGINGS to 1,
                ArmorItem.Type.CHESTPLATE to 1,
                ArmorItem.Type.HELMET to 1
            ),
            0,
            SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE,
            {Ingredient.ofItems(Items.PAPER)},
            0f,
            0f,
            false
        )

        val PANTSU_MATERIAL:Holder<ArmorMaterial> = register(
            "pantsu",
            mapOf(
                ArmorItem.Type.BOOTS to 1,
                ArmorItem.Type.LEGGINGS to 3,
                ArmorItem.Type.CHESTPLATE to 2,
                ArmorItem.Type.HELMET to 1
            ),
            15,
            SoundEvents.ITEM_ARMOR_EQUIP_LEATHER,
            {Ingredient.ofItems(Items.WHITE_WOOL)},
            0f,
            0f,
            true
        )

        val DIAPER_MATERIAL:Holder<ArmorMaterial> = register(
            "diaper",
            mapOf(
                ArmorItem.Type.BOOTS to 1,
                ArmorItem.Type.LEGGINGS to 4,
                ArmorItem.Type.CHESTPLATE to 3,
                ArmorItem.Type.HELMET to 1
            ),
            10,
            SoundEvents.ITEM_ARMOR_EQUIP_LEATHER,
            {Ingredient.ofItems(Items.WHITE_WOOL)},
            0f,
            0f,
            true
        )

        fun register(
            id: String,
            defensePoints: Map<ArmorItem.Type?, Int?>?,
            enchantability: Int,
            equipSound: Holder<SoundEvent?>?,
            repairIngredientSupplier: Supplier<Ingredient?>?,
            toughness: Float,
            knockbackResistance: Float,
            dyeable: Boolean
        ): Holder<ArmorMaterial> {
            val layers = listOf(ArmorMaterial.Layer(rodId(id), "", dyeable))
            var material = ArmorMaterial(
                defensePoints,
                enchantability,
                equipSound,
                repairIngredientSupplier,
                layers,
                toughness,
                knockbackResistance
            )
            material =
                Registry.register(BuiltInRegistries.ARMOR_MATERIAL, rodId(id), material) as ArmorMaterial
            return Holder.direct(material)
        }
    }
}