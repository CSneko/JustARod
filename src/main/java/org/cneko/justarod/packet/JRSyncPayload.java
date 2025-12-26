package org.cneko.justarod.packet;

import net.minecraft.entity.EntityType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.Optional;

import static org.cneko.justarod.Justarod.MODID;

public record JRSyncPayload(
        double power,
        int pregnant,
        Optional<EntityType<?>> childrenType,
        int menstruation,
        int menstruationComfort,
        int babyCount,
        int excretion,
        int urination,
        int syphilis,
        // 下面所有字段在网络传输层都将合并为一个 int
        boolean male,
        boolean female,
        boolean sterilization,
        boolean ectopicPregnancy,
        boolean aids,
        boolean immune2Aids,
        boolean hydatidiformMole,
        boolean hpv,
        boolean immune2HPV,
        boolean hasUterus,
        boolean isPCOS,
        boolean brithControlling,
        boolean ovarianCancer,
        boolean breastCancer,
        boolean amputated,
        boolean orchiectomy
) implements CustomPayload {

    public static final CustomPayload.Id<JRSyncPayload> ID = new CustomPayload.Id<>(Identifier.of(MODID, "sync"));

    private static final PacketCodec<RegistryByteBuf, Optional<EntityType<?>>> ENTITY_TYPE_CODEC =
            PacketCodecs.optional(PacketCodecs.registryValue(RegistryKeys.ENTITY_TYPE));

    public static final PacketCodec<RegistryByteBuf, JRSyncPayload> CODEC = PacketCodec.of(JRSyncPayload::write, JRSyncPayload::read);

    // 写入数据：将所有 boolean 压缩进一个 int
    private void write(RegistryByteBuf buf) {
        // 1. 先写入普通的数值类型
        buf.writeDouble(this.power);
        buf.writeVarInt(this.pregnant);
        ENTITY_TYPE_CODEC.encode(buf, this.childrenType);
        buf.writeVarInt(this.menstruation);
        buf.writeVarInt(this.menstruationComfort);
        buf.writeVarInt(this.babyCount);
        buf.writeVarInt(this.excretion);
        buf.writeVarInt(this.urination);
        buf.writeVarInt(this.syphilis);

        // 2. 位压缩逻辑
        int flags = 0;
        if (this.male)             flags |= (1);
        if (this.female)           flags |= (1 << 1);
        if (this.sterilization)    flags |= (1 << 2);
        if (this.ectopicPregnancy) flags |= (1 << 3);
        if (this.aids)             flags |= (1 << 4);
        if (this.immune2Aids)      flags |= (1 << 5);
        if (this.hydatidiformMole) flags |= (1 << 6);
        if (this.hpv)              flags |= (1 << 7);
        if (this.immune2HPV)       flags |= (1 << 8);
        if (this.hasUterus)        flags |= (1 << 9);
        if (this.isPCOS)           flags |= (1 << 10);
        if (this.brithControlling) flags |= (1 << 11);
        if (this.ovarianCancer)    flags |= (1 << 12);
        if (this.breastCancer)     flags |= (1 << 13);
        if (this.amputated)        flags |= (1 << 14);
        if (this.orchiectomy)      flags |= (1 << 15);

        // 3. 写入这个携带了所有开关信息的整数
        buf.writeVarInt(flags);
    }

    // 读取数据：从 int 中解析出 boolean
    private static JRSyncPayload read(RegistryByteBuf buf) {
        // 1. 读取数值
        double power = buf.readDouble();
        int pregnant = buf.readVarInt();
        Optional<EntityType<?>> childrenType = ENTITY_TYPE_CODEC.decode(buf);
        int menstruation = buf.readVarInt();
        int menstruationComfort = buf.readVarInt();
        int babyCount = buf.readVarInt();
        int excretion = buf.readVarInt();
        int urination = buf.readVarInt();
        int syphilis = buf.readVarInt();

        // 2. 读取 Flags 整数
        int flags = buf.readVarInt();

        // 3. 解压缩逻辑 (检查特定位是否为1)
        return new JRSyncPayload(
                power, pregnant, childrenType, menstruation, menstruationComfort, babyCount, excretion, urination,syphilis,
                (flags & (1)) != 0, // male
                (flags & (1 << 1)) != 0, // female
                (flags & (1 << 2)) != 0, // sterilization
                (flags & (1 << 3)) != 0, // ectopicPregnancy
                (flags & (1 << 4)) != 0, // aids
                (flags & (1 << 5)) != 0, // immune2Aids
                (flags & (1 << 6)) != 0, // hydatidiformMole
                (flags & (1 << 7)) != 0, // hpv
                (flags & (1 << 8)) != 0, // immune2HPV
                (flags & (1 << 9)) != 0, // hasUterus
                (flags & (1 << 10)) != 0, // isPCOS
                (flags & (1 << 11)) != 0, // brithControlling
                (flags & (1 << 12)) != 0, // ovarianCancer
                (flags & (1 << 13)) != 0, // breastCancer
                (flags & (1 << 14)) != 0, // amputated
                (flags & (1 << 15)) != 0  // orchiectomy
        );
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}