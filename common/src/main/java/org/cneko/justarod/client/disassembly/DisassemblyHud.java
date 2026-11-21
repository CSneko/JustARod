package org.cneko.justarod.client.disassembly;

import net.fabricmc.fabric.api.client.rendering.v1.hud.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.cneko.justarod.disassembly.BodyPart;
import org.cneko.justarod.client.JRKeyBindings;

public final class DisassemblyHud {
    public static void init() {
        HudElementRegistry.addFirst(Identifier.fromNamespaceAndPath("justarod","dual_body_view"),(g,delta)-> {
            var client=Minecraft.getInstance();
            if (client.player!=null && client.screen==null && DisassemblyClient.splitting()) SplitBodyView.draw(g);
        });
        HudElementRegistry.attachElementBefore(VanillaHudElements.HOTBAR,Identifier.fromNamespaceAndPath("justarod","detached_eyes"),(g,delta)-> {
            var client=Minecraft.getInstance();
            if (client.player==null || client.screen!=null) return;
            int id=client.player.getId();
            boolean left=DisassemblyClient.missing(id,BodyPart.LEFT_EYE), right=DisassemblyClient.missing(id,BodyPart.RIGHT_EYE);
            if (DisassemblyClient.splitting()) return;
            if (DisassemblyClient.hasView()) g.centeredText(client.font,Component.translatable("disassembly.justarod.view_hint"),g.guiWidth()/2,12,0xFFFFCADD);
            else if (client.options.getCameraType().isFirstPerson()) {
                if (left && right) {
                    g.fill(0,0,g.guiWidth(),g.guiHeight(),0xFF18141E);
                    g.centeredText(client.font,Component.translatable("disassembly.justarod.blind",JRKeyBindings.DISASSEMBLY_MENU_KEY.getTranslatedKeyMessage()),g.guiWidth()/2,g.guiHeight()/2,0xFFFFCADF);
                } else if (left || right) {
                    int start=left?0:g.guiWidth()*3/4, end=left?g.guiWidth()/4:g.guiWidth();
                    g.fill(start,0,end,g.guiHeight(),0xDD18141E);
                }
            }
        });
    }
}
