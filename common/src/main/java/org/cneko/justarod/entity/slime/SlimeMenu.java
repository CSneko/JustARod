package org.cneko.justarod.entity.slime;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * 史莱姆体内容器菜单。
 *
 * <p>版式（与作者确认）：箱子界面 + 「物品 / 实体」页签。
 * <ul>
 *   <li>物品页：9x6 = 54 格一页，翻页只改 {@link SlimeStorageContainer} 的窗口偏移，
 *       **槽位对象永不重建**（因此不需要碰 {@code AbstractContainerMenu.slots} 这种私有结构）</li>
 *   <li>实体页：数据由 {@link org.cneko.justarod.packet.SlimeEntityListPayload} 提供，客户端自绘</li>
 * </ul>
 */
public class SlimeMenu extends AbstractContainerMenu {

    public static final int ROWS = 6;
    public static final int COLS = 9;
    public static final int PAGE_SIZE = ROWS * COLS;

    public static final int TAB_ITEMS = 0;
    public static final int TAB_ENTITIES = 1;

    private final TamedSlimeEntity slime;
    private final SlimeStorageContainer container;

    private final SyncSlot syncTier;
    private final SyncSlot syncPage;
    private final SyncSlot syncTab;
    private final SyncSlot syncDecompose;
    private final SyncSlot syncCommand;

    private int page = 0;
    private int tab = TAB_ITEMS;
    /** 客户端：从数据槽同步过来的当前指令（跟随/待命/吞人），按钮文字要用它 */
    private int command = 0;
    /** 建槽位时容器物品格的数量，用来发现「升级导致两端的槽位数不再一致」 */
    private int builtCapacity = -1;

    /**
     * 客户端构造菜单时用不到的上下文（MenuType 工厂拿不到实体）。
     * 服务端在 openMenu 之前用 {@code SlimeMenuContextPayload} 先把它填好，
     * 保证两端槽位数一致。
     */
    private static int pendingTierLevel = -1;
    private static TamedSlimeEntity pendingSlime;

    /** 收到上下文包时调用（客户端） */
    public static void acceptContext(int tierLevel) {
        pendingTierLevel = tierLevel;
    }

    public static void acceptContext(int tierLevel, TamedSlimeEntity slime) {
        pendingTierLevel = tierLevel;
        pendingSlime = slime;
    }

    /** 取出并清空待用实体（界面工厂用一次） */
    public static TamedSlimeEntity consumePendingSlime() {
        TamedSlimeEntity slime = pendingSlime;
        pendingSlime = null;
        return slime;
    }

    /** 菜单构造完成后清掉，避免影响下一个菜单 */
    private static int consumePendingTierLevel() {
        int level = pendingTierLevel;
        pendingTierLevel = -1;
        return level;
    }

    /** 客户端构造（MenuType 工厂）：没有实体，容量由同步过来的等级算出 */
    public SlimeMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public SlimeMenu(int containerId, Inventory playerInventory, TamedSlimeEntity slime) {
        super(JRMenus.SLIME_MENU, containerId);
        this.slime = slime;
        // 客户端也要建容器槽位：槽位数必须和服务端逐一对齐，否则容器内容包会越界崩溃
        this.container = new SlimeStorageContainer(slime);

        // 等级槽：服务端读实体，「客户端」收到数据槽后写进镜像的等级里
        // （sendAllDataToRemote 会把数据槽一起推过来，等于给客户端的容量上了第二道保险；
        //   之前客户端的 setter 是空实现，收到值也没人记，等级只能靠上下文包那一份）
        this.syncTier = new SyncSlot(
                () -> this.slime != null ? this.slime.getTierLevel()
                        : (this.container.hasClientTierLevel() ? this.container.clientTierLevel() : 0),
                value -> this.container.setClientTierLevel(value));
        this.syncPage = new SyncSlot(() -> this.page, value -> this.page = value);
        this.syncTab = new SyncSlot(() -> this.tab, value -> this.tab = value);
        this.syncDecompose = new SyncSlot(() -> this.slime == null ? 0 : this.slime.getDecomposeState(), value -> { });
        // 指令槽：服务端读实体，客户端把收到的值记下来（否则界面上的模式按钮永远显示旧文字）
        this.syncCommand = new SyncSlot(
                () -> this.slime != null ? this.slime.getCommand() : this.command,
                value -> this.command = value);
        this.addDataSlot(syncTier);
        this.addDataSlot(syncPage);
        this.addDataSlot(syncTab);
        this.addDataSlot(syncDecompose);
        this.addDataSlot(syncCommand);

        // 客户端：槽位数必须与服务端一致，等级来自上下文包
        if (slime == null) {
            int level = consumePendingTierLevel();
            // 没收到上下文（level < 0）时保持「未知」，不要再拿别处的 0 去覆盖它
            if (level >= 0) {
                this.container.setClientTierLevel(level);
            }
        }
        this.addSlots(playerInventory);
        this.applyPageOffset();
        this.builtCapacity = container.capacity();
    }



