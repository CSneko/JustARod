package org.cneko.justarod.item.medical;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.cneko.justarod.effect.JREffects;

/**
 * 粉白虫蜕（蜕皮产物）：
 * <ul>
 *   <li>体内对着围咽神经环右键：触发破损解剖组织温和自愈；</li>
 *   <li>直接右键涂覆自身：借用蛔虫角质层「耐酸耐酶 + 高氧亲和力」特性，清除中毒/饥饿/反胃/凋零，
 *       并赋予「暖腹余温」（3 分钟）+ 抗性提升（30 秒）+ 水下呼吸（45 秒）！</li>
 * </ul>
 */
public class AscarisMoltItem extends Item {

    private static final DustParticleOptions PEARL_PINK = new DustParticleOptions(0xFFD2E6, 0.95f);

    public AscarisMoltItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel sl)) {
            return InteractionResult.PASS;
        }
        if (!player.isCreative()) {
            stack.shrink(1);
        }

        // 借用耐酸角质层蛋白酶抑制特性清除毒素并覆上保护膜
        player.removeEffect(MobEffects.POISON);
        player.removeEffect(MobEffects.HUNGER);
        player.removeEffect(MobEffects.NAUSEA);
        player.removeEffect(MobEffects.WITHER);
        player.addEffect(new MobEffectInstance(JREffects.Companion.getWARM_BELLY_EFFECT(), 3600, 0, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 600, 0, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 900, 0, false, true, true));
        player.getCooldowns().addCooldown(stack, 20);

        sl.playSound(null, player.blockPosition(), SoundEvents.SLIME_SQUISH_SMALL, SoundSource.PLAYERS, 0.7f, 1.2f);
        sl.sendParticles(PEARL_PINK, player.getX(), player.getY() + 1.0, player.getZ(), 16, 0.4, 0.5, 0.4, 0.03);
        sl.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.2, player.getZ(), 4, 0.3, 0.25, 0.3, 0.02);

        if (player instanceof ServerPlayer sp) {
            sp.sendOverlayMessage(Component.literal(
                    "§d你覆上了耐酸柔韧的「粉白角质护膜」：毒素已中和，获得暖腹余温、抗性与水下憋气支援~♡"));
        }
        return InteractionResult.SUCCESS;
    }
}
