package org.cneko.justarod.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

import static org.cneko.justarod.Justarod.MODID;

public class JRBlocks {
    // 没吃过，但感觉不好吃
    public static final Block GOLDEN_LEAVES = register(
            new LeavesBlock(AbstractBlock.Settings.create().sounds(BlockSoundGroup.CHERRY_LEAVES).noCollision()),
            "golden_leaves",
            true
    );
    public static void init(){
    }

    public static Block register(Block block, String name, boolean shouldRegisterItem) {

        Identifier id = Identifier.of(MODID, name);

        if (shouldRegisterItem) {
            BlockItem blockItem = new BlockItem(block, new Item.Settings());
            Registry.register(Registries.ITEM, id, blockItem);
        }

        return Registry.register(Registries.BLOCK, id, block);
    }
}
