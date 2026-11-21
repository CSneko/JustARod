package org.cneko.justarod.disassembly;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;
import org.cneko.justarod.entity.JREntities;
import org.cneko.justarod.packet.*;
import java.util.*;

public final class DisassemblyController {
    private static final Identifier SPEED = Identifier.fromNamespaceAndPath("justarod", "missing_legs");
    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(DisassemblyActionPayload.ID,
            (packet, ctx) -> ctx.server().execute(() -> handle(ctx.player(), packet.action(), packet.part())));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var state = DisassemblyState.of(handler.player);
            state.camera = -1; state.viewedPart = null; state.lastView = null; state.dual = false;
            sync(handler.player);
        });
        EntityTrackingEvents.START_TRACKING.register((entity, observer) -> { if (entity instanceof ServerPlayer p) send(observer, p); });
        ServerPlayerEvents.COPY_FROM.register((old, replacement, alive) -> {
            // Mounted foreign eyes remain on their host across respawns.
            DisassemblyState.of(replacement).installedEyes.addAll(DisassemblyState.of(old).installedEyes);
            // A death restores the body; dimension transfers retain the same slot identities.
            if (alive) {
                var from = DisassemblyState.of(old); var to = DisassemblyState.of(replacement);
                to.detached.putAll(from.detached); to.random = from.random;
            }
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((old, replacement, alive) -> sync(replacement));
        ServerTickEvents.END_SERVER_TICK.register(DisassemblyController::tick);
        UseBlockCallback.EVENT.register((p,l,h,hit) -> blocked(p,h) ? InteractionResult.FAIL : InteractionResult.PASS);
        UseItemCallback.EVENT.register((p,l,h) -> blocked(p,h) ? InteractionResult.FAIL : InteractionResult.PASS);
        UseEntityCallback.EVENT.register((p,l,h,e,hit) -> {
            if (p instanceof ServerPlayer actor && e instanceof ServerPlayer host && p.isShiftKeyDown()
                    && p.getItemInHand(h).isEmpty() && p.distanceToSqr(host) <= 16 && DisassemblyVision.remove(actor,host)) return InteractionResult.SUCCESS;
            if (e instanceof DetachedPartEntity && (DisassemblyState.of(p).camera < 0 || DisassemblyState.of(p).dual)) return InteractionResult.PASS; // Picking up a part is possible with a remaining hand.
            return blocked(p,h) ? InteractionResult.FAIL : InteractionResult.PASS;
        });
        AttackBlockCallback.EVENT.register((p,l,h,pos,face) -> blocked(p,h) ? InteractionResult.FAIL : InteractionResult.PASS);
        AttackEntityCallback.EVENT.register((p,l,h,e,hit) -> e instanceof DetachedPartEntity && (DisassemblyState.of(p).camera < 0 || DisassemblyState.of(p).dual) ? InteractionResult.PASS
                : blocked(p,h) ? InteractionResult.FAIL : InteractionResult.PASS);
    }
    public static boolean blocked(Player player, InteractionHand hand) {
        var s = DisassemblyState.of(player);
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        return (s.camera >= 0 && !s.dual) || s.missing(arm == HumanoidArm.LEFT ? BodyPart.LEFT_ARM : BodyPart.RIGHT_ARM);
    }
    public static boolean valid(ItemStack stack, MinecraftServer server) {
        if (!(stack.getItem() instanceof DetachedPartItem item)) return false;
        UUID id = DetachedPartItem.owner(stack), token = DetachedPartItem.token(stack);
        if (id == null || token == null) return false;
        ServerPlayer owner = server.getPlayerList().getPlayer(id);
        // An offline owner's props remain persistent, but cannot be used to manufacture body slots.
        return owner == null || token.equals(DisassemblyState.of(owner).detached.get(item.part));
    }
    public static void message(ServerPlayer p, String key) { p.sendOverlayMessage(Component.translatable("disassembly.justarod." + key)); }
    public static boolean detach(ServerPlayer p, BodyPart part) {
        var s = DisassemblyState.of(p);
        if (!p.isAlive() || p.isSpectator() || s.missing(part)) return false;
        UUID token = UUID.randomUUID(); s.detached.put(part, token);
        var stack = DetachedPartItem.create(p, part, token);
        // Put the opening shot's head into a usable hand immediately.
        InteractionHand usable = blocked(p, InteractionHand.MAIN_HAND) || !p.getMainHandItem().isEmpty()
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        if (!blocked(p, usable) && p.getItemInHand(usable).isEmpty()) p.setItemInHand(usable, stack);
        else if (!p.getInventory().add(stack)) p.drop(stack, false);
        pop(p); sync(p); return true;
    }
    public static boolean attach(ServerPlayer p, ItemStack stack) {
        if (!(stack.getItem() instanceof DetachedPartItem item) || !p.getUUID().equals(DetachedPartItem.owner(stack))
                || !Objects.equals(DisassemblyState.of(p).detached.get(item.part), DetachedPartItem.token(stack))
                || DetachedPartItem.token(stack) == null) { message(p, "wrong_part"); return false; }
        DisassemblyState.of(p).detached.remove(item.part); stack.shrink(1);
        exitView(p); pop(p); sync(p); return true;
    }
    public static List<DetachedPartEntity> entities(ServerPlayer p) {
        List<DetachedPartEntity> result = new ArrayList<>();
        for (var level : p.level().getServer().getAllLevels()) for (Entity entity : level.getAllEntities())
            if (entity instanceof DetachedPartEntity part && p.getUUID().equals(DetachedPartItem.owner(part.stack()))
                    && valid(part.stack(), level.getServer())) result.add(part);
        return result;
    }
    public static void recall(ServerPlayer p) {
        exitView(p);
        for (var part : entities(p)) if (part.level() == p.level()) {
            part.stop(); part.setPos(p.position().add((part.part().ordinal() % 3 - 1) * 0.7, 0.2, 1)); part.setDeltaMovement(Vec3.ZERO);
        }
        message(p, "recalled");
    }
    public static void reset(ServerPlayer p) {
        exitView(p);
        for (var part : entities(p)) { part.stop(); part.releaseEquipment((net.minecraft.server.level.ServerLevel)part.level()); part.discard(); }
        DisassemblyState.of(p).detached.clear();
        for (ServerPlayer carrier : p.level().getServer().getPlayerList().getPlayers())
            for (int i = 0; i < carrier.getInventory().getContainerSize(); i++) {
                var stack = carrier.getInventory().getItem(i);
                if (stack.getItem() instanceof DetachedPartItem && p.getUUID().equals(DetachedPartItem.owner(stack))) carrier.getInventory().setItem(i, ItemStack.EMPTY);
            }
        pop(p); sync(p);
    }
    public static void deploy(ServerPlayer p, BodyPart part) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            var stack = p.getInventory().getItem(i);
            if (stack.getItem() == DisassemblyItems.get(part) && p.getUUID().equals(DetachedPartItem.owner(stack)) && valid(stack,p.level().getServer())) {
                var e = new DetachedPartEntity(JREntities.DETACHED_PART, p.level());
                e.setStack(stack); e.setYRot(p.getYRot());
                var hit = p.pick(4, 0, false);
                Vec3 pos = hit.getLocation();
                if (hit instanceof BlockHitResult b && hit.getType() == HitResult.Type.BLOCK)
                    pos = pos.add(b.getDirection().getStepX() * 0.3, b.getDirection().getStepY() < 0 ? -0.8 : 0.02, b.getDirection().getStepZ() * 0.3);
                e.setPos(pos);
                if (p.level().noCollision(e,e.getBoundingBox()) && p.level().addFreshEntity(e)) stack.shrink(1);
                return;
            }
        }
        message(p, "carry_first");
    }
    public static void view(ServerPlayer p, DetachedPartEntity entity) {
        if (!entity.part().camera() || p.level() != entity.level() || !p.getUUID().equals(DetachedPartItem.owner(entity.stack()))
                || p.distanceToSqr(entity) > 64 * 64) { message(p,"view_missing"); return; }
        DisassemblyVision.start(p,entity.part(),false);
    }
    public static void exitView(ServerPlayer p) {
        var s = DisassemblyState.of(p);
        if (s.camera == -1 && s.viewedPart == null) return;
        s.camera = -1; s.viewedPart = null; s.dual = false; s.lastView = null;
        if (ServerPlayNetworking.canSend(p,DisassemblyViewPayload.ID)) ServerPlayNetworking.send(p,DisassemblyViewPayload.CLOSED);
        sync(p);
    }
    public static void mine(ServerPlayer p, BodyPart part) {
        var hit = p.pick(8, 0, false);
        if (!(hit instanceof BlockHitResult block) || hit.getType() != HitResult.Type.BLOCK) { message(p,"look_block"); return; }
        for (var entity : entities(p)) if (entity.part() == part && part.arm() && entity.level() == p.level() && !entity.tool().isEmpty()
                && entity.distanceToSqr(p) < 16 * 16) { entity.mine(block.getBlockPos(),block.getDirection()); return; }
        message(p,"equip_first");
    }
    public static void handle(ServerPlayer p, int action, int index) {
        var part = BodyPart.byIndex(index);
        if (action == DisassemblyActionPayload.EXIT) { exitView(p); return; }
        if (!p.isAlive() || p.isSpectator()) return;
        switch(action) {
            case DisassemblyActionPayload.DETACH -> { if (part != null) detach(p,part); }
            case DisassemblyActionPayload.RECALL -> recall(p);
            case DisassemblyActionPayload.VIEW -> {
                DisassemblyVision.start(p,part,false);
            }
            case DisassemblyActionPayload.DUAL -> { if (part != null && part.eye()) DisassemblyVision.start(p,part,true); }
            case DisassemblyActionPayload.REMOVE_EYES -> DisassemblyVision.remove(p,p);
            case DisassemblyActionPayload.MINE -> { if (part != null && part.arm()) mine(p,part); }
            case DisassemblyActionPayload.RANDOM -> { var s = DisassemblyState.of(p); s.random = !s.random; sync(p); }
            case DisassemblyActionPayload.ATTACH -> { // Reattach a carried part without needing to use a missing hand.
                for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                    var stack = p.getInventory().getItem(i);
                    if (stack.getItem() instanceof DetachedPartItem item && item.part == part && p.getUUID().equals(DetachedPartItem.owner(stack))) { attach(p,stack); return; }
                }
                message(p,"carry_first");
            }
            case DisassemblyActionPayload.PLACE -> { if (part != null) deploy(p,part); }
        }
    }
    public static void sync(ServerPlayer p) {
        send(p,p);
        for (ServerPlayer observer : PlayerLookup.tracking(p)) send(observer,p);
    }
    private static void send(ServerPlayer observer, ServerPlayer p) {
        if (ServerPlayNetworking.canSend(observer,DisassemblyStatePayload.ID)) {
            var s = DisassemblyState.of(p);
            ServerPlayNetworking.send(observer,new DisassemblyStatePayload(p.getId(),s.mask(),s.camera,s.random,s.installedEyes.stream().map(ItemStack::copy).toList()));
        }
    }
    private static void pop(ServerPlayer p) {
        p.level().playSound(null,p.blockPosition(),SoundEvents.CHICKEN_EGG,SoundSource.PLAYERS,0.6f,1.5f);
        p.level().sendParticles(ParticleTypes.POOF,p.getX(),p.getY()+1,p.getZ(),8,0.2,0.3,0.2,0.01);
    }
    private static void tick(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            var s = DisassemblyState.of(p);
            var speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
            int legs = (s.missing(BodyPart.LEFT_LEG) ? 1 : 0) + (s.missing(BodyPart.RIGHT_LEG) ? 1 : 0);
            double penalty = legs == 2 ? -0.75 : -0.35;
            var modifier = speed.getModifier(SPEED);
            if (legs == 0) { if (modifier != null) speed.removeModifier(SPEED); }
            else if (modifier == null || modifier.amount() != penalty) {
                speed.removeModifier(SPEED); speed.addTransientModifier(new AttributeModifier(SPEED,penalty,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
            if (s.installedEyes.removeIf(eye -> !valid(eye,server))) sync(p);
            DisassemblyVision.update(p);
            if (p.tickCount % 100 == 0) sync(p);
        }
    }
}
