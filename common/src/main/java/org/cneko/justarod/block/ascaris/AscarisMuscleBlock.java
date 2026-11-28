package org.cneko.justarod.block.ascaris;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.cneko.justarod.ascaris.AscarisEntity;
import org.cneko.justarod.body.AscarisBodyInteractions;
import org.cneko.justarod.body.BodyWorld;

/**
 * 蛔虫纵肌束：三轴旋转纵肌纤维，带轻度肌肉缓冲减伤；在体内被破坏时触发吃痛痉挛，咽部持甜食右键可投喂。
 */
public class AscarisMuscleBlock extends RotatedPillarBlock {

    public AscarisMuscleBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        entity.causeFallDamage(fallDistance, 0.5f, level.damageSources().fall());
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            AscarisBodyInteractions.onAnatomyBlockBroken(sp, pos, state);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.dimension().equals(BodyWorld.DIMENSION)) {
            if (AscarisEntity.isDewormer(stack)) {
                if (!level.isClientSide() && player instanceof ServerPlayer sp) {
                    if (AscarisBodyInteractions.tryAdministerDrugInside(sp, pos, stack)) {
                        return InteractionResult.SUCCESS;
                    }
                } else {
                    return InteractionResult.SUCCESS;
                }
            }
            if (AscarisEntity.isSweetTreat(stack)) {
                if (!level.isClientSide() && player instanceof ServerPlayer sp) {
                    if (AscarisBodyInteractions.tryFeedSweetInsidePharynx(sp, pos, stack)) {
                        return InteractionResult.SUCCESS;
                    }
                } else {
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.dimension().equals(BodyWorld.DIMENSION)) {
            if (!level.isClientSide() && player instanceof ServerPlayer sp) {
                if (AscarisBodyInteractions.tryNaturalExitAtOrifice(sp, pos)) {
                    return InteractionResult.SUCCESS;
                }
                if (AscarisBodyInteractions.tryStimulatePharynx(sp, pos)) {
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }
}
