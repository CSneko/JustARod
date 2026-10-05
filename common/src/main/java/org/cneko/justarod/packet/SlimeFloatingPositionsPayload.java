package org.cneko.justarod.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.cneko.justarod.Justarod;
import java.util.ArrayList;
import java.util.List;

/** Server-authoritative foot positions relative to the slime, for captive players only. */
public record SlimeFloatingPositionsPayload(int slimeId, List<Entry> entries) implements CustomPacketPayload {
    public record Entry(int playerId, Vec3 offset) {}
    public static final Type<SlimeFloatingPositionsPayload> ID = new Type<>(Identifier.fromNamespaceAndPath(Justarod.MODID, "slime_floating_positions"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SlimeFloatingPositionsPayload> CODEC = StreamCodec.ofMember(
            SlimeFloatingPositionsPayload::write, SlimeFloatingPositionsPayload::read);
    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(slimeId);
        buf.writeVarInt(entries.size());
        for (Entry entry : entries) {
            buf.writeVarInt(entry.playerId());
            buf.writeFloat((float) entry.offset().x);
            buf.writeFloat((float) entry.offset().y);
            buf.writeFloat((float) entry.offset().z);
        }
    }
    private static SlimeFloatingPositionsPayload read(RegistryFriendlyByteBuf buf) {
        int slimeId = buf.readVarInt();
        int count = buf.readVarInt();
        if (count < 0 || count > 256) throw new IllegalArgumentException("Invalid captive position count");
        var entries = new ArrayList<Entry>(count);
        for (int i = 0; i < count; i++) entries.add(new Entry(buf.readVarInt(), new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat())));
        return new SlimeFloatingPositionsPayload(slimeId, List.copyOf(entries));
    }
    @Override public Type<? extends CustomPacketPayload> type() { return ID; }
}
