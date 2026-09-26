package org.cneko.justarod;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import static org.cneko.justarod.Justarod.MODID;

/**
 * 26.x：Block / Item 构造函数内部会校验注册 id（"Block id not set" / "Item id not set"），
 * 因此所有 {@code BlockBehaviour.Properties} / {@code Item.Properties} 必须在构造前
 * 通过 {@code setId(ResourceKey)} 声明 id。本类统一生成，避免散落的样板代码。
 */
public final class JRIds {
    private JRIds() {
    }

    public static ResourceKey<Block> blockKey(String path) {
        return ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MODID, path));
    }

    public static ResourceKey<Item> itemKey(String path) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, path));
    }

    public static BlockBehaviour.Properties blockProps(String path) {
        return BlockBehaviour.Properties.of().setId(blockKey(path));
    }

    public static Item.Properties itemProps(String path) {
        return new Item.Properties().setId(itemKey(path));
    }
}
