package org.cneko.justarod.pet;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.cneko.justarod.config.JRConfig;
import org.cneko.toneko.common.mod.entities.NekoEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Temporary, nearby actors for absurdity mode. Never writes NoAI, positions or AI data to saves. */
public final class PetOwnerActivity {
    public static final int DEFAULT_ACTION_INTERVAL_TICKS = 200;
    private static final int MIN_ACTION_INTERVAL_TICKS = 20, MAX_ACTION_INTERVAL_TICKS = 1200;
    private static final Map<UUID, Actor> ACTORS = new HashMap<>();

    private static final class Actor {
        final Mob npc;
        final UUID player, previousOwner;
        Vec3 anchor;
        int leaseUntil, beat;
        boolean interrupted;
        Actor(ServerPlayer p, Mob npc, int leaseTicks) {
            this.npc = npc; player = p.getUUID(); previousOwner = PetState.of(p).owner;
            anchor = npc.position(); leaseUntil = now(npc) + leaseTicks;
        }
    }
    private PetOwnerActivity() { }
    private static int now(Mob npc) { return npc.level().getServer().getTickCount(); }

    /** A proposed adopter also stays for the confirmation shot, without acquiring any ownership. */
    public static void prepare(ServerPlayer p, Mob npc, int leaseTicks) {
        if (!JRConfig.isAbsurdityEnabled() || !PetController.validOwner(npc) || p.level() != npc.level()
                || p.distanceToSqr(npc) > 64 || !p.isAlive() || p.isSpectator()) return;
        Actor old = ACTORS.get(npc.getUUID());
        if (old != null && old.player.equals(p.getUUID())) {
            old.leaseUntil = Math.max(old.leaseUntil, now(npc) + leaseTicks);
            return;
        }
        // A shared NPC keeps its current actor until that player's scene ends.
        if (old != null && participant(old) != null) return;
        ACTORS.put(npc.getUUID(), new Actor(p, npc, leaseTicks));
    }

    static void maintain(ServerPlayer p, Mob npc) {
        Actor previous = ACTORS.get(npc.getUUID());
        boolean fresh = previous == null || participant(previous) == null;
        prepare(p, npc, 0);
        if (fresh && directs(p, npc)) {
            PetState state = PetState.of(p);
            state.nextOwnerAction = Math.max(state.nextOwnerAction, p.tickCount + 20);
            state.nextFeed = Math.min(state.nextFeed, p.tickCount + 120);
        }
    }

    static boolean directs(ServerPlayer p, Mob npc) {
        Actor actor = ACTORS.get(npc.getUUID());
        return JRConfig.isAbsurdityEnabled() && actor != null && actor.player.equals(p.getUUID()) && participant(actor) == p;
    }

    private static ServerPlayer participant(Actor actor) {
        Mob npc = actor.npc;
        if (!JRConfig.isAbsurdityEnabled() || npc.isRemoved() || !PetController.validOwner(npc)) return null;
        ServerPlayer p = npc.level().getServer().getPlayerList().getPlayer(actor.player);
        if (p == null || !p.isAlive() || p.isSpectator() || p.level() != npc.level() || p.isChangingDimension()
                || p.distanceToSqr(npc) > 64 * 64) return null;
        UUID owner = PetState.of(p).owner;
        if (owner == null && !JRConfig.isPlayerTamingEnabled()) return null;
        return npc.getUUID().equals(owner) || (now(npc) < actor.leaseUntil && Objects.equals(owner, actor.previousOwner)) ? p : null;
    }

    private static boolean unsafe(Mob npc) {
        return npc.hurtTime > 0 || (npc.getLastHurtByMob() != null && npc.tickCount - npc.getLastHurtByMobTimestamp() < 100)
                || npc.getTarget() != null || npc.isOnFire() || npc.isInWater() || npc.isInLava()
                || npc.isPassenger() || npc.isSleeping() || npc.isLeashed();
    }

    /** Called at Mob.serverAiStep before either goal AI or the villager Brain can send it elsewhere. */
    public static boolean controlAi(Mob npc) {
        Actor actor = ACTORS.get(npc.getUUID());
        if (actor == null || actor.npc != npc || participant(actor) == null) return false;
        if (unsafe(npc) || !npc.onGround()) { actor.interrupted = true; return false; }
        if (npc instanceof NekoEntity neko && PetController.companion(neko) != null) {
            actor.anchor = npc.position(); // Explicit companion follow still works in filming mode.
            return false;
        }
        if (actor.interrupted || Math.abs(npc.getY() - actor.anchor.y) > 2) {
            actor.anchor = npc.position(); actor.interrupted = false;
        }
        if (npc.position().distanceToSqr(actor.anchor) > 0.25 && npc.position().distanceToSqr(actor.anchor) < 36
                && npc.level().noCollision(npc, npc.getBoundingBox().move(actor.anchor.subtract(npc.position())))) {
            npc.getNavigation().moveTo(actor.anchor.x, actor.anchor.y, actor.anchor.z, 0.7);
            npc.getNavigation().tick();
            npc.getMoveControl().tick();
            npc.getJumpControl().tick();
        } else {
            npc.getNavigation().stop();
            npc.setSpeed(0); npc.setXxa(0); npc.setZza(0); npc.setJumping(false);
            Vec3 movement = npc.getDeltaMovement();
            npc.setDeltaMovement(0, movement.y, 0);
        }
        face(npc, participant(actor));
        return true;
    }

