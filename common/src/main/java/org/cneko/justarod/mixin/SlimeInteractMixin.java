package org.cneko.justarod.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.cneko.justarod.entity.slime.TamedSlimeEntity;
import org.cneko.justarod.item.JRItems;
import org.cneko.justarod.item.slime.SlimeBellItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 野生史莱姆的驯服交互。
 *
 * <p>为什么用 mixin 而不是 {@code Item#interactLivingEntity}：26.x 里
 * {@code Mob#checkAndHandleImportantInteractions} 只在「命名牌 / 刷怪蛋」时会调用
 * {@code ItemStack#interactLivingEntity}，自定义物品右键实体根本不会触发。
 *
 * <p>注入点踩过的坑：{@code interact} 与 {@code mobInteract} 都是 {@code Mob} 声明的，
 * {@code Slime} <b>两个都没有覆写</b>；而 Mixin 的 {@code @Inject} 默认不搜父类方法，
 * 所以对 {@code Slime} 注入这两个名字都会在类加载时直接崩：
 * <pre>InvalidInjectionException: could not find any targets matching 'mobInteract' in Slime</pre>
 * 正确做法是注入声明它们的 {@link Mob#interact}，再用 {@code instanceof Slime} 做类型守卫
 * （对其它生物只是一次廉价的类型判断，不改变任何行为）。
 *
 * <p>驯服流程：
 * <ol>
 *   <li>手持粘液球 → size 必须为 1，累计信任值（存在 {@link SlimeTrustMixin} 注入的字段里）</li>
 *   <li>信任满后手持史莱姆铃铛 → 转化为 {@link TamedSlimeEntity} 并绑定主人</li>
 * </ol>
 */
@Mixin(Mob.class)
public class SlimeInteractMixin {

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void justarod$tameInteraction(Player player, InteractionHand hand, Vec3 hitVec,
                                          CallbackInfoReturnable<InteractionResult> cir) {
        // 类型守卫：只处理野生史莱姆
        if (!((Object) this instanceof Slime self)) return;
        if (self instanceof TamedSlimeEntity) return;   // 已驯服的走自己的 mobInteract
        ItemStack stack = player.getItemInHand(hand);

        // ===== 喂粘液球累计信任 =====
        if (stack.is(Items.SLIME_BALL)) {
            if (self.level().isClientSide()) {
                cir.setReturnValue(InteractionResult.SUCCESS);
                return;
            }
            if (self.getSize() > 1) {
                player.sendOverlayMessage(Component.translatable("justarod.slime.tame_too_big")
                        .withStyle(ChatFormatting.RED));
                cir.setReturnValue(InteractionResult.CONSUME);
                return;
            }
            int trust = SlimeBellItem.getTrust(self);
            if (trust >= TamedSlimeEntity.TRUST_TO_TAME) {
                player.sendOverlayMessage(Component.translatable("justarod.slime.trust_full",
                        TamedSlimeEntity.TRUST_TO_TAME).withStyle(ChatFormatting.GREEN));
                cir.setReturnValue(InteractionResult.CONSUME);
                return;
            }
            SlimeBellItem.addTrust(self, 1);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            int now = SlimeBellItem.getTrust(self);
            if (self.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.ITEM_SLIME,
                        self.getX(), self.getY() + self.getBbHeight() * 0.5D, self.getZ(),
                        8, 0.3D, 0.3D, 0.3D, 0.01D);
                serverLevel.playSound(null, self.blockPosition(), SoundEvents.SLIME_SQUISH_SMALL,
                        SoundSource.NEUTRAL, 0.7F, 1.3F);
            }
            if (now >= TamedSlimeEntity.TRUST_TO_TAME) {
                player.sendOverlayMessage(Component.translatable("justarod.slime.trust_ready")
                        .withStyle(ChatFormatting.GOLD));
            } else {
                player.sendOverlayMessage(Component.translatable("justarod.slime.trust_progress",
                        now, TamedSlimeEntity.TRUST_TO_TAME).withStyle(ChatFormatting.GRAY));
            }
            cir.setReturnValue(InteractionResult.CONSUME);
            return;
        }

        // ===== 信任满后用铃铛确认驯服 =====
        if (stack.is(JRItems.Companion.getSLIME_BELL())) {
            if (self.level().isClientSide()) {
                cir.setReturnValue(InteractionResult.SUCCESS);
                return;
            }
            if (self.getSize() > 1) {
                player.sendOverlayMessage(Component.translatable("justarod.slime.too_big")
                        .withStyle(ChatFormatting.RED));
                cir.setReturnValue(InteractionResult.CONSUME);
                return;
            }
            int trust = SlimeBellItem.getTrust(self);
            if (trust < TamedSlimeEntity.TRUST_TO_TAME) {
                player.sendOverlayMessage(Component.translatable("justarod.slime.need_trust",
                        trust, TamedSlimeEntity.TRUST_TO_TAME).withStyle(ChatFormatting.YELLOW));
                cir.setReturnValue(InteractionResult.CONSUME);
                return;
            }
            if (!(self.level() instanceof ServerLevel serverLevel)) {
                cir.setReturnValue(InteractionResult.CONSUME);
                return;
            }
            TamedSlimeEntity tamed = TamedSlimeEntity.fromWild(self, player);
            serverLevel.addFreshEntity(tamed);
            self.discard();
            serverLevel.sendParticles(ParticleTypes.HEART,
                    tamed.getX(), tamed.getY() + 1.0D, tamed.getZ(), 12, 0.4D, 0.4D, 0.4D, 0.02D);
            serverLevel.playSound(null, tamed.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                    SoundSource.PLAYERS, 0.8F, 1.4F);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            player.sendOverlayMessage(Component.translatable("justarod.slime.tamed")
                    .withStyle(ChatFormatting.GREEN));
            cir.setReturnValue(InteractionResult.CONSUME);
        }
    }
}
