package org.cneko.justarod.client.tooltip

import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.tooltip.TooltipComponent
import org.cneko.justarod.item.tooltip.ChemicalStructureTooltipData
import kotlin.math.max
import kotlin.math.min

class ChemicalStructureTooltipComponent(data: ChemicalStructureTooltipData) : TooltipComponent {
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

    override fun getWidth(textRenderer: TextRenderer): Int {
        // 框框的宽度：直接使用图片缩放后的宽度
        return imageRenderWidth
    }

    override fun getHeight(): Int {
        // 框框的高度：使用图片缩放后的高度 + 4 像素边距
        return imageRenderHeight + 4
    }

    // === 核心：在框框内渲染图片 ===

    override fun drawItems(textRenderer: TextRenderer, x: Int, y: Int, context: DrawContext) {
        if (imageRenderWidth <= 0 || imageRenderHeight <= 0) return

        // 计算居中的偏移量 (如果图片比框框小，让它在框框的正中间)
        // 注意：因为上面 getWidth 和 getHeight 已经是紧贴图片了，所以这里的 offsetX/Y 一般为 0
        // 但如果你想强制规定框框永远是 120x80，可以修改 getWidth 返回固定的 MAX_BOX_WIDTH，然后这里就会自动居中！
        val offsetX = (getWidth(textRenderer) - imageRenderWidth) / 2
        val offsetY = ((getHeight() - 4) - imageRenderHeight) / 2

        val drawX = x + offsetX
        val drawY = y + offsetY + 2 // +2 是为了上下留一点空隙

        // 使用最底层的渲染方法，把图片精准地塞进计算好的区域里
        context.drawTexture(
            texture,
            drawX, drawY,               // 在屏幕上的坐标
            imageRenderWidth, imageRenderHeight, // 在屏幕上画多大
            0f, 0f,                     // UV 坐标
            origWidth, origHeight,      // 读取原图的全尺寸
            origWidth, origHeight       // 原图文件的真实宽高
        )
    }
}