package org.cneko.justarod.packet;

import org.cneko.justarod.client.gui.ScanType;

import static org.cneko.justarod.Justarod.MODID;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record XRayScanScreenPayload(ScanType scanType, int targetEntityId)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<XRayScanScreenPayload> ID =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "x_ray_scan_screen"));

    public static final StreamCodec<RegistryFriendlyByteBuf, XRayScanScreenPayload> CODEC =
            StreamCodec.composite(
                    // ScanType -> String
                    ByteBufCodecs.STRING_UTF8.map(
                            ScanType::fromId,
                            ScanType::getId
                    ),
                    XRayScanScreenPayload::scanType,

                    // Entity ID
                    ByteBufCodecs.INT,
                    XRayScanScreenPayload::targetEntityId,

                    XRayScanScreenPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}