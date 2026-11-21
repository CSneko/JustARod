package org.cneko.justarod.client.disassembly;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.cneko.justarod.packet.DisassemblyActionPayload;

/** A view from inside a pocket: live icons, no inventory interaction or transfer packets. */
public final class PocketViewScreen extends Screen {
    private int left,top;
    public PocketViewScreen() { super(Component.translatable("disassembly.justarod.pocket_title")); }
    @Override protected void init() {
        left=(width-198)/2; top=Math.max(3,(height-205)/2);
        addRenderableWidget(Button.builder(Component.translatable("disassembly.justarod.exit_view"),b->onClose())
                .bounds(left+10,top+175,178,20).build());
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { DisassemblyClient.send(DisassemblyActionPayload.EXIT,0); super.onClose(); }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float partialTick) {
        g.fill(0,0,width,height,0xF0201827);
        g.fill(left,top,left+198,top+203,0xFF3B2B40); g.fill(left,top,left+198,top+3,0xFFFFB4D4);
        g.centeredText(font,Component.translatable("disassembly.justarod.pocket_owner",DisassemblyClient.view().carrier()),width/2,top+13,0xFFFFCADD);
        g.centeredText(font,Component.translatable("disassembly.justarod.pocket_hint"),width/2,top+28,0xFFDED0E2);
        var contents=DisassemblyClient.view().contents();
        for (int display=0;display<contents.size();display++) {
            int slot=display<27?display+9:display<36?display-27:display;
            int x=left+10+(display%9)*20, y=top+49+(display/9)*23;
            if (display>=36) y+=7;
            g.fill(x-1,y-1,x+17,y+17,0xFF201C27);
            if (slot>=contents.size()) continue;
            var stack=contents.get(slot);
            g.item(stack,x,y); g.itemDecorations(font,stack,x,y);
            if (!stack.isEmpty() && mouseX>=x && mouseX<x+18 && mouseY>=y && mouseY<y+18)
                g.setTooltipForNextFrame(font,stack,mouseX,mouseY);
        }
        super.extractRenderState(g,mouseX,mouseY,partialTick);
    }
}
