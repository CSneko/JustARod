package org.cneko.justarod.client.screen;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.TypedEntityData;
import org.cneko.justarod.entity.slime.SlimeMenu;
import org.cneko.justarod.entity.slime.TamedSlimeEntity;
import org.cneko.justarod.packet.SlimeContainerActionPayload;
import org.cneko.justarod.packet.SlimeEntityListPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * 史莱姆体内容器界面。
 *
 * <p>版式（与作者确认）：箱子式界面 + 「物品 / 实体」两个页签。
 * <ul>
 *   <li>物品页：9x6 格，底部翻页按钮</li>
 *   <li>实体页：列表显示体内实体（图标 + 名字 + 占格 + 挣扎进度），可点击「取出」</li>
 * </ul>
 * 实体列表数据由 {@link SlimeEntityListPayload} 推送，缓存在本界面里。
 */
public class SlimeScreen extends AbstractContainerScreen<SlimeMenu> {

    private static final Identifier CHEST_TEXTURE =
            Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final Identifier TAB_SELECTED =
            Identifier.withDefaultNamespace("textures/gui/sprites/container/beacon/button_selected.png");
    private static final Identifier TAB_NORMAL =
            Identifier.withDefaultNamespace("textures/gui/sprites/container/beacon/button.png");

    // ===== 实体页版式（全部相对 leftPos/topPos，别再各处硬编码，免得文字和槽位打架）=====
    /** 内容面板：正好盖住物品页那 6 行槽位 */
    private static final int PANEL_X = 7;
    private static final int PANEL_Y = 17;
    private static final int PANEL_W = 162;
    private static final int PANEL_H = 106;
    /** 面板里的表头高度（容量那行） */
    private static final int PANEL_HEADER = 14;
    /** 每行实体高度 */
    private static final int ROW_H = 18;
    /** 底部提示行高度 */
    private static final int PANEL_FOOTER = 16;
    /** 一屏最多显示几行（放不下时底部提示「还有 N 个未显示」） */
    private static final int MAX_ROWS = (PANEL_H - PANEL_HEADER - PANEL_FOOTER) / ROW_H;
    /** 行内各列（相对面板左上角），加起来必须留在 PANEL_W=162 以内 */
    private static final int COL_ICON = 4;
    private static final int COL_TEXT = 24;
    private static final int COL_BAR = 96;
    private static final int BAR_W = 28;
    private static final int COL_RELEASE = 152;

    /** 服务端推送过来的体内实体列表 */
    private static SlimeEntityListPayload lastEntityList;

    private final List<Button> tabButtons = new ArrayList<>();
    private int tickCounter = 0;

    private Button prevButton;
    private Button nextButton;
    /** 底栏中部的模式按钮：文字要跟着同步过来的指令走，否则点了看不出变化 */
    private Button commandButton;

    /** MenuScreens 工厂用：从待打开上下文里取实体 */
    public static SlimeScreen create(SlimeMenu menu, Inventory inventory, Component title) {
        TamedSlimeEntity slime = SlimeMenu.consumePendingSlime();
        SlimeScreen screen = new SlimeScreen(menu, inventory, title);
        screen.slime = slime;
        // 首帧就按实体等级把容量对齐（客户端算容量的依据）
        if (slime != null) {
            menu.setClientSlime(slime);
        }
        return screen;
    }

    /** 客户端侧的实体（可能为 null） */
    private TamedSlimeEntity slime;

    public SlimeScreen(SlimeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        // imageWidth/imageHeight 在 26.x 是 final，尺寸沿用原版容器界面（176x166）
        this.titleLabelY = 6;
        this.inventoryLabelY = 128;
    }

    /** 收到服务端推送的实体列表 */
    public static void acceptEntityList(SlimeEntityListPayload payload) {
        lastEntityList = payload;
    }

