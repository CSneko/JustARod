package org.cneko.justarod.block.ascaris;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.cneko.justarod.body.AscarisBodyInteractions;

/** 半透明生物膜通用基类（肠壁上皮膜、子宫膜壁等）。 */
public class AscarisMembraneBlock extends TransparentBlock {

    public AscarisMembraneBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            AscarisBodyInteractions.onAnatomyBlockBroken(sp, pos, state);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
