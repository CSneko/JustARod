package org.cneko.justarod.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import org.cneko.justarod.client.gui.JRConfigScreen;
import org.cneko.justarod.config.ConfigBuilder;
import org.cneko.justarod.config.JRConfig;
import org.cneko.justarod.config.JsonConfiguration;

import java.nio.file.Files;
import java.nio.file.Path;

/** Actual numeric text-box edits, Apply, UI rebuilding, file reload and old string-number migration. */
public class ConfigClientGameTests implements FabricClientGameTest {
    private static final String BPM = "fun.entity_squash.bpm";
    private static final String RATIO = "fun.entity_squash.compression_ratio";
    private static final String DAYS = "pregnancy.duration_days";

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        try {
            verifyLegacyConfig();
            context.runOnClient(ConfigClientGameTests::verifyScreen);
        } catch (Exception exception) {
            throw new RuntimeException("Configuration persistence regression failed", exception);
        }
    }

    private static void verifyLegacyConfig() throws Exception {
        Path path = Files.createTempFile("justarod-config-numbers-", ".json");
        try {
            Files.writeString(path, "{\"tempo\":\"180.5\",\"days\":\"9.0\",\"note\":\"123\"}");
            ConfigBuilder builder = new ConfigBuilder(path)
                    .addFloat("tempo", 120f, null, "Tempo")
                    .addInt("days", 5, null, "Days")
                    .addString("note", "text", null, "Note");
            check(((Number) builder.getExist("tempo").value).floatValue() == 180.5f,
                    "Old numeric strings must be readable before build");
            builder.build();
            JsonConfiguration saved = new JsonConfiguration(path);
            check(saved.getJsonPrimitive("tempo").isNumber() && saved.getFloat("tempo") == 180.5f,
                    "Build migrates old decimal strings without losing edits");
            check(saved.getJsonPrimitive("days").isNumber() && saved.getInt("days") == 9,
                    "Build migrates integer-valued decimal strings");
            check(saved.getJsonPrimitive("note").isString(), "Declared strings keep their type");
            check(!builder.setFromText("days", "9.5"), "Fractional integer edits are rejected");
            check(!builder.setFromText("days", "2147483648"), "Integer overflow is rejected");
            check(!builder.setFromText("tempo", "1e100"), "Floating-point overflow is rejected");
            check(((Number) builder.getExist("days").value).intValue() == 9,
                    "Rejected edits preserve the last valid integer");
        } finally {
            Files.deleteIfExists(path);
        }
    }

    private static void verifyScreen(Minecraft minecraft) throws Exception {
        var config = JRConfig.CONFIG_BUILDER.getConfig();
        Path path = Path.of(JRConfig.CONFIG_FILE);
        byte[] oldFile = Files.exists(path) ? Files.readAllBytes(path) : null;
        Object oldBpm = config.get(BPM);
        Object oldRatio = config.get(RATIO);
        Object oldDays = config.get(DAYS);
        Screen previousScreen = minecraft.screen;
        try {
            JRConfigScreen screen = new JRConfigScreen(previousScreen);
            minecraft.setScreen(screen);
            typeNumber(findInput(screen, BPM), "240.5");
            typeNumber(findInput(screen, RATIO), "0.35");
            typeNumber(findInput(screen, DAYS), "8");
            check(config.getJsonPrimitive(BPM).isNumber() && JRConfig.getEntitySquashBpm() == 240.5f,
                    "Typed decimal BPM must be stored as a number and immediately used");
            check(config.getJsonPrimitive(DAYS).isNumber() && JRConfig.getPregnancyDurationDays() == 8,
                    "Typed integer must be stored as a number and immediately used");

            EditBox ratio = findInput(screen, RATIO);
            for (String invalid : new String[]{"", "-", ".", "NaN", "Infinity", "abc"}) {
                ratio.setValue(invalid);
                check(JRConfig.getEntitySquashCompressionRatio() == 0.35f,
                        "Incomplete/invalid input preserves the last valid decimal: " + invalid);
            }
            ratio.setValue("0.35");
            findInput(screen, DAYS).setValue("8.5");
            check(JRConfig.getPregnancyDurationDays() == 8, "Fractional input must not truncate an integer");
            findInput(screen, DAYS).setValue("8");

            // Collapse and expand the first group, rebuilding all rows from the current values.
            AbstractWidget list = (AbstractWidget) screen.children().getFirst();
            MouseButtonEvent headerClick = new MouseButtonEvent(list.getX() + 8, list.getY() + 10,
                    new MouseButtonInfo(0, 0));
            check(screen.mouseClicked(headerClick, false), "Pregnancy group can collapse");
            check(screen.mouseClicked(headerClick, false), "Pregnancy group can expand");
            check(Float.parseFloat(findInput(screen, BPM).getValue()) == 240.5f,
                    "Rebuilding rows must display the current BPM");
            check(Integer.parseInt(findInput(screen, DAYS).getValue()) == 8,
                    "Rebuilding rows must display the current integer");

            Button apply = screen.children().stream()
                    .filter(child -> child instanceof Button button && button.getMessage().getString().equals(
                            Component.translatable("screen.justarod.config.button.apply").getString()))
                    .map(child -> (Button) child).findFirst().orElseThrow();
            check(screen.mouseClicked(new MouseButtonEvent(apply.getX() + 5, apply.getY() + 5,
                    new MouseButtonInfo(0, 0)), false), "Apply can be clicked");
            JsonConfiguration saved = new JsonConfiguration(path);
            check(saved.getJsonPrimitive(BPM).isNumber() && saved.getFloat(BPM) == 240.5f,
                    "Apply persists decimal BPM as a JSON number");
            check(saved.getJsonPrimitive(RATIO).isNumber() && saved.getFloat(RATIO) == 0.35f,
                    "Apply persists the compression ratio");
            check(saved.getJsonPrimitive(DAYS).isNumber() && saved.getInt(DAYS) == 8,
                    "Apply persists integer edits");

            var reloaded = new ConfigBuilder(path)
                    .addFloat(BPM, 120f, null, "Tempo").addInt(DAYS, 5, null, "Days").build();
            check(((Number) reloaded.getExist(BPM).value).floatValue() == 240.5f
                            && ((Number) reloaded.getExist(DAYS).value).intValue() == 8,
                    "A fresh configuration instance retains saved numeric edits");
            JRConfigScreen reopened = new JRConfigScreen(previousScreen);
            minecraft.setScreen(reopened);
            check(Float.parseFloat(findInput(reopened, RATIO).getValue()) == 0.35f,
                    "Reopened screen displays the saved ratio instead of the default");
            reopened.resize(reopened.width, reopened.height);
            check(Float.parseFloat(findInput(reopened, BPM).getValue()) == 240.5f,
                    "Window resize preserves the displayed numeric setting");
        } finally {
            config.set(BPM, oldBpm);
            config.set(RATIO, oldRatio);
            config.set(DAYS, oldDays);
            if (oldFile != null) Files.write(path, oldFile);
            else Files.deleteIfExists(path);
            minecraft.setScreen(previousScreen);
        }
    }

    private static EditBox findInput(ContainerEventHandler container, String key) {
        for (var child : container.children()) {
            if (child instanceof EditBox box && box.getMessage().getString().equals(key)) return box;
            if (child instanceof ContainerEventHandler nested) {
                EditBox box = findInput(nested, key);
                if (box != null) return box;
            }
        }
        return null;
    }

    private static void typeNumber(EditBox box, String text) {
        check(box != null, "Numeric input must exist");
        box.setFocused(true);
        box.setValue("");
        for (int codepoint : text.codePoints().toArray()) {
            check(box.charTyped(new CharacterEvent(codepoint)), "Numeric character must reach the input widget");
        }
    }
}
