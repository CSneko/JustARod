package org.cneko.justarod.block.ascaris;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.cneko.justarod.ascaris.AscarisEntity;
import org.cneko.justarod.body.AscarisBodyInteractions;
import org.cneko.justarod.body.BodyWorld;
import org.cneko.justarod.item.JRItems;

/**
 * 蛔虫神经索（内外通用的生物电红石脉冲 + 体内围咽神经环安抚操控）：
 * - 任意维度踩踏或右键均会触发持续 16 tick 的生物电神经脉冲（红石信号强度 10）；
 * - 在粉白蛔虫娘体内空手右键可安抚她的神经中枢，降低病娇执念并解除挽留；持甜食右键可直接投喂并催生肠道食糜。
 */
public class AscarisNerveCordBlock extends RotatedPillarBlock {

    public static final BooleanProperty EXCITED = BooleanProperty.create("excited");
    private static final DustParticleOptions NERVE_SPARK = new DustParticleOptions(0xFFE07A, 0.85f);
    private static final DustParticleOptions BLOCKED_BLUE = new DustParticleOptions(0x88AAEE, 0.8f);

    public AscarisNerveCordBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(EXCITED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(EXCITED);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(EXCITED) ? 10 : 0;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide() && !state.getValue(EXCITED)) {
            if (level instanceof ServerLevel sl && level.dimension().equals(BodyWorld.DIMENSION)
                    && entity instanceof ServerPlayer sp) {
                var visit = org.cneko.justarod.body.BodyRegionData.get(sl.getServer()).visit(sp.getUUID());
                if (visit != null && visit.isAscaris()) {
                    var phys = org.cneko.justarod.body.BodyRegionData.get(sl.getServer()).physiologyFor(visit.slot());
                    if (phys.isParalyzedByPiperazine()) {
                        sl.sendParticles(BLOCKED_BLUE, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5,
                                4, 0.25, 0.2, 0.25, 0.01);
                        super.stepOn(level, pos, state, entity);
                        return;
                    }
                    int rx = pos.getX() - org.cneko.justarod.body.BodyRegionData.centerX(visit.slot());
                    phys.launchImpulse(rx, rx > org.cneko.justarod.body.AscarisAnatomyBuilder.NERVE_RING_X ? -1 : +1,
                            org.cneko.justarod.body.AscarisPhysiologyState.NerveImpulse.KIND_SENSORY, "");
                }
            }
            excite(level, pos, state);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(EXCITED)) {
            level.setBlock(pos, state.setValue(EXCITED, false), Block.UPDATE_ALL);
            level.updateNeighborsAt(pos, this, null);
        }
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
            if (stack.is(JRItems.Companion.getASCARIS_MOLT())) {
                if (!level.isClientSide() && player instanceof ServerPlayer sp) {
                    excite(level, pos, state);
                    if (AscarisBodyInteractions.trySelfHealFromNerveRing(sp, pos, stack)) {
                        return InteractionResult.SUCCESS;
                    }
                } else {
                    return InteractionResult.SUCCESS;
                }
            }
            if (AscarisEntity.isSweetTreat(stack)) {
                if (!level.isClientSide() && player instanceof ServerPlayer sp) {
                    excite(level, pos, state);
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
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        boolean blocked = false;
        if (level instanceof ServerLevel sl && level.dimension().equals(BodyWorld.DIMENSION) && player instanceof ServerPlayer sp) {
            var visit = org.cneko.justarod.body.BodyRegionData.get(sl.getServer()).visit(sp.getUUID());
            if (visit != null && visit.isAscaris()) {
                var phys = org.cneko.justarod.body.BodyRegionData.get(sl.getServer()).physiologyFor(visit.slot());
                blocked = phys.isParalyzedByPiperazine();
            }
        }
        if (blocked && level instanceof ServerLevel sl) {
            sl.sendParticles(BLOCKED_BLUE, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5,
                    6, 0.3, 0.25, 0.3, 0.015);
        } else {
            excite(level, pos, state);
        }
        if (level.dimension().equals(BodyWorld.DIMENSION) && player instanceof ServerPlayer sp) {
            if (player.isShiftKeyDown()) {
                if (AscarisBodyInteractions.tryOpenNeuralWindow(sp, pos)) {
                    return InteractionResult.SUCCESS;
                }
            }
            AscarisBodyInteractions.onNerveCordSoothed(sp, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            AscarisBodyInteractions.onAnatomyBlockBroken(sp, pos, state);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    private void excite(Level level, BlockPos pos, BlockState state) {
        if (!state.getValue(EXCITED)) {
            level.setBlock(pos, state.setValue(EXCITED, true), Block.UPDATE_ALL);
            level.updateNeighborsAt(pos, this, null);
        }
        level.scheduleTick(pos, this, 16);
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(NERVE_SPARK, pos.getX() + 0.5, pos.getY() + 0.65, pos.getZ() + 0.5,
                    6, 0.3, 0.3, 0.3, 0.02);
        }
    }
}
