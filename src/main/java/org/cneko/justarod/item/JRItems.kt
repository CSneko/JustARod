package org.cneko.justarod.item

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.cneko.justarod.Justarod.MODID
import java.util.function.Supplier


class JRItems {
    companion object{
        val SLIME_ROD = SlimeRodItem()
        val GIANT_ROD = GiantRodItem()
        val LUBRICATING_BOOK = LubricatingBookItem()
        val REDSTONE_ROD = RedstoneEndRod()
        val CACTUS_ROD = CactusRodItem()
        val EATABLE_ROD = EatableRodItem()

        var JR_ITEM_GROUP_KEY: RegistryKey<ItemGroup>? = null;
        var JR_ITEM_GROUP: ItemGroup? = null;
        fun init(){
            // 注册物品
            Registry.register(Registries.ITEM, Identifier.of(MODID, "slime_rod"), SLIME_ROD)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "giant_rod"), GIANT_ROD)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "lubricating_book"), LUBRICATING_BOOK)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "redstone_rod"), REDSTONE_ROD)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "cactus_rod"), CACTUS_ROD)
            Registry.register(Registries.ITEM, Identifier.of(MODID, "eatable_rod"), EATABLE_ROD)

            // 注册物品组
            JR_ITEM_GROUP_KEY = RegistryKey.of(Registries.ITEM_GROUP.key, Identifier.of(MODID, "item_group"))
            JR_ITEM_GROUP = FabricItemGroup.builder()
                .icon { ItemStack(SLIME_ROD) }
                .displayName(Text.translatable("itemGroup.justarod"))
                .build()
            Registry.register(Registries.ITEM_GROUP, JR_ITEM_GROUP_KEY, JR_ITEM_GROUP)

            ItemGroupEvents.modifyEntriesEvent(JR_ITEM_GROUP_KEY!!).register { entries ->
                entries.add(SLIME_ROD)
                entries.add(GIANT_ROD)
                entries.add(LUBRICATING_BOOK)
                entries.add(REDSTONE_ROD)
                entries.add(CACTUS_ROD)
                entries.add(EATABLE_ROD)
            }
        }
    }
}