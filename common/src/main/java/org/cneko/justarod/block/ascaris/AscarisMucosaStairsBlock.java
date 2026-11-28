package org.cneko.justarod.block.ascaris;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.cneko.justarod.body.AscarisBodyInteractions;

/** 蛔虫黏膜阶梯：连通消化道与假体腔的柔韧黏膜梯道，可采集带走。 */
public class AscarisMucosaStairsBlock extends StairBlock {

    public AscarisMucosaStairsBlock(BlockState baseState, Properties properties) {
        super(baseState, properties);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            AscarisBodyInteractions.onAnatomyBlockBroken(sp, pos, state);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
