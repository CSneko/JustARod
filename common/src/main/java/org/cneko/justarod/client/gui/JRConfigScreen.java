package org.cneko.justarod.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import org.cneko.justarod.config.ConfigBuilder;
import org.cneko.justarod.config.JRConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * JustARod 配置屏幕（粉色主题，结构参考 toNeko 的 ConfigScreen）。
 * 左侧为按分组折叠的配置树，右侧是布尔开关按钮 / 数值输入框，
 * 底部有「应用」与「关闭」按钮，列表末尾附带一条随机语录彩蛋。
 */
public class JRConfigScreen extends Screen {
    private final Screen lastScreen;
    private ConfigListWidget configList;
    private final List<ConfigNode> configTree;

    // == 配色常量 (可爱粉色系) ==
    private static final int COLOR_PINK_ACCENT = 0xFFFF69B4; // 亮粉色
    private static final int COLOR_PINK_SOFT = 0xFFFFB6C1;   // 浅粉色
    private static final int COLOR_LIST_BG = 0x60000000;     // 列表半透明黑底
    private static final int COLOR_LIST_BORDER = 0xFFFF69B4; // 列表边框

    public JRConfigScreen(Screen lastScreen) {
        super(Component.translatable("screen.justarod.config.title"));
        this.lastScreen = lastScreen;
        this.configTree = buildConfigTree();
    }

    public JRConfigScreen() {
        this(null);
    }