    public static SlimeEntityListPayload entityList() {
        return lastEntityList;
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos;
        int y = this.topPos;

        // 页签按钮
        this.tabButtons.clear();
        Button itemsTab = Button.builder(Component.translatable("justarod.slime.gui.items"), b -> switchTab(SlimeMenu.TAB_ITEMS))
                .bounds(x + 6, y - 20, 60, 18).build();
        Button entitiesTab = Button.builder(Component.translatable("justarod.slime.gui.entities"), b -> switchTab(SlimeMenu.TAB_ENTITIES))
                .bounds(x + 70, y - 20, 70, 18).build();
        this.tabButtons.add(this.addRenderableWidget(itemsTab));
        this.tabButtons.add(this.addRenderableWidget(entitiesTab));

        // 翻页按钮
        this.prevButton = this.addRenderableWidget(Button.builder(Component.nullToEmpty("◀"), b -> requestPage(menu.getPage() - 1))
                .bounds(x + 8, y + 126, 20, 16).build());
        this.nextButton = this.addRenderableWidget(Button.builder(Component.nullToEmpty("▶"), b -> requestPage(menu.getPage() + 1))
                .bounds(x + 148, y + 126, 20, 16).build());

        // 模式切换：跟随 / 待命 / 吞人（文字由同步槽驱动，见 updateButtonStates）
        this.commandButton = this.addRenderableWidget(Button.builder(commandLabel(), b -> cycleCommand())
                .bounds(x + 30, y + 126, 118, 16).build());

        updateButtonStates();
    }

    private void switchTab(int tab) {
        ClientPlayNetworking.send(SlimeContainerActionPayload.of(menu.containerId,
                SlimeContainerActionPayload.ACTION_SET_TAB, tab, 0));
    }

    /** 循环切换史莱姆指令：跟随 → 待命 → 吞人 */
    private void cycleCommand() {
        ClientPlayNetworking.send(SlimeContainerActionPayload.of(menu.containerId,
                SlimeContainerActionPayload.ACTION_SET_COMMAND, Integer.MIN_VALUE, 0));
    }

    private void requestPage(int page) {
        ClientPlayNetworking.send(SlimeContainerActionPayload.of(menu.containerId,
                SlimeContainerActionPayload.ACTION_SET_PAGE, Math.max(0, page), 0));
    }

    /** 模式按钮的文字：模式：跟随 / 待命 / 吞噬 */
    private Component commandLabel() {
        String key = switch (menu.getCommand()) {
            case 1 -> "justarod.slime.command.stay";
            case 2 -> "justarod.slime.command.swallow";
            default -> "justarod.slime.command.follow";
        };
        return Component.translatable("justarod.slime.command.button", Component.translatable(key));
    }

    private void updateButtonStates() {
        if (prevButton != null) prevButton.active = menu.getPage() > 0;
        if (nextButton != null) nextButton.active = menu.getPage() < menu.getMaxPage();
        for (int i = 0; i < tabButtons.size(); i++) {
            tabButtons.get(i).active = menu.getTab() != i;
        }
        // 指令是服务端权威的：点完等数据槽回来再刷新文字（updateButtonStates 每 tick 调一次）
        if (commandButton != null) commandButton.setMessage(commandLabel());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        // 每 5 tick 刷新一次实体列表（挣扎进度是实时变化的）
        if (++tickCounter % 5 == 0) {
            ClientPlayNetworking.send(SlimeContainerActionPayload.of(menu.containerId,
                    SlimeContainerActionPayload.ACTION_REFRESH, 0, 0));
        }
        updateButtonStates();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        if (menu.getTab() == SlimeMenu.TAB_ENTITIES) {
            renderEntityList(graphics, mouseX, mouseY);
        } else {
            renderItemInfo(graphics);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // 刻意不调 super：原版会画「物品栏」标签，那个位置被底栏的翻页/模式按钮占着，
        // 叠在一起就是一团糊（截图里那行糊字就是它）。容量改到实体页面板表头里显示。
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF404040);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // 上半 6 行槽位 + 下半玩家背包（沿用原版 generic_54 贴图）
        graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST_TEXTURE,
                this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, 125, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST_TEXTURE,
                this.leftPos, this.topPos + 125, 0.0F, 125.0F, this.imageWidth, 97, 256, 256);

