package org.cneko.justarod.block.ascaris;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.cneko.justarod.ascaris.AscarisEntity;
import org.cneko.justarod.block.JRBlocks;
import org.cneko.justarod.body.AscarisBodyInteractions;
import org.cneko.justarod.body.BodyWorld;

/**
 * 粉白蛔虫娘体内通用自定义生物组织方块：
 * - 生存模式可采集带走；
 * - 在体内维度被破坏时触发外侧蛔虫娘吃痛反馈与肌肉痉挛；
 * - 柔韧黏膜在口腔三唇 / 肛门括约肌处空手右键可自然滑出体外，手持甜食右键咽部可直接投喂。
 */
public class AscarisTissueBlock extends Block {

    public AscarisTissueBlock(Properties properties) {
        super(properties);
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
            if (AscarisEntity.isDewormer(stack)
                    && (state.is(JRBlocks.ASCARIS_MUCOSA) || state.is(JRBlocks.ASCARIS_MUSCLE))) {
                if (!level.isClientSide() && player instanceof ServerPlayer sp) {
                    if (AscarisBodyInteractions.tryAdministerDrugInside(sp, pos, stack)) {
                        return InteractionResult.SUCCESS;
                    }
                } else {
                    return InteractionResult.SUCCESS;
                }
            }
            if (AscarisEntity.isSweetTreat(stack)
                    && (state.is(JRBlocks.ASCARIS_MUCOSA) || state.is(JRBlocks.ASCARIS_MUSCLE))) {
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
            if (state.is(JRBlocks.ASCARIS_OVARY_TISSUE)) {
                if (!level.isClientSide() && player instanceof ServerPlayer sp) {
                    if (AscarisBodyInteractions.tryStimulateOvary(sp, pos)) {
                        return InteractionResult.SUCCESS;
                    }
                } else {
                    return InteractionResult.SUCCESS;
                }
            }
            if (state.is(JRBlocks.ASCARIS_MUCOSA) || state.is(JRBlocks.ASCARIS_CUTICLE)) {
                if (!level.isClientSide() && player instanceof ServerPlayer sp) {
                    if (AscarisBodyInteractions.tryNaturalExitAtOrifice(sp, pos)) {
                        return InteractionResult.SUCCESS;
                    }
                    if (state.is(JRBlocks.ASCARIS_MUCOSA) && AscarisBodyInteractions.tryStimulatePharynx(sp, pos)) {
                        return InteractionResult.SUCCESS;
                    }
                }
            }
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }
}
