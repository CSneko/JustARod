package org.cneko.justarod.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.cneko.justarod.config.JRConfig;
import org.cneko.justarod.effect.JREffects;
import org.cneko.justarod.entity.Pregnant;
import org.cneko.justarod.item.JRItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 26.1.2：Player#eat 已被移除，进食由 CONSUMABLE 组件驱动，统一从
 * ItemStack#finishUsingItem 入口处理“食用后”的 JustARod 专属效果。
 */
@Mixin(ItemStack.class)
public class ItemStackMixin {

    @Inject(method = "finishUsingItem", at = @At("HEAD"))
    private void justarod$onFinishUsingItem(Level level, LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
        if (level.isClientSide()) {
            return;
        }
        if (!(user instanceof Player player)) {
            return;
        }
        if (!(player instanceof Pregnant pregnant)) {
            return;
        }

        ItemStack stack = (ItemStack) (Object) this;

        if (stack.is(JRItems.Companion.getSHENBAO())) {
            // 肾宝提神：消除一部分疲劳
            if (pregnant.getFatigue() > 0) {
                pregnant.setFatigue(pregnant.getFatigue() - JRConfig.getShenbaoReliefTicks());
                player.sendSystemMessage(Component.nullToEmpty("§d肾宝入喉，感觉精神了不少……"));
            }
        }

        if (stack.is(Items.MILK_BUCKET)) {
            // 如果有HPV且在3天内
            if (pregnant.getHPV() > 0 && pregnant.getHPV() < 20L * 60 * 20 * 3) {
                pregnant.setHPV(0);
                player.removeEffect(JREffects.Companion.getHPV_EFFECT());
            }
        }

        if (stack.is(Items.ENCHANTED_GOLDEN_APPLE)) {
            // 如果有HPV且在6天内
            if (pregnant.getHPV() > 0 && pregnant.getHPV() < 20L * 60 * 20 * 6) {
                pregnant.setHPV(0);
                player.removeEffect(JREffects.Companion.getHPV_EFFECT());
            }
        }
    }
}
