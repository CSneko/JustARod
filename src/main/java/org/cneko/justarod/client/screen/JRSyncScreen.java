package org.cneko.justarod.client.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.cneko.justarod.property.JRProperty;
import org.cneko.justarod.property.JRRegistry;

import java.util.ArrayList;
import java.util.List;

public class JRSyncScreen extends Screen {

    private final PlayerEntity player;

    // 分页相关变量
    private final List<DataLine> dataList = new ArrayList<>();
    private int currentPage = 0;
    private final int itemsPerPage = 10;
    private int totalPages = 0;

    private ButtonWidget prevBtn;
    private ButtonWidget nextBtn;

    // 刷新计时器 (20 tick = 1 秒)
    private int tickCounter = 0;

    public JRSyncScreen(PlayerEntity player) {
        super(Text.of("身体状态监控面板"));
        this.player = player;
    }

    @Override
    protected void init() {
        super.init();

        // 1. 准备数据
        prepareData();

        // 2. 初始化按钮布局
        int btnWidth = 80;
        int btnHeight = 20;
        int bottomY = this.height - 40;
        int centerX = this.width / 2;

        // 上一页按钮
        this.prevBtn = ButtonWidget.builder(Text.of("上一页"), button -> {
            if (currentPage > 0) currentPage--;
            updateButtons();
        }).dimensions(centerX - 90, bottomY, btnWidth, btnHeight).build();

        // 下一页按钮
        this.nextBtn = ButtonWidget.builder(Text.of("下一页"), button -> {
            if (currentPage < totalPages - 1) currentPage++;
            updateButtons();
        }).dimensions(centerX + 10, bottomY, btnWidth, btnHeight).build();

        // 关闭按钮
        this.addDrawableChild(ButtonWidget.builder(Text.of("关闭"), button -> this.close())
                .dimensions(centerX - 40, bottomY + 25, 80, 20).build());

        this.addDrawableChild(prevBtn);
        this.addDrawableChild(nextBtn);

        updateButtons();
    }

    // ================= 新增：Tick更新逻辑 =================
    @Override
    public void tick() {
        super.tick();
        this.tickCounter++;

        // 每 20 tick (大约1秒) 刷新一次数据
        if (this.tickCounter >= 20) {
            this.tickCounter = 0;
            this.prepareData();

            // 防止由于动态增删属性导致当前页码越界
            if (this.currentPage >= this.totalPages && this.totalPages > 0) {
                this.currentPage = this.totalPages - 1;
            } else if (this.totalPages == 0) {
                this.currentPage = 0;
            }

            // 更新按钮状态
            this.updateButtons();
        }
    }
    // ======================================================

    private void prepareData() {
        dataList.clear();

        // 自动遍历注册表！无论你添加多少属性，UI自动生成
        for (JRProperty<?> prop : JRRegistry.INSTANCE.getPROPERTIES()) {
            // 获取值
            Object rawValue = ((JRProperty<Object>) prop).getGetter().invoke(this.player);
            // 获取格式化后的文字
            String displayStr = ((JRProperty<Object>) prop).formatValue(rawValue);
            // 获取颜色
            int color = ((JRProperty<Object>) prop).getValueColor(rawValue);

            dataList.add(new DataLine(prop.getDisplayName(), displayStr, color));
        }

        // 计算页数
        this.totalPages = (int) Math.ceil((double) dataList.size() / itemsPerPage);
    }

    private record DataLine(String key, String value, int color) {}

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 画背景 (如果你是在较新版本，建议加上这句防止文字重叠看不清)
        this.renderBackground(context, mouseX, mouseY, delta);

        // 标题
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);

        // 页码
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.of(String.format("第 %d / %d 页", currentPage + 1, Math.max(1, totalPages))),
                this.width / 2, this.height - 65, 0xAAAAAA);

        // 列表渲染
        int startY = 40;
        int lineHeight = 12;
        int startIdx = currentPage * itemsPerPage;
        int endIdx = Math.min(startIdx + itemsPerPage, dataList.size());

        for (int i = startIdx; i < endIdx; i++) {
            DataLine line = dataList.get(i);
            int relativeIndex = i - startIdx;
            int y = startY + (relativeIndex * lineHeight);

            context.drawTextWithShadow(this.textRenderer, Text.of(line.key() + ": "),
                    this.width / 2 - 100, y, 0xAAAAAA);
            context.drawTextWithShadow(this.textRenderer, Text.of(line.value),
                    this.width / 2 + 10, y, line.color);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void updateButtons() {
        if (prevBtn != null) prevBtn.active = currentPage > 0;
        if (nextBtn != null) nextBtn.active = currentPage < totalPages - 1;
    }
}