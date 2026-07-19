package org.cneko.justarod.packet;

import org.cneko.justarod.property.JRProperty;
import org.cneko.justarod.property.JRRegistry;
import static org.cneko.justarod.Justarod.MODID;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record JRSyncPayload(List<Object> values) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<JRSyncPayload> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, JRSyncPayload> CODEC = StreamCodec.ofMember(JRSyncPayload::write, JRSyncPayload::read);

    // 写入：遍历注册表，取出对应的值写入 Buf
    @SuppressWarnings("unchecked")
    private void write(RegistryFriendlyByteBuf buf) {
        List<JRProperty<?>> properties = JRRegistry.INSTANCE.getPROPERTIES();
        for (int i = 0; i < properties.size(); i++) {
            JRProperty<Object> prop = (JRProperty<Object>) properties.get(i);
            Object value = values.get(i);
            prop.writeToBuf(buf, value);
        }
    }

    // 读取：遍历注册表，从 Buf 按顺序读取值存入 List
    private static JRSyncPayload read(RegistryFriendlyByteBuf buf) {
        List<Object> decodedValues = new ArrayList<>();
        for (JRProperty<?> prop : JRRegistry.INSTANCE.getPROPERTIES()) {
            decodedValues.add(prop.readFromBuf(buf));
        }
        return new JRSyncPayload(decodedValues);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}