package org.cneko.justarod.item.armor

import net.minecraft.item.ArmorItem
import net.minecraft.item.ArmorMaterial
import net.minecraft.item.Items
import net.minecraft.recipe.Ingredient
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.sound.SoundEvent
import net.minecraft.sound.SoundEvents
import java.util.function.Supplier
import org.cneko.justarod.JRUtil.Companion.rodId

class JRArmorMaterials {
    companion object{
        val FIREWORKS_ROD_MATERIAL:RegistryEntry<ArmorMaterial> = register(
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

        fun register(
            id: String,
            defensePoints: Map<ArmorItem.Type?, Int?>?,
            enchantability: Int,
            equipSound: RegistryEntry<SoundEvent?>?,
            repairIngredientSupplier: Supplier<Ingredient?>?,
            toughness: Float,
            knockbackResistance: Float,
            dyeable: Boolean
        ): RegistryEntry<ArmorMaterial> {
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
                Registry.register(Registries.ARMOR_MATERIAL, rodId(id), material) as ArmorMaterial
            return RegistryEntry.of(material)
        }
    }
}