package org.cneko.justarod.client.event

import com.mojang.blaze3d.systems.RenderSystem
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.*
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.cneko.justarod.JRAttributes
import org.cneko.justarod.JRUtil.Companion.rodId
import org.cneko.justarod.effect.JREffects
import org.cneko.justarod.item.rod.hasEffect
import java.util.*
import kotlin.math.hypot

class ClientTickEvent {
    companion object{
        fun init() {
            HudRenderCallback.EVENT.register { context, _ ->
                val client = MinecraftClient.getInstance()
                val player = client.player ?: return@register
                if (player.hasEffect(JREffects.ESTRUS_EFFECT) || client.player!!.hasEffect(JREffects.ORGASM_EFFECT)) {
                    // 粉嫩粉嫩的
                    renderPinkGUI()
                    renderPinkOverlay(context)
                }
                if (player.hasEffect(JREffects.ORGASM_EFFECT)) {
                    // 抖起来！！！
                    val intensity = client.window.scaledWidth * (0.00f + Random().nextFloat() * 0.005f)
                    applyScreenShake(context, intensity)
                    applyViewShake(client.player!!, intensity)
                }
                if (player.hasEffect(JREffects.FAINT_EFFECT)) {
                    // 晕了
                    renderFaintEffect(context)
                } else {
                    faintAlpha = 0f
                }
                if (player.electricShock > 0) {
                    val time = client.world!!.time

                    // 轻微屏幕抖动
                    if (time % 3L == 0L) {
                        val intensity = client.window.scaledWidth * (0.0008f + Random().nextFloat() * 0.0012f)
                        applyScreenShake(context, intensity)
                        applyViewShake(player, intensity * 0.4f)
                    }

                    // 残影
                    renderAfterimageWithNoise(context, time)

                    // 渲染波纹（会同步触发闪光）
                    renderElectricRipple(context, time)

                    // 渐入渐出闪光
                    flashAlpha = if (flashAlpha < targetFlashAlpha) {
                        (flashAlpha + 0.005f).coerceAtMost(targetFlashAlpha)
                    } else {
                        (flashAlpha - 0.004f).coerceAtLeast(0f)
                    }
                    if (flashAlpha > 0f) {
                        renderSoftFlash(context, flashAlpha)
                    }
                }


                // 渲染体力条
                renderPowerBar(context)

                renderSexText(context)

                renderCataractOverlay(context)
            }

            ClientTickEvents.END_CLIENT_TICK.register {
                updateCataractShader()
            }

        }




        private var flashAlpha = 0f
        private var targetFlashAlpha = 0f
        // 淡蓝色柔光闪
        private fun renderSoftFlash(context: DrawContext, alpha: Float) {
            RenderSystem.enableBlend()
            RenderSystem.defaultBlendFunc()
            RenderSystem.setShaderTexture(0, RIPPLE_TEXTURE) // 用波纹贴图
            RenderSystem.setShaderColor(0.8f, 0.95f, 1f, alpha) // 柔和蓝色

            val client = MinecraftClient.getInstance()
            val width = client.window.scaledWidth
            val height = client.window.scaledHeight

            // 放大到覆盖全屏
            val size = (hypot(width.toDouble(), height.toDouble()) * 2).toInt()
            val x = width / 2 - size / 2
            val y = height / 2 - size / 2

            context.drawTexture(RIPPLE_TEXTURE, x, y, 0f, 0f, size, size, size, size)

            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
            RenderSystem.disableBlend()
        }


        // 残影 + 噪声干扰
        private fun renderAfterimageWithNoise(context: DrawContext, time: Long) {
            val client = MinecraftClient.getInstance()
            val width = client.window.scaledWidth
            val height = client.window.scaledHeight

            // 残影透明度随时间衰减
            val alpha = 0.1f + 0.05f * kotlin.math.sin(time / 2.0).toFloat()

            RenderSystem.enableBlend()
            RenderSystem.defaultBlendFunc()

            // 轻微蓝色调
            RenderSystem.setShaderColor(0.8f, 0.9f, 1f, alpha)

            // 噪声纹理
            val noiseTex = Identifier.of("minecraft", "textures/block/obsidian.png")
            RenderSystem.setShaderTexture(0, noiseTex)

            val offsetX = (time % 8) / 8f
            val offsetY = ((time * 2) % 8) / 8f

            context.drawTexture(
                noiseTex,
                0, 0,
                offsetX * width,
                offsetY * height,
                width, height,
                width, height
            )

            RenderSystem.disableBlend()
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        }

        // 电流波纹（淡蓝色）
        private val RIPPLE_TEXTURE = rodId("textures/misc/circle_ripple.png")
        private fun renderElectricRipple(context: DrawContext, time: Long) {
            val client = MinecraftClient.getInstance()
            val width = client.window.scaledWidth
            val height = client.window.scaledHeight

            RenderSystem.enableBlend()
            RenderSystem.defaultBlendFunc()
            RenderSystem.setShaderTexture(0, RIPPLE_TEXTURE)

            val rippleCount = 2
            val rippleMaxRadius = (hypot(width.toDouble(), height.toDouble()) / 2).toFloat()
            val rippleSpeed = 1.5f

            for (i in 0 until rippleCount) {
                val progress = ((time * rippleSpeed + i * (rippleMaxRadius / rippleCount)) % rippleMaxRadius) / rippleMaxRadius
                val radius = rippleMaxRadius * progress

                // 当第一圈波纹刚开始时触发闪光
                if (i == 0 && progress < 0.02f) {
                    targetFlashAlpha = 0.08f + Random().nextFloat() * 0.08f
                }

                val alpha = (1f - progress).coerceIn(0f, 1f) * 0.25f
                RenderSystem.setShaderColor(0.6f, 0.9f, 1f, alpha)

                val size = (radius * 2).toInt().coerceAtLeast(width.coerceAtLeast(height))
                val x = width / 2 - size / 2
                val y = height / 2 - size / 2

                context.drawTexture(RIPPLE_TEXTURE, x, y, 0f, 0f, size, size, size, size)
            }

            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
            RenderSystem.disableBlend()
        }




        private fun renderSexText(context: DrawContext) {
            val client = MinecraftClient.getInstance()
            if (client.options.hudHidden) return

            val isFemale = client.player!!.isFemale
            val isMale = client.player!!.isMale

            // 获取屏幕尺寸
            val screenWidth = context.scaledWindowWidth

            // 设置文字大小（通过矩阵缩放）
            val scale = 2.0f // 2倍大小
            context.matrices.push()
            context.matrices.scale(scale, scale, 1.0f)

            // 计算基准宽度
            val scaledScreenWidth = screenWidth / scale
            val textRenderer = client.textRenderer

            // 符号和颜色准备
            val femaleSymbol = Text.literal("♀")
            val maleSymbol = Text.literal("♂")

            val pinkColor = 0xFFC0CB // 粉色
            val blueColor = 0x0000FF // 蓝色

            // 先定义绘制起始x位置
            var x = scaledScreenWidth - 10 // 右边距10像素，后面会往左推

            // 如果同时是男性和女性，两个符号都显示，女性符号在右，男性符号在左
            if (isFemale && isMale) {
                val femaleWidth = textRenderer.getWidth(femaleSymbol)
                val maleWidth = textRenderer.getWidth(maleSymbol)

                // 先绘制女性符号（右边）
                context.drawTextWithShadow(textRenderer, femaleSymbol, (x - femaleWidth).toInt(), 10, pinkColor)

                // 再绘制男性符号，向左推一个女性符号宽度和间距（比如5像素）
                x -= (femaleWidth + 5)
                context.drawTextWithShadow(textRenderer, maleSymbol, (x - maleWidth).toInt(), 10, blueColor)
            } else if (isFemale) {
                val femaleWidth = textRenderer.getWidth(femaleSymbol)
                context.drawTextWithShadow(textRenderer, femaleSymbol, (x - femaleWidth).toInt(), 10, pinkColor)
            } else if (isMale) {
                val maleWidth = textRenderer.getWidth(maleSymbol)
                context.drawTextWithShadow(textRenderer, maleSymbol, (x - maleWidth).toInt(), 10, blueColor)
            }

            context.matrices.pop()
        }



        private val POWER_ICON = Identifier.of("textures/item/diamond_sword.png")
        private fun renderPowerBar(context: DrawContext) {
            val client = MinecraftClient.getInstance()
            if (client.options.hudHidden) return
            val player = client.player ?: return
            val power = player.power
            val maxPower = player.attributes?.getValue(JRAttributes.GENERIC_MAX_POWER) ?: return

            // 如果体力是满的，则隐藏
            if (power >= maxPower) return

            // 获取屏幕尺寸
            val height = context.scaledWindowHeight

            // 设置位置和尺寸
            val barWidth = 91
            val barHeight = 4
            val iconSize = 8
            val margin = 10
            val x = margin + iconSize + 2
            val y = height - margin - barHeight

            // 绘制钻石剑图标
            context.drawTexture(POWER_ICON, x - iconSize - 2, y - (iconSize - barHeight)/2, 0f, 0f, iconSize, iconSize, iconSize, iconSize)

            // 计算体力百分比
            val percent = power / maxPower
            val powerWidth = barWidth * percent

            // 绘制背景条
            context.fill(
                x, y,
                x + barWidth, y + barHeight,
                0x80 shl 24 // 半透明黑色背景
            )

            // 绘制渐变体力条（黄色调）
            for (i in 0 until powerWidth.toInt()) {
                val progress = i.toFloat() / barWidth
                // 渐变从暗黄色(255,200,0)到亮黄色(255,255,150)
                val red = 255 // 保持红色分量最大
                val green = (200 * (1 - progress) + 255 * progress).toInt()
                val blue = (0 * (1 - progress) + 150 * progress).toInt()
                val color = (0xFF shl 24) or (red shl 16) or (green shl 8) or blue

                context.fill(
                    x + i, y,
                    x + i + 1, y + barHeight,
                    color
                )
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



        // 高潮的时候全身会发抖... 不自觉的...
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
        /*
        说实话，咱做不出来那种晕倒后的效果，这里的话呢其实只是那种玩得太太太太多了的那种效果
         */
        private fun renderFaintEffect(context: DrawContext) {
            val client = MinecraftClient.getInstance()
            val width = client.window.scaledWidth
            val height = client.window.scaledHeight

            // 淡入黑屏效果：通过帧时间线性增加 alpha
            val maxAlpha = 0.9f // 接近全黑
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


        private val CATARACT_TEXTURE = rodId("textures/misc/cataract_overlay.png")

        private fun renderCataractOverlay(context: DrawContext) {
            val client = MinecraftClient.getInstance()
            val player = client.player ?: return

            // 获取同步过来的病情值 (0 ~ 100% 对应的 tick)
            val pregnant = player as? org.cneko.justarod.entity.Pregnant ?: return
            val progressTick = pregnant.cataract
            val maxSeverity = 20 * 60 * 20 * 10f // 10天满值

            // 计算病情进度 (0.0 ~ 1.0)
            val severity = (progressTick / maxSeverity).coerceIn(0f, 1f)

            if (severity < 0.1f) return // 初期不渲染，保证体验

            val width = client.window.scaledWidth
            val height = client.window.scaledHeight
            val world = client.world ?: return
            val pos = player.blockPos

            // --- 核心优化 1: 动态光感计算 ---
            // 获取当前位置亮度 (0~15)
            val lightLevel = world.getLightLevel(pos)

            // 基础不透明度：病情越重，底色越明显，但在暗处只有原来的 20%
            // 这样你在矿洞里或者家里不会觉得眼前全是白纸
            var alpha = severity * 0.2f

            // 眩光因子：光越强，雾越浓
            // 只有当亮度 > 8 时才开始计算眩光
            if (lightLevel > 8) {
                val glareIntensity = (lightLevel - 8) / 7f // 0.0 ~ 1.0
                // 眩光带来的额外不透明度，最高可达 0.6
                alpha += glareIntensity * severity * 0.6f
            }

            // 太阳直射惩罚：如果在白天且抬头看天
            if (world.isDay && player.pitch < -15f && world.isSkyVisible(pos)) {
                alpha += 0.3f * severity
            }

            // 最终限制：永远不要让屏幕完全变白 (保留 10% 可视度)，否则没法玩
            alpha = alpha.coerceIn(0f, 0.9f)

            // --- 核心优化 2: 渲染 ---
            RenderSystem.enableBlend()
            RenderSystem.defaultBlendFunc()

            // 设置颜色：乳黄色/米色 (1.0, 0.97, 0.92)
            // 这种颜色比纯白更护眼，也更有质感
            RenderSystem.setShaderColor(1.0f, 0.97f, 0.92f, alpha)

            // 使用 "细雪" 的贴图作为遮罩
            // 它的效果是四周有冰霜，中间相对清晰，正好符合我们想要的“边缘模糊”
            // 这样玩家看正中间的准星时不会太难受
            RenderSystem.setShaderTexture(0, CATARACT_TEXTURE)

            context.drawTexture(
                CATARACT_TEXTURE,
                0, 0, // x, y
                0f, 0f, // u, v
                width, height, // 渲染宽高
                width, height  // 纹理宽高
            )

            // 如果病情极其严重 (>80%)，再叠加一层极其淡的纯色，模拟整体视力下降
            if (severity > 0.8f) {
                // 非常淡的填充，仅用于降低对比度
                val fillAlpha = ((severity - 0.8f) * 0.5f).coerceAtMost(0.2f)
                val color = ( (fillAlpha * 255).toInt() shl 24 ) or 0xFFFDD0 // Cream color
                context.fill(0, 0, width, height, color)
            }

            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
            RenderSystem.disableBlend()
        }

        // 记录上一次的着色器状态，防止每帧重复加载导致卡顿
        private var isShaderLoaded = false
        private val BLUR_SHADER = Identifier.of("shaders/post/blur.json")

        private fun updateCataractShader() {
            val client = MinecraftClient.getInstance()
            val player = client.player ?: return
            val pregnant = player as? org.cneko.justarod.entity.Pregnant ?: return

            val cataract = pregnant.cataract
            val stage3 = 20 * 60 * 20 * 10 // 晚期

            // 只有在非常严重（晚期）时才开启真实模糊 Shader
            // 因为 Shader 会模糊掉 GUI 和血条，对游戏体验影响很大，所以建议只在晚期开启
            val shouldHaveBlur = cataract > stage3

            if (shouldHaveBlur) {
                if (!isShaderLoaded) {
                    // 加载模糊着色器
                    try {
                        client.gameRenderer.loadPostProcessor(BLUR_SHADER)
                        isShaderLoaded = true
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } else {
                // 如果病情好转，或者还没到严重程度，但Shader还开着 -> 关掉
                if (isShaderLoaded) {
                    client.gameRenderer.disablePostProcessor()
                    isShaderLoaded = false
                }
            }
        }

    }
}



