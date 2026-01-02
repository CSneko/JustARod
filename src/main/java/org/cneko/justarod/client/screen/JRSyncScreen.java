package org.cneko.justarod.client.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JRSyncScreen extends Screen {

    private final PlayerEntity player;

    // 分页相关变量
    private final List<DataLine> dataList = new ArrayList<>();
    private int currentPage = 0;
    private final int itemsPerPage = 10;
    private int totalPages = 0;

    private ButtonWidget prevBtn;
    private ButtonWidget nextBtn;

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

    private void prepareData() {
        dataList.clear();


        // === 基础信息 ===
        addEntry("性别", getGenderString(player));
        addEntry("体力", String.format("%.2f", player.getPower()));

        // === 生殖相关 ===
        // 使用枚举获取文本
        addEntry("当前生理周期", player.getMenstruationCycle().text);

        // 转换 Ticks -> 分钟
        addEntry("月经痛苦减轻剩余", formatTime(player.getMenstruationComfort()));

        addEntry("拥有子宫", formatBool(player.hasUterus()));

        // 假设怀孕数值也是时间，如果 > 0 显示分钟，否则显示否
        if (player.getPregnant() > 0) {
            addEntry("怀孕进度/时间", formatTime(player.getPregnant()));
        } else {
            addEntry("是否怀孕", "否");
        }

        addEntry("胚胎类型", formatEntityType(player.getChildrenType()));
        addEntry("胎儿数量", String.valueOf(player.getBabyCount()));

        // === 状态/疾病 (int 模拟 bool: 0=否, >0=是) ===
        addEntry("正在避孕", formatIntBool(player.getBrithControlling()));
        addEntry("绝育状态", formatBool(player.isSterilization()));
        addEntry("宫外孕", formatBool(player.isEctopicPregnancy()));
        addEntry("葡萄胎", formatBool(player.isHydatidiformMole()));
        addEntry("多囊卵巢 (PCOS)", formatBool(player.isPCOS()));

        // === 严重疾病 ===
        addEntry("艾滋病 (AIDS)", formatIntBool(player.getAids()));
        addEntry("艾滋免疫", formatBool(player.isImmune2Aids()));
        addEntry("梅毒 (Syphilis)", formatIntBool(player.getSyphilis()));
        addEntry("HPV 感染", formatIntBool(player.getHPV()));
        addEntry("HPV 免疫", formatBool(player.isImmune2HPV()));
        addEntry("卵巢癌", formatIntBool(player.getOvarianCancer()));
        addEntry("乳腺癌", formatIntBool(player.getBreastCancer()));
        addEntry("前列腺炎", formatIntBool(player.getProstatitis()));

        // === 身体状况 ===
        addEntry("截肢状态", formatBool(player.isAmputated()));
        addEntry("睾丸切除", formatBool(player.isOrchiectomy()));
        addEntry("排泄值", String.valueOf(player.getExcretion()));
        addEntry("排尿值", String.valueOf(player.getUrination()));

        // 计算总页数
        this.totalPages = (int) Math.ceil((double) dataList.size() / itemsPerPage);
        if (currentPage >= totalPages && totalPages > 0) currentPage = totalPages - 1;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
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

            // Key (灰色)
            context.drawTextWithShadow(this.textRenderer, Text.of(line.key + ": "),
                    this.width / 2 - 100, y, 0xAAAAAA);

            // Value (带颜色)
            context.drawTextWithShadow(this.textRenderer, Text.of(line.value),
                    this.width / 2 + 10, y, getColorForValue(line.value));
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void updateButtons() {
        if (prevBtn != null) prevBtn.active = currentPage > 0;
        if (nextBtn != null) nextBtn.active = currentPage < totalPages - 1;
    }

    // --- 辅助方法 ---

    private void addEntry(String key, String value) {
        dataList.add(new DataLine(key, value));
    }

    /**
     * 将 Tick 转换为 分钟
     * 1 分钟 = 1200 ticks
     */
    private String formatTime(int ticks) {
        if (ticks <= 0) return "0";
        // 保留1位小数，例如 "5.5 分钟"
        double minutes = ticks / 1200.0;
        if (minutes < 0.1) return "< 0.1 分钟";
        return String.format("%.1f 分钟", minutes);
    }

    private String getGenderString(PlayerEntity p) {
        boolean isMale = p.isMale();
        boolean isFemale = p.isFemale();
        if (isMale && isFemale) return "扶他";
        if (isMale) return "男";
        if (isFemale) return "女";
        return "无性别";
    }

    private String formatBool(boolean val) {
        return val ? "是" : "否";
    }

    private String formatIntBool(int val) {
        return val > 0 ? "是" : "否";
    }

    private String formatEntityType(Object optRaw) {
        if (optRaw instanceof Optional<?> opt) {
            if (opt.isPresent() && opt.get() instanceof EntityType<?> type) {
                return type.getName().getString();
            }
        }
        return "无";
    }

    // 根据值的含义给颜色
    private int getColorForValue(String val) {
        if (val.equals("是") || val.contains("分钟")) return 0xFF5555; // 红色 (强调状态或时间)
        if (val.equals("否") || val.equals("0")) return 0x55FF55;    // 绿色
        if (val.equals("无")) return 0xAAAAAA;    // 灰色
        if (val.equals("男") || val.equals("女") || val.equals("扶他")) return 0xFFFF55; // 黄色
        return 0xFFFFFF; // 默认白
    }

    private record DataLine(String key, String value) {}
}