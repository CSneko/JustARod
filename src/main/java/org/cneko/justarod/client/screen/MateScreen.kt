package org.cneko.justarod.client.screen

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.AbstractSliderButton
import net.minecraft.network.chat.Component
import org.cneko.justarod.packet.MatePayload
import org.cneko.toneko.common.mod.client.screens.INekoScreen
import org.cneko.toneko.common.mod.entities.NekoEntity

// 交配... （只和自己交配过）（小声）
// 和自己交配不了算是什么 -NT
class MateScreen(val nekoEntity: NekoEntity) : Screen(Component.empty()), INekoScreen {
    private var amountSlider: AmountSliderWidget? = null
    private var timeSlider: TimeSliderWidget? = null
    private var doneButton: Button? = null

    // 存储滑块值
    private var amountValue: Double = 1.0
    private var timeValue = 10

    override fun init() {
        super.init()

        // 数量滑块 (1.0-5.0)
        amountSlider = AmountSliderWidget(
            width / 2 - 100, height / 2 - 30,
            200, 20,
            Component.translatable("gui.justarod.amount"),
            amountValue.toFloat()
        )
        addRenderableWidget(amountSlider)

        // 时间滑块 (10-60分钟)
        timeSlider = TimeSliderWidget(
            width / 2 - 100, height / 2,
            200, 20,
            Component.translatable("gui.justarod.time"),
            timeValue
        )
        addRenderableWidget(timeSlider)

        // 完成按钮
        doneButton = Button.builder(Component.translatable("gui.justarod.done")) {
            onDone()
        }.bounds(width / 2 - 100, height / 2 + 30, 200, 20).build()
        addRenderableWidget(doneButton)
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        // 渲染标题
        context.drawCenteredString(
            font,
            Component.translatable("gui.justarod.mate_settings"),
            width / 2, height / 2 - 60,
            0xFFFFFF
        )

        // 渲染滑块提示文字
        context.drawString(
            font,
            Component.translatable("gui.justarod.amount_hint", "%.1f".format(amountValue)),
            width / 2 + 110, height / 2 - 25,
            0xAAAAAA
        )

        context.drawString(
            font,
            Component.translatable("gui.justarod.time_hint", timeValue),
            width / 2 + 110, height / 2 + 5,
            0xAAAAAA
        )

        super.render(context, mouseX, mouseY, delta)
    }

    override fun isPauseScreen(): Boolean {
        return false
    }

    override fun renderBackground(context: GuiGraphics?, mouseX: Int, mouseY: Int, delta: Float) {
        // 不渲染背景
    }

    private fun onDone() {
        ClientPlayNetworking.send(MatePayload(nekoEntity.uuid.toString(), amountValue, timeValue))
        onClose()
    }

    override fun getNeko(): NekoEntity? {
        return nekoEntity
    }

    // 自定义数量滑块
    private inner class AmountSliderWidget(
        x: Int, y: Int, width: Int, height: Int, text: Component, value: Float
    ) : AbstractSliderButton(x, y, width, height, text, ((value - 1.0f) / 4.0f).toDouble()) {

        override fun updateMessage() {
            message = Component.translatable("gui.justarod.amount", "%.1f".format(value * 4 + 1.0f))
        }

        override fun applyValue() {
            amountValue = value * 4 + 1.0f
        }
    }

    // 自定义时间滑块
    private inner class TimeSliderWidget(
        x: Int, y: Int, width: Int, height: Int, text: Component, value: Int
    ) : AbstractSliderButton(x, y, width, height, text, ((value - 10) / 50.0f).toDouble()) {

        override fun updateMessage() {
            val minutes = (value * 50 + 10).toInt()
            message = Component.translatable("gui.justarod.time", minutes)
        }

        override fun applyValue() {
            timeValue = (value * 50 + 10).toInt()
        }
    }
}