    @Override
    public void init() {
        super.init();

        // 1. 列表区域
        int listWidth = (int) (this.width * 0.75);
        int listHeight = (int) (this.height * 0.65);
        int listX = (this.width - listWidth) / 2;
        int listY = (int) (this.height * 0.15);

        this.configList = new ConfigListWidget(listX, listY, listWidth, listHeight);
        addRenderableWidget(this.configList);

        rebuildList();

        // 2. 底部按钮
        int btnWidth = 100;
        int btnHeight = 20;
        int btnY = (int) (this.height * 0.88);
        int center = this.width / 2;

        addRenderableWidget(Button.builder(Component.translatable("screen.justarod.config.button.quit"), btn -> onClose())
                .bounds(center - btnWidth - 10, btnY, btnWidth, btnHeight)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("screen.justarod.config.button.apply"), btn ->
                        JRConfig.save())
                .bounds(center + 10, btnY, btnWidth, btnHeight)
                .build());
    }

    private void rebuildList() {
        // 保存当前滚动的距离，重建后恢复
        double oldScroll = configList != null ? configList.getScrollAmount() : 0;

        configList.clearEntries();
        for (ConfigNode node : configTree) {
            addNodeToUI(node, 0);
        }
        configList.addEntry(new RandomTextEntry(configList.width));

        configList.setScrollAmount(oldScroll);
    }

    private void addNodeToUI(ConfigNode node, int indent) {
        if (node.isGroupHeader()) {
            configList.addEntry(new GroupHeaderEntry(node, indent, configList.width, this::rebuildList));
            if (!node.collapsed) {
                for (ConfigNode child : node.children) {
                    addNodeToUI(child, indent + 12);
                }
            }
        } else if (node.fullKey != null) {
            configList.addEntry(new ConfigSettingEntry(node, indent, configList.width));
        }
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        Component title = Component.translatable("screen.justarod.config.title")
                .withStyle(ChatFormatting.BOLD, ChatFormatting.LIGHT_PURPLE);
        guiGraphics.centeredText(this.font, title, this.width / 2, 20, 0xFFFFFFFF);

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }

    /**
     * 关键修复：Screen 必须显式处理点击并设置焦点
     */
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (super.mouseClicked(event, doubled)) {
            for (GuiEventListener child : this.children()) {
                if (child.isMouseOver(event.x(), event.y())) {
                    this.setFocused(child);
                    break;
                }
            }
            return true;
        }
        this.setFocused(null);
        return false;
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(lastScreen);
        }
    }

    // ================== 数据结构 ==================
    private List<ConfigNode> buildConfigTree() {
        List<ConfigNode> roots = new ArrayList<>();
        List<String> keys = JRConfig.CONFIG_BUILDER.getKeys();
        for (String key : keys) {
            insertNode(roots, key.split("\\."), 0, key);
        }
        return roots;
    }

    private void insertNode(List<ConfigNode> nodes, String[] parts, int index, String fullKey) {
        String part = parts[index];
        ConfigNode node = nodes.stream().filter(n -> n.name.equals(part)).findFirst().orElse(null);
        if (node == null) {
            node = new ConfigNode(part);
            nodes.add(node);
        }
        if (index == parts.length - 1) {
            node.fullKey = fullKey;
            node.entry = JRConfig.CONFIG_BUILDER.get(fullKey);
        } else {
            if (node.fullKey != null) return;
            insertNode(node.children, parts, index + 1, fullKey);
        }
    }

    public static class ConfigNode {
        String name;
        @Nullable String fullKey;
        @Nullable ConfigBuilder.Entry entry;
        List<ConfigNode> children = new ArrayList<>();
        boolean collapsed = false;
        public ConfigNode(String name) { this.name = name; }
        public boolean isGroupHeader() { return entry == null && !children.isEmpty(); }
    }

    // ================== 核心 UI 组件：ConfigListWidget ==================

    private static class ConfigListWidget extends AbstractContainerWidget {
        private final List<Entry> entries = new ArrayList<>();
        private boolean isDraggingScrollbar = false;
        private final int width, height;

        @Override
        protected int contentHeight() {
            return entries.stream().mapToInt(Entry::getHeight).sum();
        }

        public ConfigListWidget(int x, int y, int width, int height) {
            // 26.x：AbstractContainerWidget 继承 AbstractScrollArea，需要滚动条设置
            super(x, y, width, height, Component.empty(),
                    net.minecraft.client.gui.components.AbstractScrollArea.defaultSettings(height));
            this.width = width;
            this.height = height;
        }

        public void addEntry(Entry entry) { entries.add(entry); }
        public void clearEntries() { entries.clear(); }
        private int getContentHeight() { return entries.stream().mapToInt(Entry::getHeight).sum(); }
        public double getScrollAmount() { return super.scrollAmount(); }
        @Override
        public void setScrollAmount(double amount) {
            super.setScrollAmount(Mth.clamp(amount, 0, Math.max(0, getContentHeight() - this.height)));
        }

        @Override
        public void extractWidgetRenderState(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
            guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, COLOR_LIST_BG);
            // 26.x：GuiGraphicsExtractor 移除 renderOutline，用四条 1px fill 画边框
            guiGraphics.fill(getX() - 1, getY() - 1, getX() + width + 1, getY(), COLOR_LIST_BORDER);
            guiGraphics.fill(getX() - 1, getY() + height, getX() + width + 1, getY() + height + 1, COLOR_LIST_BORDER);
            guiGraphics.fill(getX() - 1, getY(), getX(), getY() + height, COLOR_LIST_BORDER);
            guiGraphics.fill(getX() + width, getY(), getX() + width + 1, getY() + height, COLOR_LIST_BORDER);

            guiGraphics.enableScissor(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1);

            List<Component> tooltip = null;
            int currentY = getY() - (int) scrollAmount() + 4;
            for (Entry entry : entries) {
                if (currentY + entry.getHeight() > getY() && currentY < getY() + height) {
                    entry.render(guiGraphics, getX(), currentY, mouseX, mouseY, partialTick);
                    // 收集悬停提示（在裁剪区外渲染，避免被裁剪）
                    List<Component> t = entry.getTooltip(getX(), currentY, mouseX, mouseY);
                    if (t != null) tooltip = t;
                }
                currentY += entry.getHeight();
            }

            guiGraphics.disableScissor();

            if (tooltip != null) {
                guiGraphics.setTooltipForNextFrame(Minecraft.getInstance().font, tooltip, java.util.Optional.empty(), mouseX, mouseY);
            }

            int contentHeight = getContentHeight();
            if (contentHeight > height) {
                int scrollbarWidth = 4;
                int scrollbarX = getX() + width - scrollbarWidth - 4;
                int barHeight = Math.max(20, (int) ((float) height / contentHeight * height));
                int barY = getY() + (int) ((float) scrollAmount() / contentHeight * height);

                int color = isDraggingScrollbar ? COLOR_PINK_ACCENT : COLOR_PINK_SOFT;
                guiGraphics.fill(scrollbarX, barY, scrollbarX + scrollbarWidth, barY + barHeight, color | 0xFF000000);
            }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
            double mouseX = event.x(), mouseY = event.y();
            int button = event.button();
            // 1. 滚动条逻辑
            int contentHeight = getContentHeight();
            if (contentHeight > height) {
                int scrollbarX = getX() + width - 10;
                if (mouseX >= scrollbarX && mouseX <= getX() + width) {
                    isDraggingScrollbar = true;
                    return true;
                }
            }

            // 2. 内容逻辑 (焦点管理)
            if (isMouseOver(mouseX, mouseY)) {
                double localY = mouseY - getY() + scrollAmount() - 4;
                int currentY = 0;

                // 先清除所有子项的焦点，防止多个输入框同时闪烁
                for (Entry entry : entries) {
                    entry.setFocused(false);
                }

                for (Entry entry : entries) {
                    if (localY >= currentY && localY < currentY + entry.getHeight()) {
                        if (entry.mouseClicked((double) mouseX, (double) mouseY, button)) {
                            this.setFocused(entry);
                            entry.setFocused(true);
                            return true;
                        }
                    }
                    currentY += entry.getHeight();
                }
            }

            this.setFocused(null);
            return false;
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            GuiEventListener focused = getFocused();
            if (focused != null && focused.keyPressed(event)) {
                return true;
            }
            return super.keyPressed(event);
        }

        @Override
        public boolean charTyped(CharacterEvent event) {
            GuiEventListener focused = getFocused();
            if (focused != null && focused.charTyped(event)) {
                return true;
            }
            return super.charTyped(event);
        }

        @Override
        public boolean mouseReleased(MouseButtonEvent event) {
            isDraggingScrollbar = false;
            return super.mouseReleased(event);
        }

        @Override
        public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
            if (isDraggingScrollbar && getContentHeight() > height) {
                double ratio = (double) getContentHeight() / height;
                setScrollAmount(scrollAmount() + dragY * ratio);
                return true;
            }
            return super.mouseDragged(event, dragX, dragY);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            if (getContentHeight() > height) {
                setScrollAmount(scrollAmount() - scrollY * 20);
                return true;
            }
            return false;
        }

        @Override
        public @NotNull List<? extends GuiEventListener> children() {
            List<GuiEventListener> all = new ArrayList<>();
            for (Entry e : entries) all.addAll(e.children());
            return all;
        }

        @Override
        protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {}
    }

    // ================== Entry 组件 ==================

    private abstract static class Entry implements GuiEventListener {
        public abstract void render(GuiGraphicsExtractor guiGraphics, int x, int y, int mouseX, int mouseY, float partialTick);
        public abstract int getHeight();
        public abstract List<? extends GuiEventListener> children();

        /** 鼠标悬停在该行上时返回要显示的工具提示，默认无 */
        @Nullable
        public List<Component> getTooltip(int x, int y, double mouseX, double mouseY) { return null; }

        // 用于手动管理焦点状态
        public abstract void setFocused(boolean focused);

        // 26.x：Entry 内部的点击判定方法（非 GuiEventListener 的覆盖）
        public boolean mouseClicked(double mouseX, double mouseY, int button) { return false; }
    }

    private static class GroupHeaderEntry extends Entry {
        private final ConfigNode node;
        private final int indent;
        private final Runnable onToggle;
        private final int width;

        public GroupHeaderEntry(ConfigNode node, int indent, int width, Runnable onToggle) {
            this.node = node;
            this.indent = indent;
            this.width = width;
            this.onToggle = onToggle;
        }

        @Override
        public void render(GuiGraphicsExtractor guiGraphics, int x, int y, int mouseX, int mouseY, float partialTick) {
            String arrow = node.collapsed ? "▶ " : "▼ ";
            MutableComponent text = Component.literal(arrow).withStyle(ChatFormatting.LIGHT_PURPLE)
                    .append(Component.translatable("screen.justarod.config.group." + node.name).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

            if (mouseY >= y && mouseY < y + getHeight() && mouseX >= x && mouseX <= x + width) {
                guiGraphics.fill(x + 2, y, x + width - 2, y + getHeight(), 0x15FFFFFF);
            }

            guiGraphics.text(Minecraft.getInstance().font, text, x + 5 + indent, y + 6, 0xFFFFFFFF, false);
            guiGraphics.fill(x + 10, y + getHeight() - 1, x + width - 10, y + getHeight(), 0x40FF69B4);
        }

        // 26.x：Entry 的输入事件为自定义方法（不再覆盖 GuiEventListener）
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            node.collapsed = !node.collapsed;
            onToggle.run();
            Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }

        @Override public boolean isFocused() { return false; }
        @Override public void setFocused(boolean focused) {}
        @Override public int getHeight() { return 24; }
        @Override public List<? extends GuiEventListener> children() { return List.of(); }
    }

    private static class ConfigSettingEntry extends Entry {
        private final ConfigNode node;
        private final int indent;
        private final int rowWidth;
        private final List<AbstractWidget> widgets = new ArrayList<>();
        private final AbstractWidget inputWidget;
        private int lastRenderX, lastRenderY;

        public ConfigSettingEntry(ConfigNode node, int indent, int rowWidth) {
            this.node = node;
            this.indent = indent;
            this.rowWidth = rowWidth;

            int inputWidth = 140;

            if (node.entry.type == ConfigBuilder.Entry.Types.BOOLEAN) {
                boolean currentVal = JRConfig.CONFIG_BUILDER.getConfig().getBoolean(node.fullKey);
                inputWidget = Button.builder(getBoolText(currentVal), btn -> {
                            boolean newVal = !JRConfig.CONFIG_BUILDER.getConfig().getBoolean(node.fullKey);
                            JRConfig.CONFIG_BUILDER.getConfig().set(node.fullKey, newVal);
                            btn.setMessage(getBoolText(newVal));
                        }).bounds(0, 0, inputWidth, 20).build();
            } else {
                EditBox editBox = new EditBox(Minecraft.getInstance().font, 0, 0, inputWidth, 20, Component.nullToEmpty(node.fullKey));
                editBox.setMaxLength(32);
                editBox.setValue(String.valueOf(node.entry.value));
                editBox.setResponder(text -> JRConfig.CONFIG_BUILDER.getConfig().set(node.fullKey, text));
                editBox.setBordered(true);
                editBox.setTextColor(0xFFFFFFFF);
                inputWidget = editBox;
            }
            widgets.add(inputWidget);
        }

        private Component getBoolText(boolean val) {
            return val ? Component.literal("✅ ").append(Component.translatable("screen.justarod.config.button.true")).withStyle(ChatFormatting.GREEN)
                    : Component.literal("❌ ").append(Component.translatable("screen.justarod.config.button.false")).withStyle(ChatFormatting.RED);
        }

        @Override
        public void render(GuiGraphicsExtractor guiGraphics, int x, int y, int mouseX, int mouseY, float partialTick) {
            this.lastRenderX = x;
            this.lastRenderY = y;

            Component label = Component.translatable("screen.justarod.config.key." + node.fullKey);
            guiGraphics.text(Minecraft.getInstance().font, label, x + 10 + indent, y + 6, 0xFFE0E0E0, false);

            int inputX = x + rowWidth - inputWidget.getWidth() - 10;
            inputWidget.setX(inputX);
            inputWidget.setY(y);
            inputWidget.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        }

        @Override
        public List<Component> getTooltip(int x, int y, double mouseX, double mouseY) {
            if (node.entry == null) return null;
            // 鼠标悬停在整行上时，显示键名与注释作为工具提示
            if (mouseX >= x && mouseX <= x + rowWidth && mouseY >= y && mouseY < y + getHeight()) {
                return List.of(
                        Component.literal(node.fullKey).withStyle(ChatFormatting.GRAY),
                        Component.literal(node.entry.comment.replace("\n", " ")).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            return null;
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (inputWidget.mouseClicked(new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0)), false)) {
                if (inputWidget instanceof EditBox editBox) {
                    editBox.setFocused(true);
                }
                return true;
            } else {
                if (inputWidget instanceof EditBox editBox) {
                    editBox.setFocused(false);
                }
            }
            return false;
        }

        @Override
        public void setFocused(boolean focused) {
            if (inputWidget instanceof EditBox editBox) {
                editBox.setFocused(focused);
            }
        }

        @Override
        public boolean isFocused() {
            return inputWidget.isFocused();
        }

        public boolean keyPressed(KeyEvent event) {
            return inputWidget.keyPressed(event);
        }

        public boolean charTyped(CharacterEvent event) {
            return inputWidget.charTyped(event);
        }

        @Override public int getHeight() { return 26; }
        @Override public List<? extends GuiEventListener> children() { return widgets; }
    }

    private static class RandomTextEntry extends Entry {
        private final int width;
        private final Button randomBtn;
        private Component currentText;
        private static final int MAX_RANDOM = 8;

        public RandomTextEntry(int width) {
            this.width = width;
            updateText();
            this.randomBtn = Button.builder(Component.translatable("screen.justarod.config.button.random"), btn -> updateText())
                    .bounds(0, 0, 80, 20).build();
        }

        private void updateText() {
            int r = new Random().nextInt(MAX_RANDOM);
            currentText = Component.translatable("screen.justarod.config.random." + r)
                    .withStyle(ChatFormatting.ITALIC, ChatFormatting.LIGHT_PURPLE);
        }

        @Override
        public void render(GuiGraphicsExtractor guiGraphics, int x, int y, int mouseX, int mouseY, float partialTick) {
            guiGraphics.centeredText(Minecraft.getInstance().font, currentText, x + width / 2, y + 8, 0xFFFFFFFF);
            randomBtn.setX(x + (width - randomBtn.getWidth()) / 2);
            randomBtn.setY(y + 25);
            randomBtn.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button) { return randomBtn.mouseClicked(new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0)), false); }
        @Override public void setFocused(boolean focused) { randomBtn.setFocused(focused); }
        @Override public boolean isFocused() { return randomBtn.isFocused(); }
        @Override public int getHeight() { return 55; }
        @Override public List<? extends GuiEventListener> children() { return List.of(randomBtn); }
    }
}
