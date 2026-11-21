package org.cneko.justarod.client.disassembly;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.cneko.justarod.disassembly.BodyPart;
import org.cneko.justarod.packet.DisassemblyActionPayload;

public final class DisassemblyScreen extends Screen {
    private int left,top,panel,rowStep,panelHeight;
    public DisassemblyScreen() { super(Component.translatable("disassembly.justarod.title")); }
    public void refresh() { if (minecraft!=null && minecraft.player!=null) rebuildWidgets(); }
    private Component text(String key) { return Component.translatable("disassembly.justarod."+key); }
    @Override protected void init() {
        panel=Math.min(370,width-12); left=(width-panel)/2;
        rowStep=Math.min(25,Math.max(16,(height-95)/7)); panelHeight=90+7*rowStep; top=Math.max(2,(height-panelHeight)/2);
        if (minecraft==null || minecraft.player==null) return;
        int id=minecraft.player.getId(), col=(panel-130)/3;
        for (BodyPart part:BodyPart.values()) {
            int y=top+39+part.ordinal()*rowStep;
            boolean missing=DisassemblyClient.missing(id,part);
            addRenderableWidget(Button.builder(text(missing?"attach":"detach"),b->DisassemblyClient.send(missing?DisassemblyActionPayload.ATTACH:DisassemblyActionPayload.DETACH,part.ordinal()))
                    .bounds(left+116,y,col,Math.min(20,rowStep-2)).build());
            var place=addRenderableWidget(Button.builder(text("place"),b->{ DisassemblyClient.send(DisassemblyActionPayload.PLACE,part.ordinal()); onClose(); })
                    .bounds(left+120+col,y,col,Math.min(20,rowStep-2)).build()); place.active=missing;
            if (part.camera() || part.arm()) {
                var action=addRenderableWidget(Button.builder(text(part.eye()?"dual":part.camera()?"view":"mine"),b->{
                    DisassemblyClient.send(part.eye()?DisassemblyActionPayload.DUAL:part.camera()?DisassemblyActionPayload.VIEW:DisassemblyActionPayload.MINE,part.ordinal()); onClose();
                }).bounds(left+124+2*col,y,col,Math.min(20,rowStep-2)).build()); action.active=missing;
            }
        }
        boolean mounted=!DisassemblyClient.state(id).eyes().isEmpty();
        String[] labels=mounted?new String[]{"recall",DisassemblyClient.state(id).random()?"random_on":"random_off","remove_eyes","exit_view"}
                :new String[]{"recall",DisassemblyClient.state(id).random()?"random_on":"random_off","exit_view"};
        int[] actions=mounted?new int[]{DisassemblyActionPayload.RECALL,DisassemblyActionPayload.RANDOM,DisassemblyActionPayload.REMOVE_EYES,DisassemblyActionPayload.EXIT}
                :new int[]{DisassemblyActionPayload.RECALL,DisassemblyActionPayload.RANDOM,DisassemblyActionPayload.EXIT};
        int bw=(panel-16-6*(labels.length-1))/labels.length;
        for (int i=0;i<labels.length;i++) {
            int action=actions[i];
            addRenderableWidget(Button.builder(text(labels[i]),b->{
                DisassemblyClient.send(action,0); if (action==DisassemblyActionPayload.EXIT) onClose();
            }).bounds(left+8+i*(bw+6),top+44+7*rowStep,bw,20).build());
        }
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float partialTick) {
        g.fill(left,top,left+panel,top+panelHeight,0xEE272A37); g.fill(left,top,left+panel,top+3,0xFFFFB4D4);
        g.centeredText(font,title,width/2,top+12,0xFFFFC9E0);
        if (minecraft!=null && minecraft.player!=null) for (BodyPart part:BodyPart.values()) {
            boolean missing=DisassemblyClient.missing(minecraft.player.getId(),part);
            g.text(font,part.label(),left+10,top+45+part.ordinal()*rowStep,missing?0xFFFFB6D3:0xFFE8E8EE);
        }
        g.centeredText(font,text("menu_hint"),width/2,top+panelHeight-14,0xFFCBC9D5);
        super.extractRenderState(g,mouseX,mouseY,partialTick);
    }
}
