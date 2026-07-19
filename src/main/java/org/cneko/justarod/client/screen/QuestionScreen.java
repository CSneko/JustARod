package org.cneko.justarod.client.screen;

import org.cneko.toneko.common.api.TickTasks;
import org.cneko.toneko.common.mod.util.ITickable;
import org.cneko.toneko.common.mod.util.TickTaskQueue;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public class QuestionScreen extends Screen {
    private Question question;
    private final Runnable onRight;
    private final Runnable onWrong;
    private final QuestionSupplier onSwitch;
    private static final int buttonWidth = 200;
    private static final int buttonHeight = 20;
    private static final int padding = 10;
    private static final int switchButtonWidth = 60;
    private static final int switchButtonHeight = 20;
    private static final int switchButtonPadding = 5;

    public QuestionScreen(Question question, Runnable onRight, Runnable onWrong, QuestionSupplier onSwitch) {
        super(Component.empty());
        this.question = question;
        this.onRight = onRight;
        this.onWrong = onWrong;
        this.onSwitch = onSwitch;
    }

    @Override
    protected void init() {
        super.init();

        this.addRenderableWidget(Button.builder(Component.nullToEmpty("换一个"), button -> switchQuestion())
                .bounds(this.width - switchButtonWidth - switchButtonPadding, switchButtonPadding, switchButtonWidth, switchButtonHeight)
                .build());

        int centerX = this.width / 2;
        int questionBottom = this.height / 3;
        int buttonYStart = questionBottom + 30;

        this.addRenderableWidget(Button.builder(question.option1(), button -> checkAnswer(1))
                .bounds(centerX - buttonWidth / 2, buttonYStart, buttonWidth, buttonHeight)
                .build());

        this.addRenderableWidget(Button.builder(question.option2(), button -> checkAnswer(2))
                .bounds(centerX - buttonWidth / 2, buttonYStart + buttonHeight + padding, buttonWidth, buttonHeight)
                .build());

        this.addRenderableWidget(Button.builder(question.option3(), button -> checkAnswer(3))
                .bounds(centerX - buttonWidth / 2, buttonYStart + 2 * (buttonHeight + padding), buttonWidth, buttonHeight)
                .build());

        this.addRenderableWidget(Button.builder(question.option4(), button -> checkAnswer(4))
                .bounds(centerX - buttonWidth / 2, buttonYStart + 3 * (buttonHeight + padding), buttonWidth, buttonHeight)
                .build());
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        // 获取屏幕宽度减去一些边距
        int maxWidth = this.width - 40;

        // 将问题文本分割成多行
        List<FormattedCharSequence> wrappedLines = this.font.split(question.question().copy().withStyle(ChatFormatting.BOLD), maxWidth);

        // 计算起始Y位置
        int startY = this.height / 4;

        // 绘制每一行文本
        for (int i = 0; i < wrappedLines.size(); i++) {
            context.drawCenteredString(
                    this.font,
                    wrappedLines.get(i),
                    this.width / 2,
                    startY + (i * this.font.lineHeight),
                    0xFFFFFF
            );
        }
    }

    private void checkAnswer(int answerOption) {
        onClose();
        if (question.checkAnswer(answerOption)) {
            this.onRight.run();
        } else {
            this.onWrong.run();
        }
    }

    private void switchQuestion() {
        this.question = onSwitch.get();
        this.rebuildWidgets();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    public record Question(Component question, int rightAnswerOption, Component option1, Component option2, Component option3, Component option4) {
        public boolean checkAnswer(int answerOption) {
            return answerOption == rightAnswerOption;
        }
    }

    @FunctionalInterface
    public interface QuestionSupplier {
        Question get();
    }
}