package org.cneko.justarod.client.pet;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import org.cneko.justarod.client.JRKeyBindings;
import org.cneko.justarod.config.JRConfig;
import org.cneko.justarod.packet.*;
import org.cneko.justarod.pet.PetState;
import java.util.HashMap;
import java.util.Map;

/** Client movement assistance never holds a key or prevents physical input from taking over. */
public final class PetClient {
    private static final class View {
        int mode, ownerId = -1, patUntil, gazeTicks;
        float lastYaw, lastPitch;
    }
    private static ClientLevel world;
    private static final Map<Integer, View> VIEWS = new HashMap<>();
    private PetClient() { }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(PetStatePayload.ID, (packet, ctx) -> ctx.client().execute(() -> accept(packet)));
        ClientPlayNetworking.registerGlobalReceiver(PetMenuPayload.ID, (packet, ctx) -> ctx.client().execute(() -> {
            if (ctx.client().player != null) ctx.client().setScreen(new PetScreen(packet));
        }));
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents.ENTITY_UNLOAD.register((entity, level) -> VIEWS.remove(entity.getId()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> { VIEWS.clear(); world = null; });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ensureWorld(client);
            if (client.player == null || client.level == null) return;
            while (JRKeyBindings.PET_CANCEL_KEY.consumeClick()) cancel();
            while (JRKeyBindings.PET_MENU_KEY.consumeClick()) {
                int id = client.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit ? hit.getEntity().getId() : -1;
                send(new PetActionPayload(PetActionPayload.OPEN, id, PetActionPayload.NONE));
            }
            View v = VIEWS.get(client.player.getId());
            if (v == null || v.gazeTicks <= 0) return;
            var p = client.player;
            if (client.screen != null || !JRConfig.isPetCameraMotion() || !p.isAlive()
                    || Math.abs(Mth.wrapDegrees(p.getYRot() - v.lastYaw)) > 0.05f || Math.abs(p.getXRot() - v.lastPitch) > 0.05f) {
                v.gazeTicks = 0; return;
            }
            Entity npc = client.level.getEntity(v.ownerId);
            if (npc == null || p.distanceToSqr(npc) > 36) { v.gazeTicks = 0; return; }
            var delta = npc.getEyePosition().subtract(p.getEyePosition());
            float yaw = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90;
            float pitch = (float) -Math.toDegrees(Math.atan2(delta.y, delta.horizontalDistance()));
            p.setYRot(p.getYRot() + Mth.clamp(Mth.wrapDegrees(yaw - p.getYRot()) * 0.2f, -4, 4));
            p.setXRot(p.getXRot() + Mth.clamp((pitch - p.getXRot()) * 0.2f, -3, 3));
            v.lastYaw = p.getYRot(); v.lastPitch = p.getXRot(); v.gazeTicks--;
        });
    }

    private static void ensureWorld(Minecraft client) {
        if (world != client.level) { VIEWS.clear(); world = client.level; }
    }
    public static void accept(PetStatePayload packet) {
        var client = Minecraft.getInstance(); ensureWorld(client);
        if (client.level == null || packet.mode() < 0 || packet.mode() >= PetState.Mode.values().length) return;
        View v = VIEWS.computeIfAbsent(packet.playerId(), id -> new View());
        v.mode = packet.mode(); v.ownerId = packet.ownerId();
        if (packet.reaction() < 0) { v.gazeTicks = 0; v.patUntil = 0; }
        if (packet.reaction() == 2) v.patUntil = (int) client.level.getGameTime() + 40;
        if (packet.reaction() == 1 && client.player != null && client.player.getId() == packet.playerId()) {
            v.gazeTicks = 12; v.lastYaw = client.player.getYRot(); v.lastPitch = client.player.getXRot();
        }
    }
    public static void send(PetActionPayload packet) {
        if (ClientPlayNetworking.canSend(PetActionPayload.ID)) ClientPlayNetworking.send(packet);
    }
    public static void cancel() {
        var client = Minecraft.getInstance();
        if (client.player != null) {
            View v = VIEWS.get(client.player.getId());
            if (v != null) { v.mode = 0; v.gazeTicks = 0; }
            send(new PetActionPayload(PetActionPayload.CANCEL, -1, PetActionPayload.NONE));
        }
    }
    public static boolean sitting(int playerId) {
        View v = VIEWS.get(playerId);
        return v != null && v.mode == PetState.Mode.SIT.ordinal();
    }

    public static Vec2 movement(Vec2 original, Input physical) {
        var client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return original;
        View v = VIEWS.get(client.player.getId());
        if (v == null || v.mode == 0) return original;
        if (physical.forward() || physical.backward() || physical.left() || physical.right() || physical.jump() || physical.shift()) {
            cancel(); return original;
        }
        var p = client.player;
        if (client.screen != null || !p.isAlive() || p.isPassenger() || p.isInWater() || p.isInLava()
                || p.getAbilities().flying || p.hurtTime > 0 || !p.onGround()) return Vec2.ZERO;
        if (v.mode == PetState.Mode.SIT.ordinal()) return Vec2.ZERO;
        Entity npc = client.level.getEntity(v.ownerId);
        if (npc == null || !npc.isAlive()) return Vec2.ZERO;
        var delta = npc.position().subtract(p.position());
        double distance = delta.horizontalDistance();
        if (distance < 2.5 || distance > 12 || Math.abs(delta.y) > 1.2) return Vec2.ZERO;
        var step = delta.normalize().scale(0.65);
        // Stop before walls, ledges, fluids and damaging blocks; recall handles longer distances.
        if (!client.level.noCollision(p, p.getBoundingBox().move(step))
                || client.level.clip(new ClipContext(p.getEyePosition(), npc.getEyePosition(),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p)).getType() != HitResult.Type.MISS) return Vec2.ZERO;
        var floor = net.minecraft.core.BlockPos.containing(p.position().add(step).add(0, -0.15, 0));
        var state = client.level.getBlockState(floor);
        if (!state.isCollisionShapeFullBlock(client.level, floor) || !client.level.getFluidState(floor.above()).isEmpty()
                || state.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK)
                || state.is(net.minecraft.world.level.block.Blocks.CAMPFIRE)
                || state.is(net.minecraft.world.level.block.Blocks.SOUL_CAMPFIRE)) return Vec2.ZERO;
        double yaw = Math.toRadians(p.getYRot());
        return new Vec2((float) ((-delta.x * Math.cos(yaw) - delta.z * Math.sin(yaw)) / distance * 0.65),
                (float) ((-delta.x * Math.sin(yaw) + delta.z * Math.cos(yaw)) / distance * 0.65));
    }

    public static void pose(HumanoidModel<?> model, AvatarRenderState avatar) {
        View v = VIEWS.get(avatar.id);
        if (v == null) return;
        if (sitting(avatar.id) && !avatar.isPassenger) {
            model.rightLeg.xRot = model.leftLeg.xRot = -1.45f;
            model.rightLeg.yRot = 0.2f; model.leftLeg.yRot = -0.2f;
            model.rightLeg.zRot = 0.06f; model.leftLeg.zRot = -0.06f;
            model.rightArm.xRot = model.leftArm.xRot = -0.25f;
        }
        if (world != null && world.getGameTime() < v.patUntil) {
            model.head.zRot += (float) Math.sin(avatar.ageInTicks * 0.2f) * 0.08f;
            model.rightArm.zRot += 0.1f; model.leftArm.zRot -= 0.1f;
        }
    }
}
