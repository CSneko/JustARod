package org.cneko.justarod.client.slime;

import net.minecraft.client.Minecraft;
import org.cneko.justarod.entity.slime.TamedSlimeEntity;
import org.cneko.justarod.packet.SlimeCaptivityStatePayload;

/** Server snapshots are extrapolated only while the same player remains inside the same slime. */
public final class SlimeCaptivityClientState {
    private static SlimeCaptivityStatePayload state;
    private static long receivedAt;
    private SlimeCaptivityClientState() {}
    public static void accept(SlimeCaptivityStatePayload payload) {
        state = payload;
        var level = Minecraft.getInstance().level;
        receivedAt = level == null ? 0 : level.getGameTime();
    }
    public static boolean active() {
        var mc = Minecraft.getInstance();
        return state != null && mc.player != null && mc.level != null && mc.player.isAlive()
                && mc.player.getVehicle() instanceof TamedSlimeEntity slime && slime.getId() == state.slimeEntityId()
                && slime.isCaptive(mc.player);
    }
    private static int elapsed() { return active() ? (int) Math.clamp(Minecraft.getInstance().level.getGameTime() - receivedAt, 0, 40) : 0; }
    public static int graceTicks() { return !active() || state.graceTicks() < 0 ? -1 : Math.max(0, state.graceTicks() - elapsed()); }
    public static float blur() {
        if (!active() || state.graceTicks() < 0) return 0;
        int digestion = state.digestionTicks() + Math.max(0, elapsed() - state.graceTicks());
        return Math.clamp(digestion / (float) Math.max(20, state.blurRampTicks()), 0f, 1f);
    }
    public static float struggle() { return active() ? state.struggle() : 0; }
}
