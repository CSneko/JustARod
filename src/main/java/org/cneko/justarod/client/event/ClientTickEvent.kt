package org.cneko.justarod.client.event

import com.mojang.blaze3d.systems.RenderSystem
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.ShaderProgram
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.*
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.item.ItemStack
import org.cneko.justarod.effect.JREffects
import org.cneko.justarod.item.hasEffect

class ClientTickEvent {
    companion object{
        fun init(){
            HudRenderCallback.EVENT.register {context,_->
                val client = MinecraftClient.getInstance()
                if (client.player != null && client.player!!.hasEffect(JREffects.ESTRUS_EFFECT)) {
                    renderPinkGUI()
                    renderPinkOverlay(context)
                }
            }
        }

        private fun renderPinkGUI() {
            // 设置半透明粉色
            RenderSystem.enableBlend()
            RenderSystem.defaultBlendFunc()
            RenderSystem.setShaderColor(1.0f, 0.6f, 0.8f, 0.5f) // RGB + 透明度
            RenderSystem.setShader(GameRenderer::getPositionProgram)
        }
private fun renderPinkOverlay(context: DrawContext) {
    val client = MinecraftClient.getInstance()
    val width = client.window.scaledWidth
    val height = client.window.scaledHeight

    // 启用混合模式以支持透明
    RenderSystem.enableBlend()
    RenderSystem.defaultBlendFunc()

    // 设置透明粉色
    RenderSystem.setShaderColor(1.0f, 0.71f, 0.76f, 0.2f) // RGB(255, 182, 193) + 透明度 80%

    // 使用 OpenGL 绘制一个覆盖整个屏幕的矩形
    val bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION)
    val matrix = context.matrices.peek().positionMatrix

    bufferBuilder.vertex(matrix, 0f, height.toFloat(), 0f)
    bufferBuilder.vertex(matrix, width.toFloat(), height.toFloat(), 0f)
    bufferBuilder.vertex(matrix, width.toFloat(), 0f, 0f)
    bufferBuilder.vertex(matrix, 0f, 0f, 0f)
    BufferRenderer.drawWithGlobalProgram(bufferBuilder.end())

    // 禁用混合模式
    RenderSystem.disableBlend()
}

    }
}


