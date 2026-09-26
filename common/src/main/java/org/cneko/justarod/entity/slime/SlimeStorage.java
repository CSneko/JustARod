package org.cneko.justarod.entity.slime;

import lombok.Getter;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 驯服史莱姆的「体内空间」数据模型：物品格 + 实体占用共享同一个空间池。
 *
 * <p>规则（与设计确认一致）：
 * <ul>
 *   <li>1 组物品 = 1 格空间</li>
 *   <li>1 个实体 = 其碰撞箱长×宽×高（半格向上取整）格空间</li>
 *   <li>总空间 = {@link SlimeTier#internalSpace()}</li>
 * </ul>
 *
 * <p>本类只负责「空间账本」与序列化；实体的实际挂载（乘客）、挣扎、渲染
 * 由 {@link TamedSlimeEntity} 负责。
 */
public class SlimeStorage {

    /** 体内物品（一组一格）；已分配但未使用的格子为 {@link ItemStack#EMPTY} */
    private final List<ItemStack> items = new ArrayList<>();

    /**
     * 内容版本号：任何一次物品变动都会 +1。
     * 服务端靠它判断「要不要把体内内容推给客户端渲染」（见 TamedSlimeEntity#syncContentsIfChanged）。
     */
    private int version = 0;

    /** 内容版本号（只增不减） */
    public int version() {
        return version;
    }

    /** 标记内容已改动 */
    public void bumpVersion() {
        version++;
    }

    /** 已被吞入的实体（UUID，实体本体作为乘客挂载在史莱姆身上） */
    private final List<UUID> containedEntities = new ArrayList<>();

    /** 每个被吞实体的挣扎进度（与 containedEntities 同序） */
    private final List<Float> struggleProgress = new ArrayList<>();

    /** 每个被吞实体占用的空间格数（与 containedEntities 同序，用于释放时归还空间） */
    private final List<Integer> entityVolumes = new ArrayList<>();

    /**
     * 每个被吞实体是否「免疫挣扎」（与 containedEntities 同序）。
     *
     * <p>目前只有**主人**会免疫：主人钻进来是为了操控史莱姆，如果照常累积挣扎进度，
     * 他会在约 36 秒后被自动漂移甩出去（玩家 0.35/tick、T3 阈值 250）——
     * 直接违背「主人进出」这条设计（§1）。所以主人照样登记（占格、实体页里看得见），
     * 但不参与挣扎；被**技能**吞进来的别的玩家不免疫，照常能挣扎逃出。
     */
    private final List<Boolean> struggleImmune = new ArrayList<>();

    /** 内部实体被分解时挂起的原因（血量不足），非空时 UI 会提示 */
    @Getter
    private String suspendedReason = null;

    // ==================== 空间账本 ====================

    /** 已占用的空间（格） */
    public int usedSpace() {
        int used = 0;
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) used += 1;
        }
        for (int v : entityVolumes) {
            used += v;
        }
        return used;
    }

    /**
     * 固定分配「当前等级容量」个物品格（多出来的为空槽）。
     * 这样容器界面的槽位数在等级不变时保持稳定，客户端与服务端一致。
     */
    public void ensureSlots(int totalSlots) {
        while (items.size() < totalSlots) {
            items.add(ItemStack.EMPTY);
        }
        while (items.size() > totalSlots) {
            items.remove(items.size() - 1);
        }
        this.bumpVersion();
    }

    /** 剩余空间（格） */
    public int freeSpace(int totalSpace) {
        return Math.max(0, totalSpace - usedSpace());
    }

    public List<ItemStack> items() {
        return items;
    }

    public List<UUID> containedEntities() {
        return containedEntities;
    }

    public List<Float> struggleProgress() {
        return struggleProgress;
    }

    public List<Integer> entityVolumes() {
        return entityVolumes;
    }

    /** 每个被吞实体是否免疫挣扎（与 {@link #containedEntities()} 同序） */
    public List<Boolean> struggleImmune() {
        return struggleImmune;
    }

    public void setSuspendedReason(String reason) {
        this.suspendedReason = reason;
    }

    /**
     * 尝试投入一组物品：**整组塞进第一个空格**（1 组 = 1 格）。
     *
     * <p>注意不要退化成「一格一个物品」——那会让 {@link SlimeMenu#PAGE_SIZE} 格只能装 54 个物品，
     * 与 {@code SlimeStorageContainer#getMaxStackSize()}（64）和界面里一格一组的预期都不符。
     *
     * @return 实际投入的物品数量（0 表示空间不足）
     */
    public int addItem(ItemStack stack, int totalSpace) {
        if (stack.isEmpty()) return 0;
        if (freeSpace(totalSpace) <= 0) return 0;
        int empty = firstEmptySlot();
        if (empty < 0) return 0;
        items.set(empty, stack.copy());
        this.bumpVersion();
        return stack.getCount();
    }

    /** 第一个空格下标；没有空格返回 -1 */
    public int firstEmptySlot() {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).isEmpty()) return i;
        }
        return -1;
    }

    /** 第一个非空格下标；空仓返回 -1（分解系统按格取内容用） */
    public int firstNonEmptySlot() {
        for (int i = 0; i < items.size(); i++) {
            if (!items.get(i).isEmpty()) return i;
        }
        return -1;
    }

    /**
     * 从第 index 格里取走**一个**物品（分解时一个个消耗用）；该格见底就置空。
     *
     * @return false 表示这一格本来就是空的
     */
    public boolean consumeOneItem(int index) {
        if (index < 0 || index >= items.size()) return false;
        ItemStack stack = items.get(index);
        if (stack.isEmpty()) return false;
        if (stack.getCount() <= 1) {
            items.set(index, ItemStack.EMPTY);
        } else {
            stack.shrink(1);
        }
        this.bumpVersion();
        return true;
    }

    /** 取出第 index 组物品（返回副本，并把该格置空） */
    public ItemStack removeItem(int index) {
        if (index < 0 || index >= items.size()) return ItemStack.EMPTY;
        ItemStack stack = items.get(index);
        items.set(index, ItemStack.EMPTY);
        this.bumpVersion();
        return stack;
    }

    /** 直接取走全部物品（用于死亡掉落） */
    public List<ItemStack> drainItems() {
        List<ItemStack> copy = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) copy.add(stack);
            items.set(i, ItemStack.EMPTY);
            this.bumpVersion();
        }
        return copy;
    }

    /** 计算实体能否被吞入 */
    public boolean canSwallow(int volume, int totalSpace) {
        return freeSpace(totalSpace) >= volume;
    }

    /** 登记一个被吞实体 */
    public void addEntity(UUID uuid, int volume) {
        addEntity(uuid, volume, false);
    }

    /**
     * 登记一个被吞实体。
     *
     * @param immune 是否免疫挣扎（主人为 true：他是来操控的，不该被自动甩出去）
     */
    public void addEntity(UUID uuid, int volume, boolean immune) {
        containedEntities.add(uuid);
        struggleProgress.add(0.0F);
        entityVolumes.add(volume);
        struggleImmune.add(immune);
    }

    /**
     * 已经登记过就更新它的免疫标记，没登记过就登记；返回是否发生了改动。
     *
     * <p>「主人状态对账」用：挂载关系可能被任意路径改变（钻进来、被技能吞、被别的模组拉下车），
     * 与其在每个入口都记得同步一次账本，不如每 tick 把实际挂载关系与账本对一次。
     */
    public boolean syncContained(UUID uuid, int volume, boolean immune) {
        int index = containedEntities.indexOf(uuid);
        if (index < 0) {
            addEntity(uuid, volume, immune);
            return true;
        }
        if (struggleImmune.get(index) != immune) {
            struggleImmune.set(index, immune);
            return true;
        }
        return false;
    }

    /** 这个实体是否免疫挣扎 */
    public boolean isStruggleImmune(UUID uuid) {
        int index = containedEntities.indexOf(uuid);
        return index >= 0 && struggleImmune.get(index);
    }

    /** 移除一个被吞实体的登记，返回它占用的空间 */
    public int removeEntity(UUID uuid) {
        int index = containedEntities.indexOf(uuid);
        if (index < 0) return 0;
        containedEntities.remove(index);
        struggleProgress.remove(index);
        struggleImmune.remove(index);
        return entityVolumes.remove(index);
    }

    public boolean containsEntity(UUID uuid) {
        return containedEntities.contains(uuid);
    }

    public void clearEntities() {
        containedEntities.clear();
        struggleProgress.clear();
        entityVolumes.clear();
        struggleImmune.clear();
    }

    /** 取出「第一个」非空物品（分解用），该格随之置空 */
    public ItemStack pollFirstItem() {
        for (int i = 0; i < items.size(); i++) {
            if (!items.get(i).isEmpty()) {
                ItemStack stack = items.get(i);
                items.set(i, ItemStack.EMPTY);
                this.bumpVersion();
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /** 是否还有可分解的内容 */
    public boolean hasContent() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) return true;
        }
        return !containedEntities.isEmpty();
    }

    // ==================== 序列化（26.x ValueOutput/ValueInput） ====================

    private static final String KEY_ITEMS = "Items";
    private static final String KEY_ENTITIES = "ContainedEntities";
    private static final String KEY_STRUGGLE = "Struggle";
    private static final String KEY_VOLUMES = "EntityVolumes";
    private static final String KEY_IMMUNE = "StruggleImmune";

    public void save(ValueOutput out) {
        // 注意 1：空槽也要写入（空栈占位），保证下标与槽位一一对应。
        // 注意 2：必须用 OPTIONAL_CODEC —— 严格的 ItemStack.CODEC 会校验
        // 「数量 ∈ [1;99] 且不能是 minecraft:air」，写空槽时直接抛
        // "Failed to append value '0 minecraft:air' to list 'Items'"（存档报错的真凶）。
        // OPTIONAL_CODEC 把空栈写成 {}，读回来仍然是空栈，下标不丢。
        var list = out.list(KEY_ITEMS, ItemStack.OPTIONAL_CODEC);
        for (ItemStack stack : items) {
            list.add(stack);
        }
        var entities = out.list(KEY_ENTITIES, UUIDUtil.CODEC);
        var struggles = out.list(KEY_STRUGGLE, com.mojang.serialization.Codec.FLOAT);
        var volumes = out.list(KEY_VOLUMES, com.mojang.serialization.Codec.INT);
        var immune = out.list(KEY_IMMUNE, com.mojang.serialization.Codec.BOOL);
        for (int i = 0; i < containedEntities.size(); i++) {
            entities.add(containedEntities.get(i));
            struggles.add(struggleProgress.get(i));
            volumes.add(entityVolumes.get(i));
            immune.add(i < struggleImmune.size() && struggleImmune.get(i));
        }
    }

    public void load(ValueInput in) {
        items.clear();
        containedEntities.clear();
        struggleProgress.clear();
        entityVolumes.clear();
        struggleImmune.clear();

        in.listOrEmpty(KEY_ITEMS, ItemStack.OPTIONAL_CODEC).forEach(stack -> items.add(stack));
        List<UUID> ids = new ArrayList<>();
        in.listOrEmpty(KEY_ENTITIES, UUIDUtil.CODEC).forEach(ids::add);
        List<Float> struggles = new ArrayList<>();
        in.listOrEmpty(KEY_STRUGGLE, com.mojang.serialization.Codec.FLOAT).forEach(struggles::add);
        List<Integer> volumes = new ArrayList<>();
        in.listOrEmpty(KEY_VOLUMES, com.mojang.serialization.Codec.INT).forEach(volumes::add);
        List<Boolean> immune = new ArrayList<>();
        in.listOrEmpty(KEY_IMMUNE, com.mojang.serialization.Codec.BOOL).forEach(immune::add);

        for (int i = 0; i < ids.size(); i++) {
            containedEntities.add(ids.get(i));
            struggleProgress.add(i < struggles.size() ? struggles.get(i) : 0.0F);
            entityVolumes.add(i < volumes.size() ? volumes.get(i) : 1);
            // 老存档没有这一列：默认 false（不免疫）——主人那份会由每 tick 的对账补上
            struggleImmune.add(i < immune.size() && immune.get(i));
        }
    }
}
