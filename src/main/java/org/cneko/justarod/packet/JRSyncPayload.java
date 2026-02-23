package org.cneko.justarod.packet;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.cneko.justarod.property.JRProperty;
import org.cneko.justarod.property.JRRegistry;
import static org.cneko.justarod.Justarod.MODID;

import java.util.ArrayList;
import java.util.List;

public record JRSyncPayload(List<Object> values) implements CustomPayload {

    public static final CustomPayload.Id<JRSyncPayload> ID = new CustomPayload.Id<>(Identifier.of(MODID, "sync"));
    public static final PacketCodec<RegistryByteBuf, JRSyncPayload> CODEC = PacketCodec.of(JRSyncPayload::write, JRSyncPayload::read);

    // 写入：遍历注册表，取出对应的值写入 Buf
    @SuppressWarnings("unchecked")
    private void write(RegistryByteBuf buf) {
        List<JRProperty<?>> properties = JRRegistry.INSTANCE.getPROPERTIES();
        for (int i = 0; i < properties.size(); i++) {
            JRProperty<Object> prop = (JRProperty<Object>) properties.get(i);
            Object value = values.get(i);
            prop.writeToBuf(buf, value);
        }
    }

    // 读取：遍历注册表，从 Buf 按顺序读取值存入 List
    private static JRSyncPayload read(RegistryByteBuf buf) {
        List<Object> decodedValues = new ArrayList<>();
        for (JRProperty<?> prop : JRRegistry.INSTANCE.getPROPERTIES()) {
            decodedValues.add(prop.readFromBuf(buf));
        }
        return new JRSyncPayload(decodedValues);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}