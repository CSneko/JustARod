package org.cneko.justarod.entity.slime;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 把 {@link SlimeStorage} 的「物品格」包装成原版 {@link Container}，供容器菜单使用。
 *
 * <p>关键设计一：**槽位对象永不重建**。
 * {@code AbstractContainerMenu.slots} 是 final 的，26.x 也没有公开的增删槽位 API，
 * 所以这里用一个「分页视图」：菜单固定建 {@link SlimeMenu#PAGE_SIZE} 个槽位，
 * 每个槽位转发到 {@link #pageOffset} + 槽位下标的真实物品格。
 * 翻页只改 {@code pageOffset}，槽位下标不变，客户端与服务端的槽位 id 永远一致。
 *
 * <p>关键设计二：**两端各自有一份真实存储**。
 * <ul>
 *   <li>服务端（{@code slime != null}）：直接读写实体上的 {@link SlimeStorage#items()}，
 *       它是唯一真相，菜单关闭后物品仍然留在史莱姆体内；</li>
 *   <li>客户端（{@code slime == null}）：把菜单同步包写进来的物品存在本地镜像
 *       {@link #clientItems} 里。客户端**没有实体**，所以镜像就是这一侧的唯一存储。</li>
 * </ul>
 *
 * <p>曾经的致命错误：客户端这一侧的所有读写都被 {@code slime == null} 挡掉了
 * （{@code getItem} 恒返回 {@code EMPTY}、{@code setItem} 直接 return）。
 * 而 {@code AbstractContainerMenu.initializeContents} 写入内容的唯一途径正是
 * {@code getSlot(i).set(stack) → Slot.set → Container.setItem}，
 * 于是同步包把物品「写进了空气」：
 * 服务端体内有物品、客户端 GUI 一片空白，但点击仍能被服务端正确处理
 * （点击只发 {@code ServerboundContainerClickPacket}，物品在服务端搬运）。
 */
public class SlimeStorageContainer implements Container {

    private final TamedSlimeEntity slime;
    /** 当前页起始下标（由菜单在翻页时设置） */
    private int pageOffset = 0;

    /**
     * 客户端本地镜像（{@code slime == null} 时使用）。
     * 只增不减：等级同步晚到（或容量被算小）时，不会把已经收到的物品抹掉。
     */
    private final List<ItemStack> clientItems = new ArrayList<>();

    /** 客户端用：由上下文包带来的等级；{@code < 0} 表示还没收到 */
    private int clientTierLevel = -1;

    public SlimeStorageContainer(TamedSlimeEntity slime) {
        this.slime = slime;
    }

    public TamedSlimeEntity slime() {
        return slime;
    }

    public void setPageOffset(int offset) {
        this.pageOffset = Math.max(0, offset);
    }

    public int getPageOffset() {
        return pageOffset;
    }

    /**
     * 真实物品格总数（= 当前等级的容量）。
     *
     * <p>关键：这是**纯计算**得到的，不依赖实体本身。
     * 客户端构造菜单时 {@code slime == null}（MenuType 工厂只拿到 Inventory），
     * 只能靠同步过来的等级算出同样的容量，两端槽位数才会一致。
     */
    public int capacity() {
        if (slime != null) {
            return slime.tier().capacity();
        }
        return SlimeTier.of(clientTierLevel).capacity();
    }

    /** 客户端用：由上下文包带来的等级（负数忽略，保持「还没收到」的状态） */
    public void setClientTierLevel(int level) {
        if (level >= 0) {
            this.clientTierLevel = level;
        }
    }

    /** 客户端是否已经拿到等级；没拿到时 {@link #capacity()} 会退化成 T0 的 4 格 */
    public boolean hasClientTierLevel() {
        return clientTierLevel >= 0;
    }

    /** 客户端记住的等级（{@code -1} = 还没收到） */
    public int clientTierLevel() {
        return clientTierLevel;
    }

    /**
     * 当前后端存储：服务端 = 实体存储（唯一真相），客户端 = 本地镜像。
     * 客户端镜像会按需增长到「本页能访问到的最大下标」，读/写前调用即可。
     */
    private List<ItemStack> backing() {
        if (slime != null) {
            return slime.storage().items();
        }
        int needed = Math.max(SlimeMenu.PAGE_SIZE, capacity());
        while (clientItems.size() < needed) {
            clientItems.add(ItemStack.EMPTY);
        }
        return clientItems;
    }

    /** 视图大小：一页固定 54 格，最后一页不足的部分返回 EMPTY */
    @Override
    public int getContainerSize() {
        return SlimeMenu.PAGE_SIZE;
    }

    /** 该视图槽位是否对应真实存在的物品格（超出容量的尾格为 false） */
    public boolean isReal(int slot) {
        return realIndex(slot) >= 0;
    }

    private int realIndex(int slot) {
        int index = pageOffset + slot;
        return (slot < 0 || slot >= SlimeMenu.PAGE_SIZE || index >= capacity()) ? -1 : index;
    }

    @Override
    public boolean isEmpty() {
        List<ItemStack> items = backing();
        for (int i = 0; i < SlimeMenu.PAGE_SIZE; i++) {
            int index = realIndex(i);
            if (index >= 0 && index < items.size() && !items.get(index).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        int index = realIndex(slot);
        if (index < 0) return ItemStack.EMPTY;
        List<ItemStack> items = backing();
        return index < items.size() ? items.get(index) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        int index = realIndex(slot);
        if (index < 0) return ItemStack.EMPTY;
        List<ItemStack> items = backing();
        if (index >= items.size()) return ItemStack.EMPTY;
        ItemStack current = items.get(index);
        if (current.isEmpty()) return ItemStack.EMPTY;
        ItemStack taken = current.copyWithCount(Math.min(amount, current.getCount()));
        if (taken.getCount() >= current.getCount()) {
            items.set(index, ItemStack.EMPTY);
        } else {
            current.shrink(taken.getCount());
        }
        this.setChanged();
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        int index = realIndex(slot);
        if (index < 0) return ItemStack.EMPTY;
        List<ItemStack> items = backing();
        if (index >= items.size()) return ItemStack.EMPTY;
        ItemStack stack = items.get(index);
        items.set(index, ItemStack.EMPTY);
        this.setChanged();
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        int index = realIndex(slot);
        if (index < 0) return;
        List<ItemStack> items = backing();
        if (index >= items.size()) return;
        items.set(index, stack == null ? ItemStack.EMPTY : stack);
        this.setChanged();
    }

    @Override
    public int getMaxStackSize() {
        // 每格 = 1 组（满堆叠），所以按物品自身的堆叠上限来
        return 64;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return Math.max(1, stack.getMaxStackSize());
    }

    @Override
    public void setChanged() {
        // 服务端数据直接存在实体上，无需额外脏标记；
        // 但要**通知内容版本变化**：GUI 里放进/拿走物品后，附近的客户端才能更新体内渲染。
        // （客户端镜像是本地数据，不需要。）
        if (slime != null) {
            slime.storage().bumpVersion();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        // 客户端镜像没有实体可校验，校验交给服务端（它会自己发关界面包）
        if (slime == null) return true;
        return slime.isAlive() && slime.distanceTo(player) < 64.0F;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return !stack.isEmpty() && realIndex(slot) >= 0;
    }

    @Override
    public void clearContent() {
        List<ItemStack> items = backing();
        int from = Math.min(pageOffset, capacity());
        int to = Math.min(pageOffset + SlimeMenu.PAGE_SIZE, capacity());
        for (int i = from; i < to && i < items.size(); i++) {
            items.set(i, ItemStack.EMPTY);
        }
        this.setChanged();
    }
}
