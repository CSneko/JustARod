package org.cneko.justarod.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.cneko.justarod.Justarod;
import org.cneko.justarod.entity.Pregnant;

public abstract class MedicalScanScreen extends Screen {
    public static final Identifier SCANNER_BG = Identifier.of(Justarod.MODID, "textures/gui/medical/scanner_bg.png");

    protected final LivingEntity targetEntity;
    protected final Pregnant pregnantData;

    protected final int imageWidth = 256;
    protected final int imageHeight = 256;

    protected int x;
    protected int y;

    // === 新增：视角控制变量 ===
    protected float zoom = 1.0F;
    protected double panX = 0.0;
    protected double panY = 0.0;

    public MedicalScanScreen(LivingEntity targetEntity) {
        super(Text.literal("扫喵ing"));
        this.targetEntity = targetEntity;
        this.pregnantData = (Pregnant) targetEntity;
    }

    @Override
    protected void init() {
        super.init();
        this.x = (this.width - this.imageWidth) / 2;
        this.y = (this.height - this.imageHeight) / 2;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 1. 绘制扫描仪外框底图 (固定不动)
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        context.drawTexture(SCANNER_BG, this.x, this.y, 0, 0, this.imageWidth, this.imageHeight);

        // 2. 开启剪裁，防止放大或拖动的器官超出 256x256 的屏幕显示区域
        // （如果你的仪器外框有厚重的边框，可以在这里微调 x, y 和 width, height 来缩小剪裁区）
        context.enableScissor(this.x, this.y, this.x + this.imageWidth, this.y + this.imageHeight);

        // 3. 应用矩阵变换：平移与缩放
        context.getMatrices().push();

        // a. 将矩阵原点移动到显示区域的中心
        float centerX = this.x + this.imageWidth / 2.0F;
        float centerY = this.y + this.imageHeight / 2.0F;
        context.getMatrices().translate(centerX, centerY, 0);

        // b. 应用缩放
        context.getMatrices().scale(this.zoom, this.zoom, 1.0F);

        // c. 应用平移（在缩放后的图像空间中平移）
        context.getMatrices().translate(this.panX, this.panY, 0);

        // d. 将原点移回原来的位置，以便子类的 (x, y) 坐标能正常对齐
        context.getMatrices().translate(-centerX, -centerY, 0);

        // 4. 调用子类的具体器官图层绘制
        this.renderOrganLayers(context, this.x, this.y, delta);

        // 5. 恢复矩阵并关闭剪裁
        context.getMatrices().pop();
        context.disableScissor();

        // 6. 渲染其他上层 UI (如工具提示等)
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // 判断鼠标是否在扫描仪区域内，只有在区域内才允许缩放
        if (mouseX >= this.x && mouseX <= this.x + this.imageWidth &&
                mouseY >= this.y && mouseY <= this.y + this.imageHeight) {

            float zoomSensitivity = 0.15F; // 缩放灵敏度
            float newZoom = this.zoom + (float) verticalAmount * zoomSensitivity;

            // 限制缩放级别在 0.5x 到 5.0x 之间
            this.zoom = MathHelper.clamp(newZoom, 0.5F, 5.0F);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    // === 鼠标拖动事件 ===
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0) { // 0 代表鼠标左键
            // 除以 zoom 可以保证你拖动鼠标移动的像素，在视觉上和图像移动的像素严格 1:1，手感更好
            this.panX += dragX / this.zoom;
            this.panY += dragY / this.zoom;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    // === 鼠标点击事件 (右键/中键 恢复默认视角) ===
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1 || button == 2) { // 1=右键, 2=中键
            this.zoom = 1.0F;
            this.panX = 0.0;
            this.panY = 0.0;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    protected abstract void renderOrganLayers(DrawContext context, int x, int y, float delta);

    protected void drawLayer(DrawContext context, Identifier texture, int x, int y) {
        context.drawTexture(texture, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }
}