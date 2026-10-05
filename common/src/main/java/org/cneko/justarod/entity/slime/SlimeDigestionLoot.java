package org.cneko.justarod.entity.slime;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import java.util.HashSet;

/** Scope collection to a digestion death; nearby pre-existing loot is never swept up. */
public final class SlimeDigestionLoot {
    private record Capture(TamedSlimeEntity slime, LivingEntity prey) {}
    private static final ThreadLocal<Capture> ACTIVE = new ThreadLocal<>();
    private SlimeDigestionLoot() {}
    public static void hurt(TamedSlimeEntity slime, LivingEntity prey, Runnable damage) {
        if (!(prey.level() instanceof ServerLevel level)) return;
        var area = prey.getBoundingBox().inflate(2);
        var existing = new HashSet<Integer>();
        level.getEntitiesOfClass(ItemEntity.class, area).forEach(item -> existing.add(item.getId()));
        Capture previous = ACTIVE.get();
        ACTIVE.set(new Capture(slime, prey));
        try {
            damage.run();
        } finally {
            if (previous == null) ACTIVE.remove(); else ACTIVE.set(previous);
            if (prey.isDeadOrDying()) {
                level.getEntitiesOfClass(ItemEntity.class, area, item -> !existing.contains(item.getId()))
                        .forEach(slime::queueDigestionDrop);
            }
        }
    }
    /** Called by the XP award hook only while vanilla is producing this prey's death rewards. */
    public static boolean absorbExperience(ServerLevel level, int amount) {
        var capture = ACTIVE.get();
        if (capture == null || !capture.prey().isDeadOrDying() || capture.slime().isRemoved()
                || capture.slime().level() != level || amount <= 0) return false;
        capture.slime().absorbDigestionExperience(amount);
        return true;
    }
}
