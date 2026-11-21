package org.cneko.justarod.client.disassembly;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.client.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;
import org.cneko.justarod.client.JRKeyBindings;
import org.cneko.justarod.disassembly.*;
import org.cneko.justarod.entity.JREntities;
import org.cneko.justarod.packet.*;
import java.util.*;
import static org.cneko.justarod.packet.DisassemblyViewPayload.*;

public final class DisassemblyClient {
    private static final Map<Integer,DisassemblyStatePayload> STATES=new HashMap<>();
    private static ClientLevel world;
    private static DetachedPartEntity anchor;
    private static CameraType previousPerspective;
    private static DisassemblyViewPayload view=DisassemblyViewPayload.CLOSED;
    private static boolean wasSplit;
    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(DisassemblyStatePayload.ID,(packet,ctx)->ctx.client().execute(()-> {
            ensureWorld(); STATES.put(packet.player(),packet); copyState(packet);
            if (ctx.client().screen instanceof DisassemblyScreen screen) screen.refresh();
        }));
        ClientPlayNetworking.registerGlobalReceiver(DisassemblyViewPayload.ID,(packet,ctx)->ctx.client().execute(()-> {
            ensureWorld();
            boolean wasPocket=view.mode()==INVENTORY;
            view=packet;
            if (ctx.client().player!=null) DisassemblyState.of(ctx.client().player).dual=packet.dual();
            if (packet.mode()<0) {
                restoreCamera(); SplitBodyView.close();
                if (ctx.client().screen instanceof PocketViewScreen) ctx.client().setScreen(null);
            } else if (packet.mode()==INVENTORY) {
                restoreCamera();
                if (!wasPocket && (ctx.client().screen==null || ctx.client().screen instanceof DisassemblyScreen)) ctx.client().setScreen(new PocketViewScreen());
            } else if (ctx.client().screen instanceof PocketViewScreen) ctx.client().setScreen(null);
        }));
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)-> { clear(); world=null; });
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents.ENTITY_UNLOAD.register((e,l)->STATES.remove(e.getId()));
        ClientTickEvents.END_CLIENT_TICK.register(client-> {
            ensureWorld(); if (client.player==null || client.level==null) return;
            while (JRKeyBindings.DISASSEMBLY_MENU_KEY.consumeClick()) client.setScreen(new DisassemblyScreen());
            while (JRKeyBindings.DETACH_HEAD_KEY.consumeClick()) send(DisassemblyActionPayload.DETACH,BodyPart.HEAD.ordinal());
            var state=STATES.get(client.player.getId()); if (state!=null) copyState(state);
            if (hasView() && (client.options.keyShift.isDown() || JRKeyBindings.PET_CANCEL_KEY.consumeClick())) { send(DisassemblyActionPayload.EXIT,0); return; }
            if (!hasView() || !client.player.isAlive() || view.mode()==INVENTORY) { restoreCamera(); return; }
            if (client.level.getEntity(view.target())==null) { restoreCamera(); return; }
            if (previousPerspective==null) previousPerspective=client.options.getCameraType();
            client.options.setCameraType(CameraType.FIRST_PERSON);
            updateAnchor(1);
            client.setCameraEntity(view.dual()?client.player:anchor);
        });
    }
    private static void clear() { restoreCamera(); SplitBodyView.close(); view=DisassemblyViewPayload.CLOSED; STATES.clear(); }
    private static void ensureWorld() {
        var client=Minecraft.getInstance();
        if (world!=client.level) { clear(); world=client.level; }
    }
    private static void copyState(DisassemblyStatePayload packet) {
        if (world==null || !(world.getEntity(packet.player()) instanceof net.minecraft.world.entity.player.Player player)) return;
        var s=DisassemblyState.of(player); s.detached.clear(); s.camera=packet.camera(); s.random=packet.random();
        s.installedEyes.clear(); s.installedEyes.addAll(packet.eyes());
        if (player==Minecraft.getInstance().player) s.dual=view.dual();
        for (BodyPart part:BodyPart.values()) if ((packet.mask()&part.bit())!=0) s.detached.put(part,new UUID(0,part.ordinal()+1));
    }
    public static void restoreCamera() {
        var client=Minecraft.getInstance();
        if (anchor!=null || wasSplit) { client.setCameraEntity(client.player); anchor=null; wasSplit=false; }
        if (previousPerspective!=null) { client.options.setCameraType(previousPerspective); previousPerspective=null; }
    }
    public static DisassemblyViewPayload view() { return view; }
    public static boolean hasView() { return view.mode()>=0; }
    /** Full remote observation freezes the body; a split view leaves normal controls available. */
    public static boolean viewing() { return hasView() && !view.dual(); }
    public static boolean splitting() { return hasView() && view.dual() && view.mode()!=INVENTORY; }
    public static boolean remoteFrame() { return viewing() || (splitting() && SplitBodyView.remoteFrame()); }
    public static void beginCamera(Camera camera, DeltaTracker tracker) {
        if (!hasView() || view.mode()==INVENTORY) {
            if (wasSplit) { camera.setEntity(Minecraft.getInstance().getCameraEntity()); wasSplit=false; }
            return;
        }
        updateAnchor(tracker.getGameTimeDeltaPartialTick(false));
        if (anchor==null) return;
        if (splitting()) {
            SplitBodyView.beginFrame(); wasSplit=true;
            camera.setEntity(SplitBodyView.remoteFrame()?anchor:Minecraft.getInstance().player);
        }
    }
    public static void restoreBodyEntity() {
        var client=Minecraft.getInstance();
        if (splitting() && client.player!=null) client.gameRenderer.getMainCamera().setEntity(client.player);
    }
    public static Vec3 anchorEye() { return anchor==null?null:anchor.getEyePosition(); }
    private static void updateAnchor(float partialTick) {
        var client=Minecraft.getInstance();
        if (client.level==null || client.player==null) return;
        Entity target=client.level.getEntity(view.target()); if (target==null) return;
        if (anchor==null) { anchor=new DetachedPartEntity(JREntities.DETACHED_PART,client.level); anchor.setNoGravity(true); }
        float yaw=target.getViewYRot(partialTick), pitch=target.getViewXRot(partialTick);
        double angle=Math.toRadians(yaw);
        Vec3 forward=new Vec3(-Math.sin(angle),0,Math.cos(angle));
        Vec3 eye;
        if (view.mode()==LEFT_HAND || view.mode()==RIGHT_HAND) {
            double side=view.mode()==LEFT_HAND?-0.42:0.42;
            eye=target.getPosition(partialTick).add(0,target.getEyeHeight()-0.5,0).add(forward.scale(0.6))
                    .add(Math.cos(angle)*side,0,Math.sin(angle)*side);
        } else if (view.mode()==MOUNTED) eye=target.getPosition(partialTick).add(0,target.getBbHeight()*0.65,0).add(forward.scale(0.35));
        else eye=target.getEyePosition(partialTick);
        if (view.mode()!=MOUNTED && !view.dual()) { yaw=client.player.getYRot(); pitch=client.player.getXRot(); }
        anchor.setPos(eye.add(0,-anchor.getEyeHeight(),0)); anchor.setYRot(yaw); anchor.setXRot(pitch); anchor.setOldPosAndRot();
    }
    public static float anchorYaw() { return anchor==null?0:anchor.getYRot(); }
    public static float anchorPitch() { return anchor==null?0:anchor.getXRot(); }
    public static DisassemblyStatePayload state(int id) { return STATES.getOrDefault(id,new DisassemblyStatePayload(id,0,-1,true)); }
    public static boolean missing(int id,BodyPart part) { return (state(id).mask()&part.bit())!=0; }
    public static boolean missingArm(HumanoidArm arm) {
        var p=Minecraft.getInstance().player;
        return p!=null && missing(p.getId(),arm==HumanoidArm.LEFT?BodyPart.LEFT_ARM:BodyPart.RIGHT_ARM);
    }
    public static void send(int action,int part) { if (ClientPlayNetworking.canSend(DisassemblyActionPayload.ID)) ClientPlayNetworking.send(new DisassemblyActionPayload(action,part)); }
    public static void pose(HumanoidModel<?> model,AvatarRenderState state) {
        model.head.skipDraw=missing(state.id,BodyPart.HEAD); model.hat.skipDraw=model.head.skipDraw;
        model.leftArm.skipDraw=missing(state.id,BodyPart.LEFT_ARM); model.rightArm.skipDraw=missing(state.id,BodyPart.RIGHT_ARM);
        model.leftLeg.skipDraw=missing(state.id,BodyPart.LEFT_LEG); model.rightLeg.skipDraw=missing(state.id,BodyPart.RIGHT_LEG);
        if (model instanceof PlayerModel player) {
            player.leftSleeve.skipDraw=model.leftArm.skipDraw; player.rightSleeve.skipDraw=model.rightArm.skipDraw;
            player.leftPants.skipDraw=model.leftLeg.skipDraw; player.rightPants.skipDraw=model.rightLeg.skipDraw;
        }
        if (model.leftLeg.skipDraw != model.rightLeg.skipDraw) { model.body.zRot+=Math.sin(state.ageInTicks*0.35)*0.07f; }
    }
}
