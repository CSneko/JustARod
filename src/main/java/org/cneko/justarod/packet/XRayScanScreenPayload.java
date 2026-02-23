package org.cneko.justarod.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.cneko.justarod.client.gui.ScanType;

import static org.cneko.justarod.Justarod.MODID;

public record XRayScanScreenPayload(ScanType scanType, int targetEntityId)
        implements CustomPayload {

    public static final CustomPayload.Id<XRayScanScreenPayload> ID =
            new CustomPayload.Id<>(Identifier.of(MODID, "x_ray_scan_screen"));

    public static final PacketCodec<RegistryByteBuf, XRayScanScreenPayload> CODEC =
            PacketCodec.tuple(
                    // ScanType -> String
                    PacketCodecs.STRING.xmap(
                            ScanType::fromId,
                            ScanType::getId
                    ),
                    XRayScanScreenPayload::scanType,

                    // Entity ID
                    PacketCodecs.INTEGER,
                    XRayScanScreenPayload::targetEntityId,

                    XRayScanScreenPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}