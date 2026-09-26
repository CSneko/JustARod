package org.cneko.justarod.client.tooltip

import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.client.renderer.RenderPipelines
import org.cneko.justarod.item.tooltip.ChemicalStructureTooltipData
import kotlin.math.max
import kotlin.math.min

/**
 * 26.x：ClientTooltipComponent 的 renderImage(GuiGraphics) 改为
 * extractImage(Font, x, y, width, height, GuiGraphicsExtractor)；getHeight 也需要 Font 参数。
 */
class ChemicalStructureTooltipComponent(data: ChemicalStructureTooltipData) : ClientTooltipComponent {
    private val texture = data.texture

    // 1. 直接定义 Tooltip 框框的最大尺寸限制
    private val MAX_BOX_WIDTH = 60
    private val MAX_BOX_HEIGHT = 40

    // 图片本身的原始尺寸
    private var origWidth = 1
    private var origHeight = 1

    // 实际要渲染的图片尺寸
    private var imageRenderWidth = 0
    private var imageRenderHeight = 0

    init {
        // 读取图片的原始尺寸（带防崩溃保护）
        val (rawW, rawH) = StructureTextureCache.getOriginalSize(texture)
        origWidth = max(1, rawW)
        origHeight = max(1, rawH)

        // 计算图片如果等比例缩小，需要缩放多少
        val scaleW = MAX_BOX_WIDTH / origWidth.toDouble()
        val scaleH = MAX_BOX_HEIGHT / origHeight.toDouble()

        // 限制最大缩放倍数为 1.0（意思是：大图会被缩小，但小图不会被强行放大变模糊）
        val scale = min(1.0, min(scaleW, scaleH))

        // 计算出图片在框框里应该占据的实际大小
        imageRenderWidth = max(1, (origWidth * scale).toInt())
        imageRenderHeight = max(1, (origHeight * scale).toInt())
    }

    // === 核心：限制 Tooltip 框框的大小 ===

    override fun getWidth(textRenderer: Font): Int {
        // 框框的宽度：直接使用图片缩放后的宽度
        return imageRenderWidth
    }

    override fun getHeight(textRenderer: Font): Int {
        // 26.x：getHeight 现在带 Font 参数
        // 框框的高度：使用图片缩放后的高度 + 4 像素边距
        return imageRenderHeight + 4
    }

    // === 核心：在框框内渲染图片 ===

    override fun extractImage(textRenderer: Font, x: Int, y: Int, width: Int, height: Int, context: GuiGraphicsExtractor) {
        if (imageRenderWidth <= 0 || imageRenderHeight <= 0) return

        // 计算居中的偏移量 (如果图片比框框小，让它在框框的正中间)
        val offsetX = (getWidth(textRenderer) - imageRenderWidth) / 2
        val offsetY = ((getHeight(textRenderer) - 4) - imageRenderHeight) / 2

        val drawX = x + offsetX
        val drawY = y + offsetY + 2 // +2 是为了上下留一点空隙

        // 26.x：blit 需要 RenderPipeline 参数，使用拉伸重载把整张贴图缩放进目标区域
        context.blit(
            RenderPipelines.GUI_TEXTURED,
            texture,
            drawX, drawY,                        // 在屏幕上的坐标
            0f, 0f,                              // UV 坐标
            imageRenderWidth, imageRenderHeight, // 在屏幕上画多大
            origWidth, origHeight,               // 读取原图的区域大小
            origWidth, origHeight                // 原图文件的真实宽高
        )
    }
}
