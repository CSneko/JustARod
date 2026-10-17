package org.cneko.justarod.body;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** One server-wide, persistent authority for region and visitor ownership. */
public final class BodyRegionData extends SavedData {
    public static final int REGION_X = 512;
    public static final int REGION_Z = 256;
    public static final int CELL = 1024;
    // Morton coordinates use 15 bits per axis, staying well inside Minecraft's coordinate limit.
    public static final long CAPACITY = 1L << 30;
    public static final SavedDataType<BodyRegionData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("justarod", "body_regions"), BodyRegionData::new,
            CompoundTag.CODEC.xmap(BodyRegionData::fromTag, BodyRegionData::toTag),
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private long nextSlot;
    private final Map<UUID, Long> owners = new HashMap<>();
    private final Map<UUID, Visit> visitors = new HashMap<>();
    private final Set<Long> initialized = new HashSet<>();

    public static BodyRegionData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public long regionFor(UUID owner) {
        Long current = owners.get(owner);
        if (current != null) return current;
        return allocate(owner);
    }

    public long reset(UUID owner) {
        Long former = owners.get(owner);
        long fresh = allocate(owner);
        if (former != null) initialized.remove(former);
        return fresh;
    }

    private long allocate(UUID owner) {
        if (nextSlot >= CAPACITY) throw new IllegalStateException("Body region capacity exhausted");
        long slot = nextSlot++;
        owners.put(owner, slot);
        setDirty();
        return slot;
    }

    public Long currentRegion(UUID owner) {
        return owners.get(owner);
    }

    public Visit visit(UUID visitor) {
        return visitors.get(visitor);
    }

    public Map<UUID, Visit> visits() {
        return Map.copyOf(visitors);
    }

    public void putVisit(UUID visitor, Visit visit) {
        visitors.put(visitor, visit);
        setDirty();
    }

    public void removeVisit(UUID visitor) {
        if (visitors.remove(visitor) != null) setDirty();
    }

    public boolean isInitialized(long slot) {
        return initialized.contains(slot);
    }

    public void markInitialized(long slot) {
        if (initialized.add(slot)) setDirty();
    }

    public static int centerX(long slot) {
        return centered(deinterleave(slot, 0)) * CELL;
    }

    public static int centerZ(long slot) {
        return centered(deinterleave(slot, 1)) * CELL;
    }

    public static boolean contains(long slot, double x, double z) {
        int cx = centerX(slot);
        int cz = centerZ(slot);
        return x >= cx - REGION_X / 2.0 && x < cx + REGION_X / 2.0
                && z >= cz - REGION_Z / 2.0 && z < cz + REGION_Z / 2.0;
    }

    private static int deinterleave(long slot, int shift) {
        int n = 0;
        for (int bit = 0; bit < 15; bit++) n |= ((slot >>> (bit * 2 + shift)) & 1L) << bit;
        return n;
    }

    private static int centered(int n) {
        return (n & 1) == 0 ? n / 2 : -(n + 1) / 2;
    }

    static BodyRegionData fromTag(CompoundTag tag) {
        BodyRegionData data = new BodyRegionData();
        data.nextSlot = Math.max(0, tag.getLongOr("next_slot", 0));
        Set<Long> used = new HashSet<>();
        for (var entry : tag.getListOrEmpty("owners")) {
            if (!(entry instanceof CompoundTag ownerTag)) continue;
            try {
                UUID uuid = UUID.fromString(ownerTag.getStringOr("uuid", ""));
                long slot = ownerTag.getLongOr("slot", -1);
                if (slot < 0 || slot >= CAPACITY || data.owners.containsKey(uuid)) continue;
                if (!used.add(slot)) continue;
                data.owners.put(uuid, slot);
                data.nextSlot = Math.max(data.nextSlot, slot + 1);
            } catch (IllegalArgumentException ignored) { }
        }
        for (var entry : tag.getListOrEmpty("visitors")) {
            if (!(entry instanceof CompoundTag visitTag)) continue;
            try {
                UUID uuid = UUID.fromString(visitTag.getStringOr("uuid", ""));
                UUID host = UUID.fromString(visitTag.getStringOr("host", ""));
                long slot = visitTag.getLongOr("slot", -1);
                if (slot < 0 || slot >= CAPACITY) continue;
                data.visitors.put(uuid, new Visit(host, slot,
                        visitTag.getStringOr("origin", "minecraft:overworld"),
                        visitTag.getDoubleOr("x", 0), visitTag.getDoubleOr("y", 64),
                        visitTag.getDoubleOr("z", 0), visitTag.getFloatOr("yaw", 0),
                        visitTag.getFloatOr("pitch", 0)));
            } catch (IllegalArgumentException ignored) { }
        }
        tag.getLongArray("initialized").ifPresent(slots -> {
            for (long slot : slots) if (slot >= 0 && slot < CAPACITY) data.initialized.add(slot);
        });
        return data;
    }

    CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("next_slot", nextSlot);
        ListTag ownerList = new ListTag();
        owners.forEach((uuid, slot) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("uuid", uuid.toString());
            entry.putLong("slot", slot);
            ownerList.add(entry);
        });
        tag.put("owners", ownerList);
        ListTag visitorList = new ListTag();
        visitors.forEach((uuid, visit) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("uuid", uuid.toString());
            entry.putString("host", visit.host().toString());
            entry.putLong("slot", visit.slot());
            entry.putString("origin", visit.origin());
            entry.putDouble("x", visit.x());
            entry.putDouble("y", visit.y());
            entry.putDouble("z", visit.z());
            entry.putFloat("yaw", visit.yaw());
            entry.putFloat("pitch", visit.pitch());
            visitorList.add(entry);
        });
        tag.put("visitors", visitorList);
        tag.putLongArray("initialized", initialized.stream().mapToLong(Long::longValue).toArray());
        return tag;
    }

    public record Visit(UUID host, long slot, String origin,
                        double x, double y, double z, float yaw, float pitch) { }
}
