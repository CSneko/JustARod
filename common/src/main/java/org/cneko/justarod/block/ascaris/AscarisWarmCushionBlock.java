package org.cneko.justarod.block.ascaris;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.cneko.justarod.body.AscarisBodyInteractions;
import org.cneko.justarod.body.BodyWorld;
import org.cneko.justarod.entity.Pregnant;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 柔暖体腔软垫（体温暖窝核心方块，内外通用）：
 * - 柔和玫瑰蜜桃暖光，踩落减免 85% 摔落伤害并带轻微软垫缓冲；
 * - 右键依偎歇息：温暖回血、恢复少量饱食度、清除疲劳与宫寒、飘出暖桃光尘与爱心；
 * - 在粉白蛔虫娘体内依偎时，还会温柔安抚她的执念、提升亲密并触发她的暖心轻语！
 */
public class AscarisWarmCushionBlock extends AscarisTissueBlock {

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 10, 16);
    private static final DustParticleOptions WARM_PEACH = new DustParticleOptions(0xFFD6B0, 0.95f);
    private static final Map<UUID, Long> LAST_SNUGGLE_TICK = new ConcurrentHashMap<>();

    public AscarisWarmCushionBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        entity.causeFallDamage(fallDistance, 0.15f, level.damageSources().fall());
    }

    @Override
    public void updateEntityMovementAfterFallOn(BlockGetter level, Entity entity) {
        if (entity.isSuppressingBounce()) {
            super.updateEntityMovementAfterFallOn(level, entity);
            return;
        }
        Vec3 motion = entity.getDeltaMovement();
        if (motion.y < -0.25) {
            entity.setDeltaMovement(motion.x, Math.min(0.38, -motion.y * 0.38), motion.z);
        } else {
            super.updateEntityMovementAfterFallOn(level, entity);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel sl) || !(player instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }
        // 在粉白蛔虫娘体内：潜行+右键 = 安心入眠 / 午后小憩（可安稳跳过外界黑夜与风雨，并付与暖腹余温！）
        if (level.dimension().equals(BodyWorld.DIMENSION) && player.isShiftKeyDown()) {
            if (AscarisBodyInteractions.trySleepInWarmNook(sp, pos)) {
                return InteractionResult.SUCCESS;
            }
        }
        long now = sl.getGameTime();
        Long prev = LAST_SNUGGLE_TICK.get(sp.getUUID());
        if (prev != null && now - prev < 80L) {
            sp.sendOverlayMessage(Component.literal("§d你暖呼呼地靠在柔软体腔垫上，全身都放松了下来~♡ §7（潜行+右键可安心小睡）"));
            return InteractionResult.SUCCESS;
        }
        LAST_SNUGGLE_TICK.put(sp.getUUID(), now);

        sp.heal(4.0f);
        sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 0, true, false));
        if (sp.getFoodData().getFoodLevel() < 20) {
            sp.getFoodData().eat(3, 0.5f);
        }
        if (sp instanceof Pregnant p) {
            if (p.getFatigue() > 0) p.setFatigue(0);
            if (p.getUterineCold() > 0) p.setUterineCold(0);
        }

        sl.playSound(null, pos, SoundEvents.WOOL_STEP, SoundSource.BLOCKS, 0.65f, 0.9f);
        sl.sendParticles(ParticleTypes.HEART, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 6, 0.45, 0.3, 0.45, 0.02);
        sl.sendParticles(WARM_PEACH, pos.getX() + 0.5, pos.getY() + 0.85, pos.getZ() + 0.5, 12, 0.5, 0.3, 0.5, 0.02);

        if (level.dimension().equals(BodyWorld.DIMENSION)) {
            AscarisBodyInteractions.onWarmCushionSnuggled(sp, pos);
        } else {
            sp.sendOverlayMessage(Component.literal("§d暖烘烘的体腔软垫包裹着你，疲惫与寒意都悄悄散去啦~♡"));
        }
        return InteractionResult.SUCCESS;
    }

    public static void clearCooldown(UUID playerUuid) {
        LAST_SNUGGLE_TICK.remove(playerUuid);
    }
}
