package org.cneko.justarod.client.screen

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.text.Text
import org.cneko.justarod.packet.FullHeatPayload
import kotlin.math.abs
import kotlin.random.Random
import kotlin.math.min
import kotlin.math.max

// 好奇芦管是什么感觉
class FrictionScreen : Screen(Text.empty()) {
    private var heat = 0.0f
    private var sliderPosition = 0.5f // 初始位置在中间
    private var sliderDragging = false
    private val particles = mutableListOf<Particle>()
    private var lastSliderPosition = 0.5f // 记录上次滑块位置

    // 常量
    private val maxHeat =3000f
    private val heatWarningThreshold = 0.7f*maxHeat // 粒子开始生成的阈值
    private val shakeThreshold = 0.8f*maxHeat // 屏幕开始抖动的阈值

    // 屏幕抖动相关
    private var shakeOffsetX = 0f
    private var shakeOffsetY = 0f
    private var lastShakeTime = 0L

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // 更新热力值
        updateHeat(delta)

        // 更新粒子
        updateParticles(delta)

        // 应用屏幕抖动
        val matrices = context.matrices
        matrices.push()
        if (heat > shakeThreshold) {
            val shakeIntensity = (heat - shakeThreshold) / (maxHeat - shakeThreshold) // 0-1之间的强度
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastShakeTime > 50) { // 每50ms更新一次抖动
                shakeOffsetX = Random.nextFloat() * 10f * shakeIntensity - 5f * shakeIntensity
                shakeOffsetY = Random.nextFloat() * 10f * shakeIntensity - 5f * shakeIntensity
                lastShakeTime = currentTime
            }
            matrices.translate(shakeOffsetX, shakeOffsetY, 0f)
        } else {
            shakeOffsetX = 0f
            shakeOffsetY = 0f
        }

        val width = this.width
        val height = this.height

        // 绘制热力条背景
        val barWidth = width * 0.8f
        val barHeight = 20f
        val barX = (width - barWidth) / 2
        val barY = height * 0.3f

        context.fill(barX.toInt(), barY.toInt(), (barX + barWidth).toInt(), (barY + barHeight).toInt(), 0xFF333333.toInt())

        // 绘制热力条（颜色从红到白渐变）
        val heatWidth = barWidth * (heat / maxHeat)
        for (i in 0 until heatWidth.toInt()) {
            val progress = i / barWidth
            val red = 255
            val green = (255 * progress).toInt()
            val blue = (255 * progress).toInt()
            val color = (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
            context.fill((barX + i).toInt(), barY.toInt(), (barX + i + 1).toInt(), (barY + barHeight).toInt(), color)
        }

        // 绘制滑动条
        val baseSliderWidth = 100f
        val sliderWidth = if (heat > maxHeat * 0.5f) {
            val progress = (heat - 0.5f * maxHeat) / (0.5f * maxHeat) // 50%-100% 范围内映射为 0-1
            baseSliderWidth * (1f + progress) // 从1倍到2倍
        } else {
            baseSliderWidth
        }

        val sliderHeight = 20f
        val sliderX = (width - sliderWidth) / 2
        val sliderY = height * 0.5f
        val sliderThumbX = sliderX + sliderPosition * sliderWidth

        // 滑动条轨道
        context.fill(sliderX.toInt(), (sliderY + sliderHeight / 2 - 2).toInt(),
            (sliderX + sliderWidth).toInt(), (sliderY + sliderHeight / 2 + 2).toInt(), 0xFFAAAAAA.toInt())

        // 使用纸张物品渲染滑块
        val stack = ItemStack(Items.PAPER)
        val itemX = (sliderThumbX - 8).toInt()
        val itemY = (sliderY - 4).toInt()
        context.drawItem(stack, itemX, itemY)


        // 绘制粒子
        for (particle in particles) {
            context.fill(particle.x.toInt(), particle.y.toInt(),
                (particle.x + particle.size).toInt(), (particle.y + particle.size).toInt(), 0xFFFFFFFF.toInt())
        }

        matrices.pop()

        super.render(context, mouseX, mouseY, delta)
    }

    private fun updateHeat(delta: Float) {
        // 如果大于95%，则晕倒
        if (heat > 0.95f * maxHeat) {
            ClientPlayNetworking.send(FullHeatPayload("full"))
            MinecraftClient.getInstance().setScreen(null)
        }
        // 计算滑块位置的变化量
        val positionChange = abs(sliderPosition - lastSliderPosition)

        // 只有当位置有明显变化时才加热
        if (sliderDragging && positionChange > 0.001f) {
            val friction = (abs(sliderPosition - 0.5f) * 2 + positionChange * 10) // 摩擦值
            heat += friction * delta * 5f // 热力增加
        }

        // 记录当前滑块位置用于下次计算
        lastSliderPosition = sliderPosition

        // 限制热力值范围
        heat = max(0f, min(maxHeat, heat))

        // 自然冷却
        val coolingRate = if (heat > heatWarningThreshold) 0.0005f*maxHeat else 0.0002f*maxHeat
        heat = max(0f, heat - coolingRate * delta)
    }


    private fun updateParticles(delta: Float) {
        if (heat > heatWarningThreshold) {
            // 添加新粒子
            val spawnChance = ((heat - heatWarningThreshold) / (maxHeat - heatWarningThreshold)) * delta * 200
            val particlesToAdd = (spawnChance * 3).toInt()

            repeat(particlesToAdd) {
                val barWidth = width * 0.8f
                val barX = (width - barWidth) / 2
                val barY = height * 0.3f

                // 随机决定粒子是上升还是下降
                val isRising = Random.nextBoolean()
                val baseSpeedY = if (isRising) -50f else 30f

                particles.add(Particle(
                    x = barX + barWidth * (heat / maxHeat),
                    y = barY + Random.nextFloat() * 20f,
                    size = 1f + Random.nextFloat() * 4f,
                    speedX = 50f + Random.nextFloat() * 150f,
                    speedY = baseSpeedY + Random.nextFloat() * (if (isRising) 40f else 30f), // 上升或下降趋势
                    life = 0.5f + Random.nextFloat() * 3f,
                    isRising = isRising
                ))
            }
        }

        // 更新现有粒子
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val particle = iterator.next()
            particle.x += particle.speedX * delta
            particle.y += particle.speedY * delta
            particle.life -= delta

            // 根据粒子类型调整运动（上升或下降）
            if (particle.isRising) {
                particle.speedY -= 10f * delta // 上升粒子会减速
            } else {
                particle.speedY += 10f * delta // 下降粒子会加速
            }

            if (particle.life <= 0 || particle.x > width || particle.y > height || particle.y < 0) {
                iterator.remove()
            }
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val sliderWidth = 100f
        val sliderHeight = 20f
        val sliderX = (width - sliderWidth) / 2
        val sliderY = height * 0.5f

        if (mouseX >= sliderX && mouseX <= sliderX + sliderWidth &&
            mouseY >= sliderY && mouseY <= sliderY + sliderHeight) {
            sliderDragging = true
            updateSliderPosition(mouseX.toFloat())
            return true
        }

        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        sliderDragging = false
        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        if (sliderDragging) {
            updateSliderPosition(mouseX.toFloat())
            return true
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
    }

    private fun updateSliderPosition(mouseX: Float) {
        val sliderWidth = 100f
        val sliderX = (width - sliderWidth) / 2
        sliderPosition = ((mouseX - sliderX) / sliderWidth).coerceIn(0f, 1f)
    }

    override fun shouldPause(): Boolean {
        return false
    }

    override fun renderBackground(context: DrawContext?, mouseX: Int, mouseY: Int, delta: Float) {
    }

    private data class Particle(
        var x: Float,
        var y: Float,
        val size: Float,
        val speedX: Float,
        var speedY: Float,
        var life: Float,
        val isRising: Boolean
    )
}