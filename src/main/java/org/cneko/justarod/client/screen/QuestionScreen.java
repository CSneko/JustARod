package org.cneko.justarod.client.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.cneko.toneko.common.api.TickTasks;
import org.cneko.toneko.common.mod.util.ITickable;
import org.cneko.toneko.common.mod.util.TickTaskQueue;

import java.util.List;

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
        super(Text.empty());
        this.question = question;
        this.onRight = onRight;
        this.onWrong = onWrong;
        this.onSwitch = onSwitch;
    }

    @Override
    protected void init() {
        super.init();

        this.addDrawableChild(ButtonWidget.builder(Text.of("换一个"), button -> switchQuestion())
                .dimensions(this.width - switchButtonWidth - switchButtonPadding, switchButtonPadding, switchButtonWidth, switchButtonHeight)
                .build());

        int centerX = this.width / 2;
        int questionBottom = this.height / 3;
        int buttonYStart = questionBottom + 30;

        this.addDrawableChild(ButtonWidget.builder(question.option1(), button -> checkAnswer(1))
                .dimensions(centerX - buttonWidth / 2, buttonYStart, buttonWidth, buttonHeight)
                .build());

        this.addDrawableChild(ButtonWidget.builder(question.option2(), button -> checkAnswer(2))
                .dimensions(centerX - buttonWidth / 2, buttonYStart + buttonHeight + padding, buttonWidth, buttonHeight)
                .build());

        this.addDrawableChild(ButtonWidget.builder(question.option3(), button -> checkAnswer(3))
                .dimensions(centerX - buttonWidth / 2, buttonYStart + 2 * (buttonHeight + padding), buttonWidth, buttonHeight)
                .build());

        this.addDrawableChild(ButtonWidget.builder(question.option4(), button -> checkAnswer(4))
                .dimensions(centerX - buttonWidth / 2, buttonYStart + 3 * (buttonHeight + padding), buttonWidth, buttonHeight)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        // 获取屏幕宽度减去一些边距
        int maxWidth = this.width - 40;

        // 将问题文本分割成多行
        List<OrderedText> wrappedLines = this.textRenderer.wrapLines(question.question().copy().formatted(Formatting.BOLD), maxWidth);

        // 计算起始Y位置
        int startY = this.height / 4;

        // 绘制每一行文本
        for (int i = 0; i < wrappedLines.size(); i++) {
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    wrappedLines.get(i),
                    this.width / 2,
                    startY + (i * this.textRenderer.fontHeight),
                    0xFFFFFF
            );
        }
    }

    private void checkAnswer(int answerOption) {
        close();
        if (question.checkAnswer(answerOption)) {
            this.onRight.run();
        } else {
            this.onWrong.run();
        }
    }

    private void switchQuestion() {
        this.question = onSwitch.get();
        this.clearAndInit();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    public record Question(Text question, int rightAnswerOption, Text option1, Text option2, Text option3, Text option4) {
        public boolean checkAnswer(int answerOption) {
            return answerOption == rightAnswerOption;
        }
    }

    @FunctionalInterface
    public interface QuestionSupplier {
        Question get();
    }
}