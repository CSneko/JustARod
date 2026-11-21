package org.cneko.justarod.pet;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import java.util.UUID;

/** Only the relationship is persistent. Orders and pending consent never survive a reconnect. */
public final class PetState {
    public enum Mode { FREE, SIT, FOLLOW }
    public UUID owner;
    public String ownerName = "";
    public Mode mode = Mode.FREE;
    public int pausedUntil, nextOffer, nextInteraction, nextFeed, nextRecall, nextInstinct;
    public int nextOrder, nextHint, nextMenu, nextOwnerAction;
    public int ownerActionIntervalTicks = PetOwnerActivity.DEFAULT_ACTION_INTERVAL_TICKS;
    public boolean ownerActivityPaused;
    public double sitX, sitZ;

    public interface Holder { PetState justarod$petState(); }
    public static PetState of(Player player) { return ((Holder) player).justarod$petState(); }

    public void save(ValueOutput output) {
        if (owner == null) return;
        var child = output.child("JustARodPet");
        child.putString("Owner", owner.toString());
        child.putString("Name", ownerName);
    }

    public void load(ValueInput input) {
        owner = null;
        ownerName = "";
        mode = Mode.FREE;
        ownerActionIntervalTicks = PetOwnerActivity.DEFAULT_ACTION_INTERVAL_TICKS;
        var child = input.childOrEmpty("JustARodPet");
        try {
            owner = UUID.fromString(child.getStringOr("Owner", ""));
            ownerName = child.getStringOr("Name", "");
        } catch (IllegalArgumentException ignored) { }
    }
}
