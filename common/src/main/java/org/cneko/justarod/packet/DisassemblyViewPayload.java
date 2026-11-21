package org.cneko.justarod.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/** Only the owner of the observed body part receives the pocket's contents. */
public record DisassemblyViewPayload(int mode, int target, String carrier, boolean dual, List<ItemStack> contents) implements CustomPacketPayload {
    public static final int WORLD = 0, LEFT_HAND = 1, RIGHT_HAND = 2, INVENTORY = 3, MOUNTED = 4;
    public static final DisassemblyViewPayload CLOSED = new DisassemblyViewPayload(-1,-1,"",false,List.of());
    public static final Type<DisassemblyViewPayload> ID = new Type<>(Identifier.fromNamespaceAndPath("justarod","disassembly_view"));
    private static final StreamCodec<RegistryFriendlyByteBuf,List<ItemStack>> ITEMS = ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(64));
    public static final StreamCodec<RegistryFriendlyByteBuf,DisassemblyViewPayload> CODEC = StreamCodec.ofMember(
        (p,b) -> { b.writeVarInt(p.mode); b.writeVarInt(p.target); b.writeUtf(p.carrier,64); b.writeBoolean(p.dual); ITEMS.encode(b,p.contents); },
        b -> new DisassemblyViewPayload(b.readVarInt(),b.readVarInt(),b.readUtf(64),b.readBoolean(),ITEMS.decode(b)));
    public boolean sameView(DisassemblyViewPayload other) {
        if (other == null || mode != other.mode || target != other.target || dual != other.dual || !carrier.equals(other.carrier) || contents.size() != other.contents.size()) return false;
        for (int i=0;i<contents.size();i++) if (!ItemStack.matches(contents.get(i),other.contents.get(i))) return false;
        return true;
    }
    @Override public Type<? extends CustomPacketPayload> type() { return ID; }
}
