package org.cneko.justarod.pet;

import net.minecraft.world.entity.ai.goal.Goal;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import java.util.EnumSet;

/** Optional companion movement; never replaces the catgirl's normal AI permanently. */
final class PetNekoFollowGoal extends Goal {
    private final NekoEntity npc;
    PetNekoFollowGoal(NekoEntity npc) {
        this.npc = npc;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }
    @Override public boolean canUse() { return PetController.companion(npc) != null; }
    @Override public void tick() {
        var p = PetController.companion(npc);
        if (p == null) return;
        npc.getLookControl().setLookAt(p, 30, 30);
        if (npc.distanceToSqr(p) > 9) npc.getNavigation().moveTo(p, 1);
        else npc.getNavigation().stop();
    }
    @Override public void stop() { npc.getNavigation().stop(); }
}
