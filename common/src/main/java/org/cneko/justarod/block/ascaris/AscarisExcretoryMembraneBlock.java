package org.cneko.justarod.block.ascaris;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.cneko.justarod.body.BodyWorld;
import org.cneko.justarod.entity.Pregnant;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 蛔虫排泄管膜（H 形排泄系统净化功能，内外通用）：
 * - 右键可借用排泄管膜清除玩家身上的负面状态效果；
 * - 在体内维度使用时冷却更短（5 秒）并顺带缓解疲劳与排泄积压；带回主世界同样可排毒（20 秒冷却）。
 */
public class AscarisExcretoryMembraneBlock extends AscarisMembraneBlock {

    private static final DustParticleOptions AQUA_DUST = new DustParticleOptions(0x98EEFF, 0.9f);
    private static final Map<UUID, Long> LAST_DETOX_TICK = new ConcurrentHashMap<>();

    public AscarisExcretoryMembraneBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer sp) || !(level instanceof ServerLevel sl)) {
            return InteractionResult.PASS;
        }
        boolean insideBody = level.dimension().equals(BodyWorld.DIMENSION);
        long cooldown = insideBody ? 100L : 400L;
        long now = sl.getGameTime();
        Long prev = LAST_DETOX_TICK.get(sp.getUUID());
        if (prev != null && now - prev < cooldown) {
            long remainSec = Math.max(1L, (cooldown - (now - prev) + 19L) / 20L);
            sp.sendOverlayMessage(Component.literal("§b排泄管膜正在缓冲渗透中……（剩余 " + remainSec + " 秒）"));
            return InteractionResult.SUCCESS;
        }

        List<Holder<MobEffect>> harmful = new ArrayList<>();
        for (MobEffectInstance inst : sp.getActiveEffects()) {
            if (!inst.getEffect().value().isBeneficial()) {
                harmful.add(inst.getEffect());
            }
        }
        for (Holder<MobEffect> eff : harmful) {
            sp.removeEffect(eff);
        }

        boolean easedPhysiology = false;
        if (insideBody && sp instanceof Pregnant p) {
            if (p.getExcretion() > 0) {
                p.setExcretion(Math.max(0, p.getExcretion() - 6000));
                easedPhysiology = true;
            }
            if (p.getFatigue() > 0) {
                p.setFatigue(Math.max(0, p.getFatigue() - 6000));
                easedPhysiology = true;
            }
        }

        LAST_DETOX_TICK.put(sp.getUUID(), now);
        sl.playSound(null, pos, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.BLOCKS, 0.7f, 1.25f);
        sl.sendParticles(AQUA_DUST, sp.getX(), sp.getY() + 1.0, sp.getZ(), 14, 0.4, 0.5, 0.4, 0.04);
        sl.sendParticles(ParticleTypes.BUBBLE_POP, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 6, 0.3, 0.2, 0.3, 0.02);

        if (!harmful.isEmpty() || easedPhysiology) {
            sp.sendOverlayMessage(Component.literal(insideBody
                    ? "§b借助 H 形排泄管膜完成了渗透净化，负面状态与倦意一扫而空~"
                    : "§b排泄管膜渗出清凉体液，清除了你身上的负面状态~"));
        } else {
            sp.sendOverlayMessage(Component.literal("§b冰凉清澈的排泄管膜拂过手心，你目前很健康~"));
        }
        if (insideBody) {
            org.cneko.justarod.body.AscarisBodyInteractions.onExcretoryFlushedInside(sp, pos);
        }
        return InteractionResult.SUCCESS;
    }

    public static void clearCooldown(UUID playerUuid) {
        LAST_DETOX_TICK.remove(playerUuid);
    }
}