    private void addSlots(Inventory playerInventory) {
        final SlimeStorageContainer view = this.container;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int viewIndex = row * COLS + col;
                this.addSlot(new Slot(container, viewIndex, 8 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return !stack.isEmpty() && isActive();
                    }

                    @Override
                    public boolean mayPickup(Player player) {
                        return isActive();
                    }

                    @Override
                    public boolean isActive() {
                        // 两种「不可用」：
                        //  * 超出容量的尾格（升级后自动变成可用）；
                        //  * 实体页：整个物品区被实体列表面板盖住，槽位必须一起隐身——
                        //    否则原版会把槽位网格/物品画在面板上面（截图里那团糊字就是这么来的）。
                        return view.isReal(viewIndex) && tab != TAB_ENTITIES;
                    }

                    // 不要再覆写 getMaxStackSize()！一格 = 一组（原版会取
                    // container.getMaxStackSize() 与物品自身上限的较小值）。
                    // 曾经这里返回 1，实际效果退化成「一格一个物品」，
                    // 与 SlimeStorage / 界面文案的「1 组 = 1 格」互相矛盾。
                });
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 198));
        }
    }

    private void applyPageOffset() {
        container.setPageOffset(page * PAGE_SIZE);
        // 注意：只有服务端拿得到实体等级。客户端必须沿用「上下文包」里设好的等级，
        // 不能在这里用 syncTier.get() 覆盖它——同步槽在客户端构造时还没收到值，会把等级冲成 0，
        // 导致容量恒为 4（翻页失效、超出容量的格子判断也全错）。
        if (slime != null) {
            container.setClientTierLevel(slime.getTierLevel());
        }
    }

    public TamedSlimeEntity slime() {
        return slime;
    }

    /**
     * 客户端界面工厂用：把实体挂上，顺便把容量对齐。
     *
     * <p>注意顺序：客户端实体的 {@code tierLevel} 只有在收到 {@code DATA_TIER} 同步后才是对的
     * （见 {@link TamedSlimeEntity#onSyncedDataUpdated}），而「上下文包」是服务端在 {@code openMenu}
     * 之前发的、和菜单槽位数同一份快照，更可信。所以这里**只在上下文包没给等级时兜底**，
     * 绝不覆盖——否则容量会被冲成 T0 的 4 格，表现为「只能看到/点到前 4 格」（§9.6 的翻版）。
     */
    public void setClientSlime(TamedSlimeEntity clientSlime) {
        if (this.slime == null && clientSlime != null && !this.container.hasClientTierLevel()) {
            this.container.setClientTierLevel(clientSlime.getTierLevel());
        }
    }

    public int getPage() {
        return page;
    }

    public int getMaxPage() {
        return Math.max(0, (container.capacity() - 1) / PAGE_SIZE);
    }

    /** 翻页：只移动窗口偏移，槽位对象保持不动 */
    public void applyPage(int newPage) {
        int clamped = Math.max(0, Math.min(getMaxPage(), newPage));
        if (clamped == this.page) return;
        this.page = clamped;
        this.applyPageOffset();
        // 让客户端重新同步一遍当前页内容
        this.broadcastChanges();
    }

    public int getTab() {
        return tab;
    }

    public void setTab(int tab) {
        this.tab = tab;
    }

    public int getTierLevel() {
        return slime != null ? slime.getTierLevel() : syncTier.get();
    }

    public int getDecomposeState() {
        return slime != null ? slime.getDecomposeState() : syncDecompose.get();
    }

    /** 当前指令（跟随/待命/吞人）：服务端读实体，客户端读数据槽同步过来的值 */
    public int getCommand() {
        return slime != null ? slime.getCommand() : command;
    }

    @Override
    public boolean stillValid(Player player) {
        // 客户端没有实体可校验（slime == null），界面是否该关由服务端说了算：
        // 服务端失效时会主动发关界面包；这里返回 false 只会让客户端自己把界面判定为非法。
        if (slime == null) return true;
        // 只有主人能用这个背包：非主人（哪怕被别的途径塞进这个菜单）会被服务端自动关掉
        return slime.isAlive() && slime.isOwner(player) && slime.distanceTo(player) < 64.0F;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= this.slots.size()) return ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int containerSlots = PAGE_SIZE;
        if (index < containerSlots) {
            if (!this.moveItemStackTo(stack, containerSlots, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!this.moveItemStackTo(stack, 0, containerSlots, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    /**
     * 菜单打开期间史莱姆升级会改变容量（= 槽位数），而 {@code slots} 是 final 的、无法增删。
     * 一旦两端数量不再一致，继续同步就会越界掉线，所以这里检测到变化就让调用方关掉界面，
     * 玩家重新右键即可打开一个尺寸正确的界面。
     */
    public boolean slotCountOutOfSync() {
        return slime != null && builtCapacity >= 0 && container.capacity() != builtCapacity;
    }

    // ==================== 同步（无方块实体的容器菜单必须自己做） ====================

    /**
     * 原版容器菜单是在构造时 {@code addSlotListener(this)} + {@code setSynchronizer(...)} 的
     * （方块实体那条路），本模组的菜单挂在实体上，没有方块实体，所以必须自己补这两步。
     *
     * <p>漏掉的后果：服务端 {@code broadcastChanges()} 一直在空转，**客户端永远收不到任何
     * 容器内容**——GUI 里能看到容量/槽位布局（那些走自定义包与 DataSlot），却看不到物品。
     */
    @Override
    public void addSlotListener(net.minecraft.world.inventory.ContainerListener listener) {
        super.addSlotListener(listener);
        if (listener instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            // 直接借用玩家自带的容器同步器，避免自己重写协议细节
            this.setSynchronizer(
                    ((org.cneko.justarod.mixin.debug.ServerPlayerSyncAccessor) serverPlayer)
                            .justarod$getContainerSynchronizer());
        }
    }

    /**
     * 打开菜单后立即把初始内容推给客户端。
     * 顺序上必须在 {@code setSynchronizer} 之后调用，否则什么都不会发出去。
     */
    public void sendInitialSync() {
        if (slime == null) return;   // 只有服务端有实体
        // 26.x 的正确做法：sendAllDataToRemote() 会把全部槽位 + 数据槽推给客户端。
        // 不能用 broadcastChanges()——它是「差分」发送，服务端的 remoteSlots 初始状态
        // 与真实槽位比较后可能判定「无变化」，于是一个包都不发（这正是物品永远不显示的根因）。
        this.sendAllDataToRemote();
    }

    /** 单向同步槽：服务端 → 客户端 */
    private static class SyncSlot extends DataSlot {
        private final IntSupplier getter;
        private final IntConsumer setter;

        SyncSlot(IntSupplier getter, IntConsumer setter) {
            this.getter = getter;
            this.setter = setter;
        }

        @Override
        public int get() {
            return getter.getAsInt();
        }

        @Override
        public void set(int value) {
            setter.accept(value);
        }
    }
}
