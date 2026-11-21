package org.cneko.justarod.disassembly;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.cneko.justarod.packet.DisassemblyViewPayload;
import java.util.*;
import static org.cneko.justarod.packet.DisassemblyViewPayload.*;

/** Resolve the slot's identity, rather than a temporary entity ID, throughout a handoff. */
public final class DisassemblyVision {
    public static boolean matches(ItemStack stack, ServerPlayer owner, BodyPart part) {
        return stack.getItem() == DisassemblyItems.get(part) && owner.getUUID().equals(DetachedPartItem.owner(stack))
                && Objects.equals(DisassemblyState.of(owner).detached.get(part), DetachedPartItem.token(stack))
                && DetachedPartItem.token(stack) != null;
    }
    public static DisassemblyViewPayload locate(ServerPlayer owner, BodyPart part, boolean dual) {
        if (part == null || !part.camera() || !DisassemblyState.of(owner).missing(part)) return null;
        for (ServerPlayer carrier : owner.level().getServer().getPlayerList().getPlayers()) {
            if (!carrier.isAlive() || carrier.level() != owner.level() || carrier.distanceToSqr(owner) > 64*64) continue;
            for (var eye : DisassemblyState.of(carrier).installedEyes) if (matches(eye,owner,part))
                return new DisassemblyViewPayload(MOUNTED,carrier.getId(),carrier.getName().getString(),dual,List.of());
            for (InteractionHand hand : InteractionHand.values()) if (matches(carrier.getItemInHand(hand),owner,part)) {
                HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? carrier.getMainArm() : carrier.getMainArm().getOpposite();
                return new DisassemblyViewPayload(arm == HumanoidArm.LEFT ? LEFT_HAND : RIGHT_HAND,carrier.getId(),carrier.getName().getString(),dual,List.of());
            }
            for (int i=0;i<carrier.getInventory().getContainerSize();i++) if (matches(carrier.getInventory().getItem(i),owner,part)) {
                List<ItemStack> contents = new ArrayList<>();
                for (int j=0;j<carrier.getInventory().getContainerSize();j++) contents.add(carrier.getInventory().getItem(j).copy());
                return new DisassemblyViewPayload(INVENTORY,carrier.getId(),carrier.getName().getString(),dual,List.copyOf(contents));
            }
        }
        for (Entity entity : owner.level().getEntitiesOfClass(Entity.class,owner.getBoundingBox().inflate(64),
                e -> e instanceof DetachedPartEntity || e instanceof ItemEntity)) {
            ItemStack stack = entity instanceof DetachedPartEntity p ? p.stack() : ((ItemEntity)entity).getItem();
            if (matches(stack,owner,part) && owner.distanceToSqr(entity) <= 64*64)
                return new DisassemblyViewPayload(WORLD,entity.getId(),"",dual,List.of());
        }
        return null;
    }
    public static void start(ServerPlayer owner, BodyPart part, boolean dual) {
        var view = locate(owner,part,dual);
        if (view == null) { DisassemblyController.message(owner,"view_missing"); return; }
        var state = DisassemblyState.of(owner); state.viewedPart = part; state.dual = dual;
        publish(owner,view); DisassemblyController.message(owner,"view_hint");
    }
    public static void update(ServerPlayer owner) {
        var state=DisassemblyState.of(owner);
        if (state.viewedPart == null) return;
        var view=locate(owner,state.viewedPart,state.dual);
        if (view == null || !owner.isAlive() || owner.hurtTime > 0 || owner.isPassenger() || owner.isSleeping()) {
            DisassemblyController.exitView(owner); return;
        }
        // Locations change immediately; pocket snapshots refresh five times per second.
        if (state.lastView == null || view.mode()!=state.lastView.mode() || view.target()!=state.lastView.target()
                || owner.tickCount % 4 == 0) publish(owner,view);
    }
    private static void publish(ServerPlayer owner, DisassemblyViewPayload view) {
        var s=DisassemblyState.of(owner);
        if (s.camera != view.target() || s.lastView == null || s.dual != view.dual()) {
            s.camera=view.target(); DisassemblyController.sync(owner);
        }
        if (!view.sameView(s.lastView) && ServerPlayNetworking.canSend(owner,DisassemblyViewPayload.ID))
            ServerPlayNetworking.send(owner,view);
        s.lastView=view;
    }
    public static boolean install(ServerPlayer actor, ServerPlayer target, ItemStack stack) {
        if (!(stack.getItem() instanceof DetachedPartItem item) || !item.part.eye() || actor == target
                || !matches(stack,actor,item.part) || actor.level()!=target.level() || actor.distanceToSqr(target)>16 || !target.isAlive()) return false;
        var eyes=DisassemblyState.of(target).installedEyes;
        if (eyes.size()>=2 || eyes.stream().anyMatch(e->e.getItem()==stack.getItem())) { DisassemblyController.message(actor,"eye_occupied"); return false; }
        eyes.add(stack.split(1)); DisassemblyController.sync(target);
        actor.sendOverlayMessage(Component.translatable("disassembly.justarod.eye_installed",target.getName()));
        start(actor,item.part,true); return true;
    }
    public static boolean remove(ServerPlayer actor, ServerPlayer target) {
        var eyes=DisassemblyState.of(target).installedEyes;
        boolean changed=false;
        var it=eyes.iterator();
        while (it.hasNext()) {
            var eye=it.next();
            if (actor==target || actor.getUUID().equals(DetachedPartItem.owner(eye))) {
                if (!actor.getInventory().add(eye)) actor.drop(eye,false);
                it.remove(); changed=true;
            }
        }
        if (changed) DisassemblyController.sync(target);
        return changed;
    }
}
