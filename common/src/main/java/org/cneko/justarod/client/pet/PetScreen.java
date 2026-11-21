package org.cneko.justarod.client.pet;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import org.cneko.justarod.packet.PetActionPayload;
import org.cneko.justarod.packet.PetMenuPayload;
import static org.cneko.justarod.packet.PetActionPayload.*;

/** The absurd trade is a consent dialog with a theatrical vanilla-style exchange row. */
public final class PetScreen extends Screen {
    private final PetMenuPayload menu;
    private boolean submitted;
    private int left, top, panelWidth;
    public PetScreen(PetMenuPayload menu) {
        super(Component.translatable("pet.justarod.screen." + menu.kind())); this.menu = menu;
    }
    private Component text(String key, Object... args) { return Component.translatable("pet.justarod." + key, args); }
    @Override protected void init() {
        panelWidth = Math.min(330, width - 20); left = (width - panelWidth) / 2; top = Math.max(6, (height - 226) / 2);
        if (menu.kind() == PetMenuPayload.OWNER) {
            String[] labels = {"sit", "stand", "follow", "cancel", "stroke", "feed", "transfer", "neko_follow", "release", "close"};
            int[] actions = {SIT, STAND, FOLLOW, CANCEL, STROKE, FEED, TRANSFER, NEKO_FOLLOW, RELEASE, DECLINE};
            int buttonWidth = (panelWidth - 30) / 2;
            for (int i = 0; i < labels.length; i++) {
                int action = actions[i];
                var b = addRenderableWidget(Button.builder(text(labels[i]), button -> submit(action))
                        .bounds(left + 10 + (i % 2) * (buttonWidth + 10), top + 91 + (i / 2) * 25, buttonWidth, 20).build());
                if (action == TRANSFER && menu.candidateName().isEmpty()) b.active = false;
                if (action == NEKO_FOLLOW && (minecraft == null || minecraft.level == null
                        || !(minecraft.level.getEntity(menu.npcId()) instanceof org.cneko.toneko.common.mod.entities.NekoEntity))) b.active = false;
            }
        } else {
            int action = menu.kind() == PetMenuPayload.OFFER ? ACCEPT : CONFIRM;
            addRenderableWidget(Button.builder(text(menu.kind() == PetMenuPayload.OFFER ? "accept_food" : "confirm_transfer"), b -> submit(action))
                    .bounds(left + 10, top + 150, panelWidth - 20, 20).build());
            addRenderableWidget(Button.builder(text("decline"), b -> submit(DECLINE))
                    .bounds(left + 10, top + 177, panelWidth - 20, 20).build());
        }
    }
    private void submit(int action) {
        if (submitted) return;
        submitted = true;
        PetClient.send(new PetActionPayload(action, menu.npcId(), menu.token()));
        if (minecraft != null) minecraft.setScreen(null);
    }
    @Override public void onClose() { submit(DECLINE); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.fill(left, top, left + panelWidth, top + 222, 0xED292631);
        g.fill(left, top, left + panelWidth, top + 3, 0xFFFFAACD);
        g.centeredText(font, title, width / 2, top + 12, 0xFFFFC3DC);
        g.centeredText(font, font.plainSubstrByWidth(menu.npcName(), panelWidth - 20), width / 2, top + 30, 0xFFFFFFFF);
        if (menu.kind() == PetMenuPayload.OWNER) {
            g.centeredText(font, text("menu_hint"), width / 2, top + 49, 0xFFD6CBD5);
            String candidate = menu.candidateName().isEmpty() ? text("no_candidate").getString()
                    : text("candidate", menu.candidateName()).getString();
            g.centeredText(font, font.plainSubstrByWidth(candidate, panelWidth - 20), width / 2, top + 67, 0xFFD6CBD5);
        } else if (menu.kind() == PetMenuPayload.OFFER) {
            g.centeredText(font, text("offer_body"), width / 2, top + 59, 0xFFD6CBD5);
            g.centeredText(font, text("offer_consent"), width / 2, top + 77, 0xFFD6CBD5);
            g.item(new ItemStack(Items.COD), width / 2 - 8, top + 104);
        } else {
            String target = text("candidate", menu.candidateName()).getString();
            g.centeredText(font, font.plainSubstrByWidth(target, panelWidth - 20), width / 2, top + 54, 0xFFD6CBD5);
            g.centeredText(font, text("transfer_consent"), width / 2, top + 73, 0xFFD6CBD5);
            if (menu.absurd()) {
                g.fill(left + 24, top + 94, left + panelWidth - 24, top + 137, 0xFF55515D);
                g.item(new ItemStack(Items.EMERALD), width / 2 - 73, top + 104);
                g.text(font, "3", width / 2 - 55, top + 113, 0xFFFFFFFF);
                g.centeredText(font, Component.literal("→"), width / 2, top + 108, 0xFFFFFFFF);
                var head = new ItemStack(Items.PLAYER_HEAD);
                if (minecraft != null && minecraft.player != null)
                    head.set(DataComponents.PROFILE, ResolvableProfile.createResolved(minecraft.player.getGameProfile()));
                g.item(head, width / 2 + 52, top + 104);
            }
        }
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }
}
