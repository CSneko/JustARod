package org.cneko.justarod.disassembly;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import java.util.EnumMap;
import java.util.UUID;

/** A detached slot has one identity, preserved across item/entity transfers and saves. */
public final class DisassemblyState {
    public final EnumMap<BodyPart, UUID> detached = new EnumMap<>(BodyPart.class);
    public boolean random = true;
    public int camera = -1;
    public BodyPart viewedPart;
    public boolean dual;
    public org.cneko.justarod.packet.DisassemblyViewPayload lastView;
    public final java.util.List<net.minecraft.world.item.ItemStack> installedEyes = new java.util.ArrayList<>();
    public interface Holder { DisassemblyState justarod$disassembly(); }
    public static DisassemblyState of(Player player) { return ((Holder) player).justarod$disassembly(); }
    public boolean missing(BodyPart part) { return detached.containsKey(part); }
    public int mask() { int mask = 0; for (BodyPart p : detached.keySet()) mask |= p.bit(); return mask; }
    public boolean crawling() { return missing(BodyPart.LEFT_LEG) && missing(BodyPart.RIGHT_LEG); }
    public void save(ValueOutput output) {
        var data = output.child("JustARodDisassembly");
        data.putBoolean("Random", random);
        data.store("InstalledEyes", net.minecraft.world.item.ItemStack.CODEC.listOf(), installedEyes);
        detached.forEach((part, id) -> data.putString(part.key(), id.toString()));
    }
    public void load(ValueInput input) {
        detached.clear(); camera = -1; viewedPart = null; lastView = null; dual = false; installedEyes.clear();
        var data = input.childOrEmpty("JustARodDisassembly");
        random = data.getBooleanOr("Random", true);
        installedEyes.addAll(data.read("InstalledEyes", net.minecraft.world.item.ItemStack.CODEC.listOf()).orElse(java.util.List.of()));
        for (BodyPart part : BodyPart.values()) data.getString(part.key()).ifPresent(value -> {
            try { detached.put(part, UUID.fromString(value)); } catch (IllegalArgumentException ignored) { }
        });
    }
}
