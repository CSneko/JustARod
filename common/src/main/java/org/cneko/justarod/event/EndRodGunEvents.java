package org.cneko.justarod.event;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.cneko.justarod.entity.EndRodProjectile;
import org.cneko.justarod.entity.JREntities;
import org.cneko.justarod.item.EndRodGunItem;
import org.cneko.justarod.item.EndRodGunItem.Mode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class EndRodGunEvents {
    public static final int SALVO_SIZE = 20;
    public static final int ATTACK_INTERVAL = 2;
    public static final double LOCK_RANGE = 48;
    private static final int GATHER_IDLE_TIMEOUT = 600;
    private static final Map<UUID, GunState> STATES = new HashMap<>();

    private EndRodGunEvents() { }

    private static final class GunState {
        long lastShot = Long.MIN_VALUE / 2;
        Salvo salvo;
    }

    private static final class Salvo {
        final LivingEntity target;
        final List<EndRodProjectile> rods = new ArrayList<>();
        int launched;
        long nextAttack;
        long lastAdded;
        boolean releasing;

        Salvo(LivingEntity target) { this.target = target; }

        void discard() {
            for (EndRodProjectile rod : rods) if (rod.isGathering()) rod.discard();
        }
    }

    public static void init() {
        net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (!player.isSpectator() && player.getItemInHand(hand).getItem() instanceof EndRodGunItem gun) {
                return gun.use(level, player, hand);
            }
            return net.minecraft.world.InteractionResult.PASS;
        });
        ServerTickEvents.END_SERVER_TICK.register(EndRodGunEvents::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> STATES.clear());
    }

    public static void cancelGathering(Player player) {
        GunState state = STATES.get(player.getUUID());
        if (state != null && state.salvo != null && state.salvo.rods.size() < SALVO_SIZE) {
            state.salvo.discard();
            state.salvo = null;
        }
    }

    /** One trigger drives both hands; cooldown is checked on the server across all modes and hand swaps. */
    public static void fire(Player player, Mode mode) {
        if (!(player.level() instanceof ServerLevel level) || !player.isAlive() || player.isSpectator()) return;
        if (mode == Mode.BIG_BROTHER && !EndRodGunItem.isDualWielding(player)) return;
        GunState state = STATES.computeIfAbsent(player.getUUID(), ignored -> new GunState());
        long now = level.getServer().getTickCount();
        if (now - state.lastShot < EndRodGunItem.fireInterval(mode)) return;
        state.lastShot = now;

        if (mode == Mode.FOCUS) {
            if (state.salvo == null) {
                LivingEntity target = findTarget(player);
                if (target == null) return;
                state.salvo = new Salvo(target);
            }
            if (state.salvo.rods.size() >= SALVO_SIZE) return;
        } else {
            cancelGathering(player);
        }

        int firedHands = 0;
        for (InteractionHand hand : InteractionHand.values()) {
            if (!(player.getItemInHand(hand).getItem() instanceof EndRodGunItem)) continue;
            if (mode == Mode.FOCUS && state.salvo.rods.size() >= SALVO_SIZE) break;
            Vec3 direction;
            Vec3 muzzle;
            boolean right = (hand == InteractionHand.MAIN_HAND) == (player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT);
            if (mode == Mode.BIG_BROTHER) {
                direction = Vec3.directionFromRotation(0, EndRodGunItem.spinAngle(player, 0) + (right ? -90 : 90));
                muzzle = player.getEyePosition().add(0, -0.25, 0).add(direction.scale(0.85));
            } else {
                direction = player.getLookAngle();
                Vec3 side = Vec3.directionFromRotation(0, player.getYRot() - 90).scale(right ? 0.3 : -0.3);
                muzzle = player.getEyePosition().add(side).add(direction.scale(0.25)).add(0, -0.15, 0);
            }
            EndRodProjectile rod = new EndRodProjectile(JREntities.END_ROD_PROJECTILE, level);
            rod.setOwner(player);
            rod.setPos(muzzle);
            rod.shoot(direction.x, direction.y, direction.z, (float) EndRodProjectile.SPEED, 0);
            if (mode == Mode.FOCUS) {
                rod.gatherAround(state.salvo.target, state.salvo.rods.size());
                state.salvo.rods.add(rod);
                state.salvo.lastAdded = now;
            }
            level.addFreshEntity(rod);
            firedHands |= hand == InteractionHand.MAIN_HAND ? 1 : 2;
        }
        if (firedHands != 0) {
            level.playSound(null, player.blockPosition(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.3f, 1.6f);
            if (player instanceof net.minecraft.server.level.ServerPlayer shooter) {
                var packet = new org.cneko.justarod.packet.EndRodGunShotPayload(player.getId(), firedHands, EndRodGunItem.fireInterval(mode));
                for (var observer : net.fabricmc.fabric.api.networking.v1.PlayerLookup.tracking(player)) {
                    if (net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(observer, packet.type())) {
                        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(observer, packet);
                    }
                }
                if (net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(shooter, packet.type())) {
                    net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(shooter, packet);
                }
            }
        }
    }

    /** Ray/expanded hitbox selection gives slight aim tolerance, while blocks still occlude targets. */
    public static LivingEntity findTarget(Player player) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(LOCK_RANGE));
        Vec3 blockHit = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getLocation();
        double nearest = start.distanceToSqr(blockHit);
        LivingEntity chosen = null;
        for (LivingEntity candidate : player.level().getEntitiesOfClass(LivingEntity.class, new AABB(start, end).inflate(1.25),
                entity -> entity != player && entity.isAlive() && !entity.isSpectator() && entity.isPickable())) {
            if (candidate instanceof Player victim && (!player.canHarmPlayer(victim)
                    || (player.level() instanceof ServerLevel server && !server.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.PVP))
                    || victim.isCreative())) continue;
            var hit = candidate.getBoundingBox().inflate(0.65).clip(start, end);
            if (hit.isEmpty() || !player.hasLineOfSight(candidate)) continue;
            double distance = start.distanceToSqr(hit.get());
            if (distance < nearest) {
                nearest = distance;
                chosen = candidate;
            }
        }
        return chosen;
    }

    private static void tick(MinecraftServer server) {
        long now = server.getTickCount();
        var iterator = STATES.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            Player player = server.getPlayerList().getPlayer(entry.getKey());
            GunState state = entry.getValue();
            Salvo salvo = state.salvo;
            if (player == null || !player.isAlive() || player.isSpectator()) {
                if (salvo != null) salvo.discard();
                iterator.remove();
                continue;
            }
            if (salvo != null) {
                if (!salvo.target.isAlive() || salvo.target.level() != player.level()
                        || salvo.target.distanceToSqr(player) > 64 * 64
                        || now - salvo.lastAdded > GATHER_IDLE_TIMEOUT
                        || salvo.rods.stream().anyMatch(rod -> rod.isGathering() && !rod.isAlive())) {
                    salvo.discard();
                    state.salvo = null;
                } else if (salvo.rods.size() < SALVO_SIZE) {
                    // Releasing use pauses collection. Keep existing rods so taps can accumulate a full salvo.
                    boolean holdingFocusGun = false;
                    for (InteractionHand hand : InteractionHand.values()) {
                        var held = player.getItemInHand(hand);
                        if (held.getItem() instanceof EndRodGunItem && EndRodGunItem.getMode(held) == Mode.FOCUS) {
                            holdingFocusGun = true;
                            break;
                        }
                    }
                    if (!holdingFocusGun) {
                        salvo.discard();
                        state.salvo = null;
                    }
                } else {
                    if (!salvo.releasing && salvo.rods.stream().allMatch(EndRodProjectile::isReady)) {
                        salvo.releasing = true;
                        salvo.nextAttack = now;
                    }
                    if (salvo.releasing && now >= salvo.nextAttack) {
                        salvo.rods.get(salvo.launched++).launchAtTarget();
                        salvo.nextAttack = now + ATTACK_INTERVAL;
                        if (salvo.launched == SALVO_SIZE) state.salvo = null;
                    }
                }
            }
            if (state.salvo == null && now - state.lastShot > 200) iterator.remove();
        }
    }
}
