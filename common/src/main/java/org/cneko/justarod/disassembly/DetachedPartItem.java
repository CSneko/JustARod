package org.cneko.justarod.disassembly;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.cneko.justarod.entity.JREntities;
import java.util.UUID;
import java.util.function.Consumer;

public final class DetachedPartItem extends Item {
    public final BodyPart part;
    public DetachedPartItem(Properties props, BodyPart part) { super(props.stacksTo(1)); this.part = part; }
    public static ItemStack create(ServerPlayer owner, BodyPart part, UUID token) {
        var stack = new ItemStack(DisassemblyItems.get(part));
        stack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(owner.getGameProfile()));
        var tag = new CompoundTag(); tag.putString("PartIdentity", token.toString());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }
    public static UUID owner(ItemStack stack) {
        var profile = stack.get(DataComponents.PROFILE);
        return profile == null ? null : profile.partialProfile().id();
    }
    public static UUID token(ItemStack stack) {
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        try { return UUID.fromString(data.getStringOr("PartIdentity", "")); }
        catch (IllegalArgumentException ignored) { return null; }
    }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        if (player instanceof ServerPlayer p) DisassemblyController.attach(p, player.getItemInHand(hand));
        return InteractionResult.SUCCESS;
    }
    @Override public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof ServerPlayer owner)) return target instanceof Player ? InteractionResult.SUCCESS : InteractionResult.PASS;
        if (part.eye() && player instanceof ServerPlayer actor && !owner.getUUID().equals(owner(stack)))
            return DisassemblyVision.install(actor,owner,stack) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        return DisassemblyController.attach(owner, stack) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || player.isSpectator()) return InteractionResult.FAIL;
        if (player.isShiftKeyDown()) {
            if (player instanceof ServerPlayer p) DisassemblyController.attach(p, context.getItemInHand());
            return InteractionResult.SUCCESS;
        }
        var entity = new DetachedPartEntity(JREntities.DETACHED_PART, context.getLevel());
        var pos = context.getClickLocation().add(context.getClickedFace().getStepX() * 0.3,
                context.getClickedFace().getStepY() < 0 ? -0.8 : 0.01, context.getClickedFace().getStepZ() * 0.3);
        entity.setPos(pos); entity.setYRot(player.getYRot() + 180); entity.setStack(context.getItemInHand());
        if (!context.getLevel().noCollision(entity, entity.getBoundingBox())) return InteractionResult.FAIL;
        if (context.getLevel() instanceof ServerLevel level) {
            if (!player.mayUseItemAt(context.getClickedPos().relative(context.getClickedFace()), context.getClickedFace(), context.getItemInHand())) return InteractionResult.FAIL;
            if (!DisassemblyController.valid(context.getItemInHand(), level.getServer())) return InteractionResult.FAIL;
            if (!level.addFreshEntity(entity)) return InteractionResult.FAIL;
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        var profile = stack.get(DataComponents.PROFILE);
        if (profile != null) lines.accept(Component.translatable("disassembly.justarod.owner", profile.name().orElse("?")));
        lines.accept(Component.translatable("disassembly.justarod.item_hint"));
    }
}
