package org.cneko.justarod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 26.x：实体存档改为 ValueInput/ValueOutput。
 * 为减少迁移量，把 JustARod 自有的 NBT 数据作为单个 {@link CompoundTag} 编解码，
 * 使用独立命名空间避免与 toNeko 的 TonekoData 冲突。
 */
public final class JRNbtBridge {
    public static final String DATA_KEY = "JustARodData";

    private JRNbtBridge() {
    }

    public static void store(CompoundTag data, ValueOutput out) {
        if (data == null || data.isEmpty()) {
            return;
        }
        out.store(DATA_KEY, CompoundTag.CODEC, data);
    }

    public static CompoundTag read(ValueInput in) {
        return in.read(DATA_KEY, CompoundTag.CODEC).orElseGet(CompoundTag::new);
    }
}
