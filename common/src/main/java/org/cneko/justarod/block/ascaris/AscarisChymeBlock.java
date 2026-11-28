package org.cneko.justarod.block.ascaris;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.cneko.justarod.body.AscarisBodyInteractions;
import org.cneko.justarod.body.BodyWorld;

/**
 * 甜味食糜团（内外通用的 4 口可食用多风味生物营养块）：
 * <ul>
 *   <li>{@code FLAVOR = 0} 经典粉蜜风味：4 饱食 + 生命恢复（微光 6）；</li>
 *   <li>{@code FLAVOR = 1} 发光浆果风味：4 饱食 + 夜视 45s + 生命恢复（柔和荧光 12）；</li>
 *   <li>{@code FLAVOR = 2} 蜂蜜金苹风味：6 饱食 + 伤害吸收 45s + 生命恢复 5s（温润蜜光 10）。</li>
 * </ul>
 */
public class AscarisChymeBlock extends AscarisTissueBlock {

    public static final int FLAVOR_SWEET = 0;
    public static final int FLAVOR_GLOW = 1;
    public static final int FLAVOR_HONEY_GOLD = 2;

    public static final IntegerProperty BITES = IntegerProperty.create("bites", 0, 3);
    public static final IntegerProperty FLAVOR = IntegerProperty.create("flavor", 0, 2);

    private static final DustParticleOptions PEACH_DUST = new DustParticleOptions(0xFFD59A, 0.9f);
    private static final DustParticleOptions GLOW_DUST = new DustParticleOptions(0xFFEE88, 0.95f);
    private static final DustParticleOptions HONEY_DUST = new DustParticleOptions(0xFFB84D, 0.95f);

    private static final VoxelShape[] SHAPES = {
            Block.box(1, 0, 1, 15, 14, 15),
            Block.box(2, 0, 2, 14, 11, 14),
            Block.box(3, 0, 3, 13, 8, 13),
            Block.box(4, 0, 4, 12, 5, 12)
    };

    public AscarisChymeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(BITES, 0)
                .setValue(FLAVOR, FLAVOR_SWEET));
    }

    public static int flavorForItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return FLAVOR_SWEET;
        if (stack.is(Items.GLOW_BERRIES)) return FLAVOR_GLOW;
        if (stack.is(Items.HONEY_BOTTLE)
                || stack.is(Items.GOLDEN_APPLE)
                || stack.is(Items.ENCHANTED_GOLDEN_APPLE)
                || stack.is(Items.CAKE)) {
            return FLAVOR_HONEY_GOLD;
        }
        return FLAVOR_SWEET;
    }

    public static String flavorLabel(int flavor) {
        return switch (flavor) {
            case FLAVOR_GLOW -> "发光浆果微光风味";
            case FLAVOR_HONEY_GOLD -> "蜂蜜金苹滋养风味";
            default -> "粉蜜甜香风味";
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BITES, FLAVOR);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(BITES)];
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        int flavor = state.getValue(FLAVOR);
        // 特殊风味食糜即使满饱食也可以尝一口领取风味增益！
        if (flavor == FLAVOR_SWEET && !player.canEat(false) && player.getHealth() >= player.getMaxHealth()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (flavor == FLAVOR_GLOW) {
            player.getFoodData().eat(4, 0.5f);
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 900, 0, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false));
        } else if (flavor == FLAVOR_HONEY_GOLD) {
            player.getFoodData().eat(6, 0.7f);
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 900, 0, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, true, false));
        } else {
            player.getFoodData().eat(4, 0.45f);
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false));
        }

        int bites = state.getValue(BITES);
        if (bites < 3) {
            level.setBlock(pos, state.setValue(BITES, bites + 1), Block.UPDATE_ALL);
        } else {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }

        if (level instanceof ServerLevel sl) {
            DustParticleOptions dust = switch (flavor) {
                case FLAVOR_GLOW -> GLOW_DUST;
                case FLAVOR_HONEY_GOLD -> HONEY_DUST;
                default -> PEACH_DUST;
            };
            sl.playSound(null, pos, SoundEvents.HONEY_DRINK.value(), SoundSource.BLOCKS, 0.55f,
                    flavor == FLAVOR_GLOW ? 1.28f : 1.15f);
            sl.sendParticles(dust, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.3, 0.25, 0.3, 0.03);
            sl.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.2, player.getZ(), 2, 0.25, 0.2, 0.25, 0.01);
        }

        if (level.dimension().equals(BodyWorld.DIMENSION) && player instanceof ServerPlayer sp) {
            AscarisBodyInteractions.onChymeEatenInside(sp, flavor);
        }
        return InteractionResult.SUCCESS;
    }
}
