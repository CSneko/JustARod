package org.cneko.justarod.client.renderer;

/** Shared frame-clock rhythm anchored to world time; entity age never affects the phase. */
public final class EntitySquashAnimation {
    private EntitySquashAnimation() {
    }

    public static float heightScale(double gameTicks, float bpm, float compressionRatio) {
        // 20 ticks/second, 120/BPM seconds/half note. Beats 1/3 squash, beats 2/4 restore.
        return heightScaleForPhase(gameTicks * bpm / 2400.0, compressionRatio);
    }

    public static float heightScaleForPhase(double halfNotes, float compressionRatio) {
        double amount = (1.0 + Math.cos(Math.PI * (halfNotes % 2.0))) * 0.5;
        return (float) (1.0 - (1.0 - compressionRatio) * amount);
    }

    public static float rotationDegrees(double gameTicks, float bpm) {
        // One full turn per 4/2 bar, using the same half-note phase as the squash.
        return rotationDegreesForPhase(gameTicks * bpm / 2400.0);
    }

    public static float rotationDegreesForPhase(double halfNotes) {
        return (float) ((halfNotes % 4.0) * 90.0);
    }

    /** One clock per dispatcher, sampled once per frame for every entity. */
    public static final class Clock {
        private boolean initialized;
        private boolean wasPaused;
        private long lastNanos;
        private double halfNotes;

        public void reset() {
            initialized = false;
        }

        public double sample(long gameTime, float partialTick, long nowNanos, float bpm, boolean paused) {
            if (!initialized) {
                // Cast BEFORE adding: long + float otherwise discards sub-tick precision in old worlds.
                double ticks = (double) gameTime + partialTick;
                halfNotes = (ticks * bpm / 2400.0) % 4.0;
                initialized = true;
            } else if (!paused && !wasPaused) {
                double seconds = Math.max(0L, nowNanos - lastNanos) / 1_000_000_000.0;
                // Integrate tempo instead of multiplying absolute world time by the current BPM.
                halfNotes = (halfNotes + seconds * bpm / 120.0) % 4.0;
            }
            wasPaused = paused;
            lastNanos = nowNanos;
            return halfNotes;
        }
    }
}
