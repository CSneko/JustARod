package org.cneko.justarod.client.event

import com.mojang.blaze3d.systems.RenderSystem
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.PostEffectProcessor
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.*
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.Identifier
import org.cneko.justarod.effect.JREffects
import org.cneko.justarod.item.hasEffect
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

class ClientTickEvent {
    companion object{
        fun init(){
            HudRenderCallback.EVENT.register {context,_->
                val client = MinecraftClient.getInstance()
                if (client.player != null && client.player!!.hasEffect(JREffects.ESTRUS_EFFECT) || client.player!!.hasEffect(JREffects.ORGASM_EFFECT)) {
                    // 粉嫩粉嫩的
                    renderPinkGUI()
                    renderPinkOverlay(context)
                }
                if (client.player!!.hasEffect(JREffects.ORGASM_EFFECT)){
                    // 抖起来！！！
                    val intensity = client.window.scaledWidth * (0.00f + Random().nextFloat() * 0.005f)
                    applyScreenShake(context, intensity)
                    applyViewShake(client.player!!, intensity)
                }
                if (client.player!!.hasEffect(JREffects.FAINT_EFFECT)){
                    // 晕了
                    renderFaintEffect(context)
                }else {
                    faintAlpha = 0f
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

            // 动态透明度，基于随机数波动
            val baseAlpha = 0.3f
            val fluctuation = 0.01f
            val alpha = baseAlpha + Random().nextFloat() * fluctuation

            // 启用混合模式以支持透明
            RenderSystem.enableBlend()
            RenderSystem.defaultBlendFunc()

            // 设置透明粉色
            RenderSystem.setShaderColor(1.0f, 0.71f, 0.76f, alpha) // 动态透明度

            // 使用 OpenGL 绘制一个覆盖整个屏幕的矩形
            val bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION)
            val matrix = context.matrices.peek().positionMatrix

            with(bufferBuilder) {
                vertex(matrix, 0f, height.toFloat(), 0f)
                vertex(matrix, width.toFloat(), height.toFloat(), 0f)
                vertex(matrix, width.toFloat(), 0f, 0f)
                vertex(matrix, 0f, 0f, 0f)
            }
            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end())

            // 禁用混合模式
            RenderSystem.disableBlend()
        }




        private fun applyScreenShake(context: DrawContext, intensity: Float) {
            val random = Random()
            val shakeX = (random.nextFloat() - 0.5f) * 2 * intensity // 随机偏移X，范围 [-intensity, intensity]
            val shakeY = (random.nextFloat() - 0.5f) * 2 * intensity // 随机偏移Y，范围 [-intensity, intensity]

            // 在当前渲染矩阵中应用偏移
            context.matrices.translate(shakeX.toDouble(), shakeY.toDouble(), 0.0)
        }

        private fun applyViewShake(player: PlayerEntity, intensity: Float) {
            val random = Random()

            // 随机生成偏移角度，控制范围为 [-intensity, intensity]
            val shakeYaw = (random.nextFloat() - 0.5f) * 2 * intensity
            val shakePitch = (random.nextFloat() - 0.5f) * 2 * intensity

            // 修改玩家视角
            player.yaw += shakeYaw
            player.pitch = (player.pitch + shakePitch).coerceIn(-90f, 90f) // 限制 pitch 在 [-90, 90] 范围内
        }



        private var faintAlpha = 0f
        private fun renderFaintEffect(context: DrawContext) {
            val client = MinecraftClient.getInstance()
            val width = client.window.scaledWidth
            val height = client.window.scaledHeight

            // 淡入黑屏效果：通过帧时间线性增加 alpha
            val maxAlpha = 0.95f // 接近全黑
            val fadeSpeed = 0.01f // 每帧增加的透明度
            faintAlpha = (faintAlpha + fadeSpeed).coerceAtMost(maxAlpha)

            // 启用混合模式
            RenderSystem.enableBlend()
            RenderSystem.defaultBlendFunc()

            // 设置全黑遮罩颜色和透明度
            RenderSystem.setShaderColor(0f, 0f, 0f, faintAlpha)

            // 绘制全屏黑色覆盖层
            val bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION)
            val matrix = context.matrices.peek().positionMatrix

            with(bufferBuilder) {
                vertex(matrix, 0f, height.toFloat(), 0f)
                vertex(matrix, width.toFloat(), height.toFloat(), 0f)
                vertex(matrix, width.toFloat(), 0f, 0f)
                vertex(matrix, 0f, 0f, 0f)
            }
            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end())

            RenderSystem.disableBlend()
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f) // 重置颜色
        }



    }
}



