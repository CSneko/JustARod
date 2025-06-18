package org.cneko.justarod.client.screen

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.SliderWidget
import net.minecraft.text.Text
import org.cneko.justarod.packet.MatePayload
import org.cneko.toneko.common.mod.client.screens.INekoScreen
import org.cneko.toneko.common.mod.entities.NekoEntity

// 交配... （只和自己交配过）（小声）
class MateScreen(val nekoEntity: NekoEntity) : Screen(Text.empty()), INekoScreen {
    private var amountSlider: AmountSliderWidget? = null
    private var timeSlider: TimeSliderWidget? = null
    private var doneButton: ButtonWidget? = null

    // 存储滑块值
    private var amountValue: Double = 1.0
    private var timeValue = 10

    override fun init() {
        super.init()

        // 数量滑块 (1.0-5.0)
        amountSlider = AmountSliderWidget(
            width / 2 - 100, height / 2 - 30,
            200, 20,
            Text.translatable("gui.justarod.amount"),
            amountValue.toFloat()
        )
        addDrawableChild(amountSlider)

        // 时间滑块 (10-60分钟)
        timeSlider = TimeSliderWidget(
            width / 2 - 100, height / 2,
            200, 20,
            Text.translatable("gui.justarod.time"),
            timeValue
        )
        addDrawableChild(timeSlider)

        // 完成按钮
        doneButton = ButtonWidget.builder(Text.translatable("gui.justarod.done")) {
            onDone()
        }.dimensions(width / 2 - 100, height / 2 + 30, 200, 20).build()
        addDrawableChild(doneButton)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // 渲染标题
        context.drawCenteredTextWithShadow(
            textRenderer,
            Text.translatable("gui.justarod.mate_settings"),
            width / 2, height / 2 - 60,
            0xFFFFFF
        )

        // 渲染滑块提示文字
        context.drawTextWithShadow(
            textRenderer,
            Text.translatable("gui.justarod.amount_hint", "%.1f".format(amountValue)),
            width / 2 + 110, height / 2 - 25,
            0xAAAAAA
        )

        context.drawTextWithShadow(
            textRenderer,
            Text.translatable("gui.justarod.time_hint", timeValue),
            width / 2 + 110, height / 2 + 5,
            0xAAAAAA
        )

        super.render(context, mouseX, mouseY, delta)
    }

    override fun shouldPause(): Boolean {
        return false
    }

    override fun renderBackground(context: DrawContext?, mouseX: Int, mouseY: Int, delta: Float) {
        // 不渲染背景
    }

    private fun onDone() {
        ClientPlayNetworking.send(MatePayload(nekoEntity.uuid.toString(), amountValue, timeValue))
        close()
    }

    override fun getNeko(): NekoEntity? {
        return nekoEntity
    }

    // 自定义数量滑块
    private inner class AmountSliderWidget(
        x: Int, y: Int, width: Int, height: Int, text: Text, value: Float
    ) : SliderWidget(x, y, width, height, text, ((value - 1.0f) / 4.0f).toDouble()) {

        override fun updateMessage() {
            message = Text.translatable("gui.justarod.amount", "%.1f".format(value * 4 + 1.0f))
        }

        override fun applyValue() {
            amountValue = value * 4 + 1.0f
        }
    }

    // 自定义时间滑块
    private inner class TimeSliderWidget(
        x: Int, y: Int, width: Int, height: Int, text: Text, value: Int
    ) : SliderWidget(x, y, width, height, text, ((value - 10) / 50.0f).toDouble()) {

        override fun updateMessage() {
            val minutes = (value * 50 + 10).toInt()
            message = Text.translatable("gui.justarod.time", minutes)
        }

        override fun applyValue() {
            timeValue = (value * 50 + 10).toInt()
        }
    }
}