package org.cneko.justarod.entity.slime;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.cneko.justarod.config.JRConfig;
import org.cneko.justarod.packet.SlimeCaptivityStatePayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

/** Player digestion is independent of the fast creature/catalyst queue. Ages count loaded server ticks. */
public final class SlimeCaptivityController {
    private final TamedSlimeEntity slime;
    private final Map<UUID, Integer> ages = new HashMap<>();
    private final Map<UUID, Float> digestionVitals = new HashMap<>();
    private final Map<UUID, net.minecraft.world.phys.Vec3> offsets = new HashMap<>();

    public SlimeCaptivityController(TamedSlimeEntity slime) { this.slime = slime; }
    public void onEnter(Player player) {
        if (slime.isUncontrolled()) ages.putIfAbsent(player.getUUID(), 0);
        send(player);
    }
    public void forget(UUID id) { ages.remove(id); digestionVitals.remove(id); offsets.remove(id); }
    public float digestionProgress(Player player) {
        if (!isDigesting(player)) return 0;
        float vitality = digestionVitals.getOrDefault(player.getUUID(), player.getHealth() + player.getAbsorptionAmount());
        int duration = Math.max(1, (int) Math.ceil(vitality / JRConfig.getSlimePlayerDamage())) * JRConfig.getSlimePlayerDamageInterval();
        return Math.clamp((age(player.getUUID()) - JRConfig.getSlimePlayerGraceTicks()) / (float) duration, 0, 1);
    }
    public net.minecraft.world.phys.Vec3 offset(Entity player) {
        return offsets.getOrDefault(player.getUUID(), slime.defaultFloatingOffset(player));
    }
    private void moveInside(Player player) {
        player.setPose(net.minecraft.world.entity.Pose.STANDING);
        var point = offset(player);
        if (player instanceof ServerPlayer serverPlayer) {
            var input = serverPlayer.getLastClientInput();
            double forward = (input.forward() ? 1 : 0) - (input.backward() ? 1 : 0);
            double left = (input.left() ? 1 : 0) - (input.right() ? 1 : 0);
            var direction = net.minecraft.world.phys.Vec3.directionFromRotation(0, player.getYRot()).scale(forward)
                    .add(net.minecraft.world.phys.Vec3.directionFromRotation(0, player.getYRot() - 90).scale(left));
            if (direction.lengthSqr() > 0) direction = direction.normalize().scale(0.065);
            double vertical = ((input.jump() ? 1 : 0) - (input.shift() ? 1 : 0)) * 0.05;
            point = point.add(direction.x, vertical, direction.z);
        }
        offsets.put(player.getUUID(), slime.clampFloatingOffset(player, point));
        slime.positionRider(player);
    }
    public int age(UUID id) { return ages.getOrDefault(id, 0); }
    public boolean isDigesting(Player player) {
        return slime.isUncontrolled() && slime.isCaptive(player) && ages.containsKey(player.getUUID())
                && age(player.getUUID()) >= JRConfig.getSlimePlayerGraceTicks();
    }

    public void tick() {
        if (!(slime.level() instanceof ServerLevel level)) return;
        var present = new HashSet<UUID>();
        for (Entity entity : new ArrayList<>(slime.getPassengers())) {
            if (!(entity instanceof Player player) || !slime.isCaptive(player) || !player.isAlive()) continue;
            present.add(player.getUUID());
            if (slime.isUncontrolled()) {
                int age = Math.min(20_000_000, age(player.getUUID()) + 1);
                ages.put(player.getUUID(), age);
                moveInside(player);
                int elapsed = age - JRConfig.getSlimePlayerGraceTicks();
                if (elapsed >= 0) digestionVitals.putIfAbsent(player.getUUID(), player.getHealth() + player.getAbsorptionAmount());
                if (elapsed == 0) player.sendOverlayMessage(Component.translatable("justarod.slime.captivity.digesting"));
                if (elapsed > 0 && elapsed % JRConfig.getSlimePlayerDamageInterval() == 0) {
                    SlimeDigestionLoot.hurt(slime, player, () -> player.hurtServer(level, level.damageSources().magic(), JRConfig.getSlimePlayerDamage()));
                    if (!player.isAlive()) {
                        slime.releaseEntity(player.getUUID());
                        continue;
                    }
                }
            } else forget(player.getUUID());
            if (slime.tickCount % 20 == 0) send(player);
        }
        ages.keySet().retainAll(present);
        digestionVitals.keySet().retainAll(present);
        offsets.keySet().retainAll(present);
        if (slime.tickCount % 2 == 0 && (!offsets.isEmpty() || !slime.getPassengers().isEmpty())) syncFloating(level);
    }

