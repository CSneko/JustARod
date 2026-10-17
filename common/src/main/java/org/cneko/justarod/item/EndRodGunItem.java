package org.cneko.justarod.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.UseEffects;
import net.minecraft.world.level.Level;
import org.cneko.justarod.JRIds;
import org.cneko.justarod.event.EndRodGunEvents;

import java.util.function.Consumer;

public final class EndRodGunItem extends Item {
    public static final int FIRE_INTERVAL = 4;
    public static final int BIG_BROTHER_FIRE_INTERVAL = 1;
    private static final String MODE_KEY = "justarod_gun_mode";

    public enum Mode {
        NORMAL, BIG_BROTHER, FOCUS;

        public Component displayName() {
            return Component.translatable("item.justarod.end_rod_gun.mode." + name().toLowerCase(java.util.Locale.ROOT));
        }
    }

    public EndRodGunItem() {
        super(JRIds.itemProps("end_rod_gun").stacksTo(1)
                .component(DataComponents.USE_EFFECTS, new UseEffects(true, false, 1.0f)));
    }

    public static Mode getMode(ItemStack stack) {
        int mode = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getIntOr(MODE_KEY, 0);
        return Mode.values()[Math.floorMod(mode, Mode.values().length)];
    }

    public static void setMode(ItemStack stack, Mode mode) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(MODE_KEY, mode.ordinal()));
    }

    public static int fireInterval(Mode mode) {
        return mode == Mode.BIG_BROTHER ? BIG_BROTHER_FIRE_INTERVAL : FIRE_INTERVAL;
    }

    public static boolean isDualWielding(LivingEntity player) {
        return player.getMainHandItem().getItem() instanceof EndRodGunItem
                && player.getOffhandItem().getItem() instanceof EndRodGunItem;
    }

    public static boolean isSpinning(LivingEntity player) {
        return player.isUsingItem() && player.getUseItem().getItem() instanceof EndRodGunItem
                && getMode(player.getUseItem()) == Mode.BIG_BROTHER && isDualWielding(player);
    }

    public static float spinAngle(LivingEntity player, float partialTick) {
        // Shared world time keeps the visible arms aligned with server shots for every observer.
        return (player.level().getGameTime() % 12 + partialTick) * 30.0f;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                Mode next = Mode.values()[(getMode(stack).ordinal() + 1) % Mode.values().length];
                for (InteractionHand held : InteractionHand.values()) {
                    if (player.getItemInHand(held).getItem() instanceof EndRodGunItem) {
                        setMode(player.getItemInHand(held), next);
                    }
                }
                EndRodGunEvents.cancelGathering(player);
                if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    serverPlayer.sendOverlayMessage(Component.translatable("item.justarod.end_rod_gun.mode_changed", next.displayName()));
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (getMode(stack) == Mode.BIG_BROTHER && !isDualWielding(player)) {
            if (!level.isClientSide()) {
                if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    serverPlayer.sendOverlayMessage(Component.translatable("item.justarod.end_rod_gun.dual_required"));
                }
            }
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        if (!level.isClientSide()) EndRodGunEvents.fire(player, getMode(stack));
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingTicks) {
        if (!level.isClientSide() && user instanceof Player player) {
            if (getMode(stack) == Mode.BIG_BROTHER && !isDualWielding(player)) {
                player.stopUsingItem();
                return;
            }
            // The server also checks the last shot time, including release/re-click and hand swaps.
            EndRodGunEvents.fire(player, getMode(stack));
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.NONE;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.justarod.end_rod_gun.mode_changed", getMode(stack).displayName())
                .withStyle(ChatFormatting.AQUA));
        tooltip.accept(Component.translatable("item.justarod.end_rod_gun.controls").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.justarod.end_rod_gun.description").withStyle(ChatFormatting.GRAY));
    }
}
