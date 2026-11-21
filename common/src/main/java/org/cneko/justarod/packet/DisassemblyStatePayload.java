package org.cneko.justarod.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record DisassemblyStatePayload(int player, int mask, int camera, boolean random, java.util.List<net.minecraft.world.item.ItemStack> eyes) implements CustomPacketPayload {
    public DisassemblyStatePayload(int player, int mask, int camera, boolean random) { this(player,mask,camera,random,java.util.List.of()); }
    private static final StreamCodec<RegistryFriendlyByteBuf,java.util.List<net.minecraft.world.item.ItemStack>> EYES =
            net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.apply(net.minecraft.network.codec.ByteBufCodecs.list(2));
    public static final Type<DisassemblyStatePayload> ID = new Type<>(Identifier.fromNamespaceAndPath("justarod", "disassembly_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DisassemblyStatePayload> CODEC = StreamCodec.ofMember(
        (p,b) -> { b.writeVarInt(p.player); b.writeVarInt(p.mask); b.writeVarInt(p.camera); b.writeBoolean(p.random); EYES.encode(b,p.eyes); },
        b -> new DisassemblyStatePayload(b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readBoolean(), EYES.decode(b)));
    @Override public Type<? extends CustomPacketPayload> type() { return ID; }
}
