package org.cneko.justarod.block.ascaris;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.cneko.justarod.body.AscarisBodyInteractions;
import org.cneko.justarod.body.BodyWorld;

/**
 * 柔粉肠绒毛（微绒毛 / 黏膜垂丝）：
 * 无碰撞、柔和摇曳、自带柔光，支持地面丛生（hanging=false）与顶壁垂挂（hanging=true），
 * 打破方块直角的生硬感，赋予肠道与假体腔毛茸茸的活体生物质感。
 */
public class AscarisMicrovilliBlock extends AscarisTissueBlock {

    public static final BooleanProperty HANGING = BooleanProperty.create("hanging");
    private static final VoxelShape FLOOR_SHAPE = Block.box(2, 0, 2, 14, 13, 14);
    private static final VoxelShape HANGING_SHAPE = Block.box(2, 3, 2, 14, 16, 14);

    public AscarisMicrovilliBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(HANGING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HANGING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean hang = context.getClickedFace() == Direction.DOWN;
        return this.defaultBlockState().setValue(HANGING, hang);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HANGING) ? HANGING_SHAPE : FLOOR_SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.dimension().equals(BodyWorld.DIMENSION)) {
            if (!level.isClientSide() && player instanceof ServerPlayer sp) {
                AscarisBodyInteractions.onMicrovilliInteracted(sp, pos);
            }
            return InteractionResult.SUCCESS;
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }
}
