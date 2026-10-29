package org.cneko.justarod.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.cneko.justarod.config.JRConfig;
import org.cneko.justarod.entity.EmpathyDollEntity;
import org.cneko.justarod.entity.JREntities;

import java.util.UUID;
import java.util.function.Consumer;

public final class EmpathyDollItem extends Item {
    public EmpathyDollItem(Properties properties) { super(properties.stacksTo(1)); }

    public static UUID boundPlayer(ItemStack stack) {
        var profile = stack.get(DataComponents.PROFILE);
        return profile == null ? null : profile.partialProfile().id();
    }

    public static void bind(ItemStack stack, Player target) {
        stack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(target.getGameProfile()));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!JRConfig.isEmpathyDollEnabled() || player.isSpectator() || !player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            ItemStack stack = player.getItemInHand(hand);
            if (boundPlayer(stack) == null) {
                bind(stack, player);
                player.sendOverlayMessage(Component.translatable("message.justarod.empathy_doll.bound", player.getName()));
            } else {
                stack.remove(DataComponents.PROFILE);
                player.sendOverlayMessage(Component.translatable("message.justarod.empathy_doll.unbound"));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (!JRConfig.isEmpathyDollEnabled() || player == null || player.isSpectator()) return InteractionResult.FAIL;
        Level level = context.getLevel();
        EmpathyDollEntity doll = new EmpathyDollEntity(JREntities.EMPATHY_DOLL, level);
        var pos = context.getClickLocation().add(context.getClickedFace().getStepX() * 0.36,
                context.getClickedFace().getStepY() < 0 ? -0.96 : 0,
                context.getClickedFace().getStepZ() * 0.36);
        doll.setPos(pos);
        doll.setYRot(player.getYRot() + 180);
        doll.setDollStack(context.getItemInHand());
        if (!level.noCollision(doll, doll.getBoundingBox())) return InteractionResult.FAIL;
        if (level instanceof ServerLevel server) {
            if (!player.mayUseItemAt(context.getClickedPos().relative(context.getClickedFace()), context.getClickedFace(), context.getItemInHand()))
                return InteractionResult.FAIL;
            if (!server.addFreshEntity(doll)) return InteractionResult.FAIL;
            context.getItemInHand().consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                               Consumer<Component> adder, TooltipFlag flag) {
        var profile = stack.get(DataComponents.PROFILE);
        adder.accept(profile == null ? Component.translatable("tooltip.justarod.empathy_doll.unbound")
                : Component.translatable("tooltip.justarod.empathy_doll.bound", profile.name().orElse("?")));
        adder.accept(Component.translatable("tooltip.justarod.empathy_doll.bind"));
        adder.accept(Component.translatable("tooltip.justarod.empathy_doll.place"));
        adder.accept(Component.translatable("tooltip.justarod.empathy_doll.interact"));
    }
}
