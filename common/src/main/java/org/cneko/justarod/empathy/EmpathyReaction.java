package org.cneko.justarod.empathy;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Shared identifiers and timing for the server and the renderers. */
public enum EmpathyReaction {
    STROKE(60), TICKLE(40), COLD(80), PRICK(12), SQUEEZE(16), END_ROD(60);

    public final int duration;

    EmpathyReaction(int duration) { this.duration = duration; }

    public static EmpathyReaction forItem(ItemStack stack) {
        if (stack.isEmpty()) return STROKE;
        if (stack.is(Items.FEATHER)) return TICKLE;
        if (stack.is(Items.ICE) || stack.is(Items.PACKED_ICE) || stack.is(Items.BLUE_ICE)) return COLD;
        if (stack.is(Items.CACTUS)) return PRICK;
        if (stack.is(Items.END_ROD)) return END_ROD;
        return null;
    }

    /** A quick compression and a damped rebound, always ending at the original size. */
    public static float squeezeHeight(float age) {
        if (age < 0 || age >= SQUEEZE.duration) return 1;
        if (age < 3) return 1 - 0.65f * (float) Math.sin(age / 3 * Math.PI / 2);
        float t = (age - 3) / 13;
        return 1 - 0.65f * (float) (Math.cos(t * Math.PI * 2.5) * Math.pow(1 - t, 2));
    }
}
