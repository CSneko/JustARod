package org.cneko.justarod.client.slime;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.cneko.justarod.entity.slime.TamedSlimeEntity;
import org.cneko.justarod.packet.SlimeFloatingPositionsPayload;
import java.util.HashMap;
import java.util.Map;

/** Interpolate position snapshots; never use captive input to predict or drive the slime itself. */
public final class SlimeFloatingClientState {
    private record Sample(Vec3 from, Vec3 to, long time) {}
    private static final Map<Integer, Map<Integer, Sample>> CACHE = new HashMap<>();
    private SlimeFloatingClientState() {}
    public static void accept(SlimeFloatingPositionsPayload payload) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var old = CACHE.getOrDefault(payload.slimeId(), Map.of());
        var next = new HashMap<Integer, Sample>();
        for (var entry : payload.entries()) {
            var previous = old.get(entry.playerId());
            Vec3 from = previous == null ? entry.offset() : interpolate(previous, level.getGameTime(), 0);
            next.put(entry.playerId(), new Sample(from, entry.offset(), level.getGameTime()));
        }
        if (next.isEmpty()) CACHE.remove(payload.slimeId());
        else CACHE.put(payload.slimeId(), next);
    }
    public static Vec3 offset(TamedSlimeEntity slime, Entity player, float partialTick) {
        var sample = CACHE.getOrDefault(slime.getId(), Map.of()).get(player.getId());
        return sample == null ? slime.defaultFloatingOffset(player) : interpolate(sample, slime.level().getGameTime(), partialTick);
    }
    private static Vec3 interpolate(Sample sample, long time, float partialTick) {
        return sample.from().lerp(sample.to(), Math.clamp((time - sample.time() + partialTick) / 2.0, 0, 1));
    }
    public static void forget(int slimeId) { CACHE.remove(slimeId); }
    public static void reset() { CACHE.clear(); }
}
