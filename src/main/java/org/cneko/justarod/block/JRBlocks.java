package org.cneko.justarod.block;

import static org.cneko.justarod.Justarod.MODID;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class JRBlocks {
    // 没吃过，但感觉不好吃
    // 所以猫猫一直在自娱自乐 炫压抑是吧~ -NT
    public static final Block GOLDEN_LEAVES = register(
            new LeavesBlock(BlockBehaviour.Properties.of().sound(SoundType.CHERRY_LEAVES).noCollission()),
            "golden_leaves",
            true
    );
    public static void init(){
    }

    public static Block register(Block block, String name, boolean shouldRegisterItem) {

        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MODID, name);

        if (shouldRegisterItem) {
            BlockItem blockItem = new BlockItem(block, new Item.Properties());
            Registry.register(BuiltInRegistries.ITEM, id, blockItem);
        }

        return Registry.register(BuiltInRegistries.BLOCK, id, block);
    }
}
