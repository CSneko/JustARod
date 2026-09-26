package org.cneko.justarod.block;

import static org.cneko.justarod.Justarod.MODID;

import org.cneko.justarod.JRIds;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.UntintedParticleLeavesBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class JRBlocks {
    // 没吃过，但感觉不好吃
    // 所以猫猫一直在自娱自乐 炫压抑是吧~ -NT
    public static final Block GOLDEN_LEAVES = register(
            // 26.x：LeavesBlock 变为抽象类，使用其具体子类
            new UntintedParticleLeavesBlock(0.01f, net.minecraft.core.particles.ParticleTypes.CHERRY_LEAVES, JRIds.blockProps("golden_leaves").sound(SoundType.CHERRY_LEAVES).noCollision()),
            "golden_leaves",
            true
    );
    public static void init(){
    }

    public static Block register(Block block, String name, boolean shouldRegisterItem) {

        Identifier id = Identifier.fromNamespaceAndPath(MODID, name);

        if (shouldRegisterItem) {
            BlockItem blockItem = new BlockItem(block, JRIds.itemProps(name));
            Registry.register(BuiltInRegistries.ITEM, id, blockItem);
        }

        return Registry.register(BuiltInRegistries.BLOCK, id, block);
    }
}
