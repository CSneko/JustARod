package org.cneko.justarod.block.ascaris;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.cneko.justarod.body.BodyWorld;

/**
 * 假体腔液透膜（内外通用水床缓冲特性）：
 * - 踩落减免 80% 摔落伤害，并带有柔和 Q 弹水床回弹（潜行时不回弹）；
 * - 在体内维度假体腔上行走时额外赋予轻盈跳跃感，还原高液压体腔液的悬浮承托。
 */
public class AscarisPseudocoelomMembraneBlock extends AscarisMembraneBlock {

    public AscarisPseudocoelomMembraneBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        entity.causeFallDamage(fallDistance, 0.2f, level.damageSources().fall());
    }

    @Override
    public void updateEntityMovementAfterFallOn(BlockGetter level, Entity entity) {
        if (entity.isSuppressingBounce()) {
            super.updateEntityMovementAfterFallOn(level, entity);
            return;
        }
        Vec3 motion = entity.getDeltaMovement();
        if (motion.y < -0.22) {
            double bounce = Math.min(0.52, -motion.y * 0.48);
            entity.setDeltaMovement(motion.x, bounce, motion.z);
        } else {
            super.updateEntityMovementAfterFallOn(level, entity);
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide() && level.dimension().equals(BodyWorld.DIMENSION)
                && entity instanceof LivingEntity living && living.tickCount % 15 == 0) {
            float pressure = 64.0f;
            if (level instanceof net.minecraft.server.level.ServerLevel sl && living instanceof net.minecraft.server.level.ServerPlayer sp) {
                var visit = org.cneko.justarod.body.BodyRegionData.get(sl.getServer()).visit(sp.getUUID());
                if (visit != null && visit.isAscaris()) {
                    pressure = org.cneko.justarod.body.BodyRegionData.get(sl.getServer()).physiologyFor(visit.slot()).hydrostaticPressure;
                }
            }
            if (pressure <= 35.0f) {
                // 静水骨骼失压（体壁渗漏或哌嗪麻痹）：失去高液压弹力托举，变为黏滞阻力！
                living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 0, true, false, false));
            } else {
                int amp = pressure >= 80.0f ? 1 : 0;
                living.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 30, amp, true, false, false));
            }
        }
        super.stepOn(level, pos, state, entity);
    }
}