    private static void face(Mob npc, ServerPlayer p) {
        Vec3 direction = p.getEyePosition().subtract(npc.getEyePosition());
        if (direction.horizontalDistanceSqr() < 0.01) return;
        float yaw = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90;
        float pitch = Mth.clamp((float) -Math.toDegrees(Math.atan2(direction.y, direction.horizontalDistance())), -30, 35);
        float facing = Mth.approachDegrees(npc.getYRot(), yaw, 12);
        npc.setYRot(facing); npc.setYBodyRot(facing); npc.setYHeadRot(facing);
        npc.setXRot(Mth.approachDegrees(npc.getXRot(), pitch, 8));
    }

    static void tick(MinecraftServer server) {
        ACTORS.values().removeIf(actor -> {
            if (participant(actor) != null) return false;
            if (!actor.npc.isRemoved()) {
                actor.npc.getNavigation().stop();
                PetController.stopGesture(actor.npc);
            }
            return true;
        });
        for (Actor actor : ACTORS.values()) {
            ServerPlayer p = participant(actor);
            if (p == null || unsafe(actor.npc)) continue;
            if (actor.npc instanceof NekoEntity neko && PetController.companion(neko) != null) continue;
            face(actor.npc, p); // Also animates an actor explicitly spawned with NoAI by the director.
            PetState state = PetState.of(p);
            if (!actor.npc.getUUID().equals(state.owner) || state.ownerActivityPaused || p.tickCount < state.pausedUntil
                    || p.tickCount < state.nextOwnerAction || PetController.hasPrompt(p)
                    || p.hurtTime > 0 || p.isPassenger() || p.isSleeping() || p.getAbilities().flying || !PetController.close(p, actor.npc)) continue;
            int interval = actionIntervalTicks(p);
            state.nextOwnerAction = p.tickCount + interval;
            switch (actor.beat++ % 6) {
                case 0 -> {
                    PetController.temptFromOwner(p, Math.max(80, interval));
                    PetController.voice(actor.npc);
                    PetController.message(p, "active.tempt", actor.npc.getName());
                }
                case 1 -> PetController.issueOrder(p, PetState.Mode.SIT);
                case 2 -> {
                    if (p.distanceToSqr(actor.npc) <= 9 && PetController.stroke(p, false))
                        PetController.message(p, "active.stroke", actor.npc.getName());
                    else {
                        actor.npc.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                        PetController.voice(actor.npc);
                        PetController.message(p, "active.come", actor.npc.getName());
                    }
                }
                case 3 -> PetController.issueOrder(p, PetState.Mode.FREE);
                case 4 -> {
                    if (PetController.feed(p, Math.max(60, interval))) PetController.message(p, "active.feed", actor.npc.getName());
                }
                case 5 -> PetController.issueOrder(p, PetState.Mode.FOLLOW);
            }
        }
    }

    public static int actionIntervalTicks(ServerPlayer p) {
        return Mth.clamp(PetState.of(p).ownerActionIntervalTicks, MIN_ACTION_INTERVAL_TICKS, MAX_ACTION_INTERVAL_TICKS);
    }

    /** A manual shot gets at least one configured interval, with a six-second minimum. */
    public static void defer(ServerPlayer p) {
        if (JRConfig.isAbsurdityEnabled()) PetState.of(p).nextOwnerAction = p.tickCount + Math.max(120, actionIntervalTicks(p));
    }

    public static boolean setAutomatic(ServerPlayer p, boolean enabled) {
        return setAutomatic(p, enabled, actionIntervalTicks(p) / 20.0);
    }

    public static boolean setAutomatic(ServerPlayer p, boolean enabled, double intervalSeconds) {
        if (!Double.isFinite(intervalSeconds) || intervalSeconds < 1 || intervalSeconds > 60) return false;
        Mob npc = PetController.owner(p);
        if (!JRConfig.isAbsurdityEnabled() || npc == null || npc.level() != p.level()) return false;
        PetState state = PetState.of(p);
        state.ownerActionIntervalTicks = (int) Math.round(intervalSeconds * 20);
        state.ownerActivityPaused = !enabled;
        state.nextOwnerAction = p.tickCount + 20;
        if (enabled) state.pausedUntil = p.tickCount + 20;
        maintain(p, npc);
        Actor actor = ACTORS.get(npc.getUUID());
        if (enabled && actor != null && actor.player.equals(p.getUUID())) actor.beat = 0;
        if (enabled) PetController.message(p, "active.resumed", actionIntervalTicks(p) / 20.0);
        else PetController.message(p, "active.paused");
        return true;
    }

    static void endProposal(ServerPlayer p) {
        ACTORS.values().removeIf(actor -> actor.player.equals(p.getUUID()) && !actor.npc.getUUID().equals(PetState.of(p).owner));
    }
    static void forget(ServerPlayer p) { ACTORS.values().removeIf(actor -> actor.player.equals(p.getUUID())); }
    static void remove(Mob npc) { ACTORS.remove(npc.getUUID()); }
    static void clear() { ACTORS.clear(); }
}