        if (menu.getTab() == SlimeMenu.TAB_ENTITIES) {
            // 实体页：整块不透明面板盖掉物品槽位网格（半透明的话网格会透出来，很乱）
            int x0 = this.leftPos + PANEL_X;
            int y0 = this.topPos + PANEL_Y;
            graphics.fill(x0, y0, x0 + PANEL_W, y0 + PANEL_H, 0xFF1B1B1B);
            graphics.fill(x0, y0, x0 + PANEL_W, y0 + 1, 0xFF3C3C3C);            // 上边框
            graphics.fill(x0, y0 + PANEL_H - 1, x0 + PANEL_W, y0 + PANEL_H, 0xFF3C3C3C);
        }
    }

    private void renderItemInfo(GuiGraphicsExtractor graphics) {
        // 页数提示（夹在 ◀ / ▶ 两个按钮中间）
        graphics.centeredText(this.font,
                Component.nullToEmpty((menu.getPage() + 1) + " / " + (menu.getMaxPage() + 1)),
                this.leftPos + this.imageWidth / 2, this.topPos + 130, 0xFF404040);
    }

    private void renderEntityList(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        SlimeEntityListPayload list = entityList();
        int x0 = this.leftPos + PANEL_X;
        int y0 = this.topPos + PANEL_Y;

        // ===== 表头：容量 =====
        int used = list == null ? 0 : list.usedSpace();
        int total = list == null ? 0 : list.totalSpace();
        graphics.text(this.font,
                Component.translatable("justarod.slime.gui.capacity").append("  " + used + " / " + total),
                x0 + 5, y0 + 3, 0xFFD0D0D0);

        if (list == null || list.entries().isEmpty()) {
            graphics.centeredText(this.font, Component.translatable("justarod.slime.gui.empty"),
                    x0 + PANEL_W / 2, y0 + PANEL_HEADER + 22, 0xFF9A9A9A);
            return;
        }

        int threshold = Math.max(1, list.struggleThreshold());
        int rows = Math.min(MAX_ROWS, list.entries().size());
        for (int row = 0; row < rows; row++) {
            SlimeEntityListPayload.Entry entry = list.entries().get(row);
            int rowY = y0 + PANEL_HEADER + row * ROW_H;
            boolean hovered = mouseY >= rowY && mouseY < rowY + ROW_H - 1
                    && mouseX >= x0 && mouseX < x0 + PANEL_W;

            // 行底：隔行深浅 + 悬停高亮，肉眼容易跟行
            int rowBg = hovered ? 0x40FFFFFF : ((row % 2 == 0) ? 0x14FFFFFF : 0x08FFFFFF);
            graphics.fill(x0 + 2, rowY, x0 + PANEL_W - 2, rowY + ROW_H - 2, rowBg);

            // 图标（刷怪蛋 / 玩家头颅）
            graphics.item(iconFor(entry), x0 + COL_ICON, rowY + 1);

            // 名字（太长就按像素截断，别压到进度条上）
            String name = this.font.plainSubstrByWidth(entry.name().getString(), COL_BAR - COL_TEXT - 4);
            graphics.text(this.font, Component.nullToEmpty(name), x0 + COL_TEXT, rowY, 0xFFF0F0F0);

            // 占格
            graphics.text(this.font, Component.translatable("justarod.slime.gui.volume", entry.volume()),
                    x0 + COL_TEXT, rowY + 9, 0xFF9A9A9A);

            // 挣扎进度条 + 百分比；免疫挣扎的（主人）显示标记而不是一条永远不动的空条
            if (entry.struggleImmune()) {
                graphics.text(this.font, Component.translatable("justarod.slime.gui.owner_inside"),
                        x0 + COL_BAR, rowY + 11, 0xFF7CC4FF);
            } else {
                int struggle = Math.min(100, entry.struggle() * 100 / threshold);
                int filled = (int) (BAR_W * Math.min(1.0F, entry.struggle() / (float) threshold));
                int barY = rowY + 11;
                graphics.fill(x0 + COL_BAR, barY, x0 + COL_BAR + BAR_W, barY + 5, 0xFF303030);
                graphics.fill(x0 + COL_BAR, barY, x0 + COL_BAR + filled, barY + 5, 0xFF54C24A);
                graphics.text(this.font, Component.nullToEmpty(struggle + "%"),
                        x0 + COL_BAR + BAR_W + 3, rowY + 9, 0xFF9A9A9A);
            }

            // 取出按钮：hover 时整块变亮，点击区域见 mouseClicked。
            // 免疫挣扎**不等于**不能被分解——主人照样可以被分解，所以这个按钮对他也要画，
            // 否则玩家会以为他动不了。
            boolean hoverX = isOverRelease(mouseX, mouseY, x0, rowY);
            graphics.fill(x0 + COL_RELEASE - 1, rowY + 2, x0 + PANEL_W - 4, rowY + ROW_H - 4,
                    hoverX ? 0x60FF5555 : 0x30FFFFFF);
            graphics.centeredText(this.font, Component.nullToEmpty("×"),
                    x0 + COL_RELEASE + 6, rowY + 4, hoverX ? 0xFFFF6666 : 0xFFD08080);
        }

        int footerY = y0 + PANEL_H - PANEL_FOOTER + 3;
        if (list.entries().size() > rows) {
            graphics.text(this.font,
                    Component.translatable("justarod.slime.gui.more", list.entries().size() - rows),
                    x0 + 5, footerY, 0xFF909090);
        } else {
            graphics.text(this.font, Component.translatable("justarod.slime.gui.hint_take"),
                    x0 + 5, footerY, 0xFF909090);
        }
    }

    /** 「取出」按钮的点击/悬停区域（渲染与 mouseClicked 共用，避免两处坐标写不一致） */
    private boolean isOverRelease(double mouseX, double mouseY, int x0, int rowY) {
        return mouseX >= x0 + COL_RELEASE - 1 && mouseX < x0 + PANEL_W - 3
                && mouseY >= rowY + 1 && mouseY < rowY + ROW_H - 3;
    }

    private ItemStack iconFor(SlimeEntityListPayload.Entry entry) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.byId(entry.entityTypeId());
        if (type != null) {
            // 26.x：刷怪蛋不再有 byId，改为在刷怪蛋物品上查 ENTITY_DATA 组件
            for (Item item : BuiltInRegistries.ITEM) {
                if (!(item instanceof SpawnEggItem)) continue;
                ItemStack candidate = new ItemStack(item);
                TypedEntityData<EntityType<?>> data = candidate.get(DataComponents.ENTITY_DATA);
                if (data != null && data.type() == type) {
                    return candidate;
                }
            }
        }
        return new ItemStack(net.minecraft.world.item.Items.SLIME_BALL);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        // 实体页：点击最右侧的 × 取出对应实体
        if (menu.getTab() == SlimeMenu.TAB_ENTITIES) {
            SlimeEntityListPayload list = entityList();
            if (list != null) {
                double mx = event.x();
                double my = event.y();
                int x0 = this.leftPos + PANEL_X;
                int y0 = this.topPos + PANEL_Y;
                int rows = Math.min(MAX_ROWS, list.entries().size());
                for (int row = 0; row < rows; row++) {
                    int rowY = y0 + PANEL_HEADER + row * ROW_H;
                    SlimeEntityListPayload.Entry entry = list.entries().get(row);
                    if (isOverRelease(mx, my, x0, rowY)) {
                        ClientPlayNetworking.send(SlimeContainerActionPayload.ofUuid(menu.containerId,
                                SlimeContainerActionPayload.ACTION_RELEASE_ENTITY, entry.uuid()));
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    /** 被吞在体内时的挣扎按键入口 */
    public static void sendStruggle(int containerId) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity vehicle = minecraft.player == null ? null : minecraft.player.getVehicle();
        if (vehicle instanceof TamedSlimeEntity) {
            ClientPlayNetworking.send(SlimeContainerActionPayload.of(containerId,
                    SlimeContainerActionPayload.ACTION_STRUGGLE, 0, 0));
        }
    }
}
