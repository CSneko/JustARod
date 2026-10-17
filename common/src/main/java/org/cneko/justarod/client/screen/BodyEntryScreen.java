package org.cneko.justarod.client.screen;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.cneko.justarod.body.BodyWorld;
import org.cneko.justarod.packet.BodyTravelPayload;
import java.util.UUID;

public final class BodyEntryScreen extends Screen {
    private final UUID target;
    private final Component hostName;

    public BodyEntryScreen(UUID target, Component hostName) {
        super(Component.translatable("body.entry.title"));
        this.target = target;
        this.hostName = hostName;
    }

    @Override
    protected void init() {
        int left = width / 2 - 100;
        int top = height / 2 - 55;
        addRenderableWidget(Button.builder(Component.translatable("body.entry.mouth"), b -> choose(BodyWorld.MOUTH))
                .bounds(left, top, 200, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("body.entry.vagina"), b -> choose(BodyWorld.VAGINA))
                .bounds(left, top + 26, 200, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("body.entry.anus"), b -> choose(BodyWorld.ANUS))
                .bounds(left, top + 52, 200, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(left, top + 84, 200, 20).build());
    }

    private void choose(int route) {
        ClientPlayNetworking.send(new BodyTravelPayload(target, route));
        onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(font, Component.translatable("body.entry.prompt", hostName), width / 2,
                height / 2 - 85, 0xFFFFFFFF);
    }
}