    private void syncFloating(ServerLevel level) {
        var entries = new ArrayList<org.cneko.justarod.packet.SlimeFloatingPositionsPayload.Entry>();
        for (Entity passenger : slime.getPassengers()) if (slime.isFloatingCaptive(passenger)) {
            entries.add(new org.cneko.justarod.packet.SlimeFloatingPositionsPayload.Entry(passenger.getId(), offset(passenger)));
        }
        var payload = new org.cneko.justarod.packet.SlimeFloatingPositionsPayload(slime.getId(), entries);
        for (var player : level.players()) if (player.distanceToSqr(slime) < 64 * 64
                && ServerPlayNetworking.canSend(player, payload.type())) ServerPlayNetworking.send(player, payload);
    }

    private void send(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || !ServerPlayNetworking.canSend(serverPlayer, SlimeCaptivityStatePayload.ID)) return;
        int elapsed = slime.isUncontrolled() ? age(player.getUUID()) - JRConfig.getSlimePlayerGraceTicks() : -1;
        int grace = slime.isUncontrolled() ? Math.max(0, -elapsed) : -1;
        int index = slime.storage().containedEntities().indexOf(player.getUUID());
        float struggle = index < 0 ? 0 : slime.storage().struggleProgress().get(index) / slime.struggleThreshold();
        ServerPlayNetworking.send(serverPlayer, new SlimeCaptivityStatePayload(slime.getId(), grace,
                Math.max(0, elapsed), JRConfig.getSlimePlayerBlurRampTicks(), Math.clamp(struggle, 0f, 1f)));
    }

    public void save(ValueOutput out) {
        var ids = out.list("Players", UUIDUtil.CODEC);
        var ticks = out.list("Ages", com.mojang.serialization.Codec.INT);
        var vitals = out.list("DigestionVitals", com.mojang.serialization.Codec.FLOAT);
        var positions = out.list("Offsets", net.minecraft.world.phys.Vec3.CODEC);
        ages.forEach((id, age) -> {
            ids.add(id); ticks.add(age);
            vitals.add(digestionVitals.getOrDefault(id, -1f));
            positions.add(offsets.getOrDefault(id, net.minecraft.world.phys.Vec3.ZERO));
        });
    }
    public void load(ValueInput in) {
        ages.clear();
        digestionVitals.clear();
        offsets.clear();
        var ids = new ArrayList<UUID>();
        var ticks = new ArrayList<Integer>();
        in.listOrEmpty("Players", UUIDUtil.CODEC).forEach(ids::add);
        in.listOrEmpty("Ages", com.mojang.serialization.Codec.INT).forEach(ticks::add);
        var vitals = new ArrayList<Float>();
        var positions = new ArrayList<net.minecraft.world.phys.Vec3>();
        in.listOrEmpty("DigestionVitals", com.mojang.serialization.Codec.FLOAT).forEach(vitals::add);
        in.listOrEmpty("Offsets", net.minecraft.world.phys.Vec3.CODEC).forEach(positions::add);
        for (int i = 0; i < ids.size(); i++) {
            ages.put(ids.get(i), i < ticks.size() ? Math.clamp(ticks.get(i), 0, 20_000_000) : 0);
            if (i < vitals.size() && Float.isFinite(vitals.get(i)) && vitals.get(i) > 0) digestionVitals.put(ids.get(i), vitals.get(i));
            if (i < positions.size()) offsets.put(ids.get(i), positions.get(i));
        }
    }
}
