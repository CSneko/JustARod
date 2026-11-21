package org.cneko.justarod.client.disassembly;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Alternate normal world frames between two cameras, retaining both on the GPU.
 * Each panel is live at half the world frame rate; no recursive level rendering or extra world ticks.
 */
public final class SplitBodyView {
    private static TextureTarget body,eye;
    private static boolean remote,bodyReady,eyeReady;
    public static boolean remoteFrame() { return remote; }
    public static boolean ready() { return bodyReady && eyeReady; }
    public static void beginFrame() {
        remote=!remote;
        var target=Minecraft.getInstance().getMainRenderTarget();
        if (body==null || body.width!=target.width || body.height!=target.height) {
            close();
            body=new TextureTarget("justarod body eye",target.width,target.height,false);
            eye=new TextureTarget("justarod mounted eye",target.width,target.height,false);
        }
    }
    public static void capture() {
        if (!DisassemblyClient.splitting() || body==null) return;
        var target=Minecraft.getInstance().getMainRenderTarget();
        var copy=remote?eye:body;
        if (copy.width!=target.width || copy.height!=target.height) return;
        RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(target.getColorTexture(),copy.getColorTexture(),0,0,0,0,0,target.width,target.height);
        if (remote) eyeReady=true; else bodyReady=true;
    }
    public static void draw(GuiGraphicsExtractor g) {
        if (!DisassemblyClient.splitting() || body==null) return;
        var client=Minecraft.getInstance();
        int gap=4,w=(g.guiWidth()-gap)/2,h=Math.min(g.guiHeight()-52,w*body.height/body.width),y=(g.guiHeight()-h)/2;
        g.fill(0,0,g.guiWidth(),g.guiHeight(),0xFF17141F);
        var sampler=RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        if (bodyReady) g.blit(body.getColorTextureView(),sampler,0,y,w,y+h,0,1,1,0);
        if (eyeReady) g.blit(eye.getColorTextureView(),sampler,w+gap,y,2*w+gap,y+h,0,1,1,0);
        g.fill(w,0,w+gap,g.guiHeight(),0xFFFFB4D4);
        if (client.options.hideGui) return;
        int cx=w/2,cy=y+h/2;
        g.fill(cx-3,cy,cx+4,cy+1,0xFFFFFFFF); g.fill(cx,cy-3,cx+1,cy+4,0xFFFFFFFF);
        g.centeredText(client.font,Component.translatable("disassembly.justarod.body_view"),w/2,y-15,0xFFFFCADD);
        g.centeredText(client.font,Component.translatable("disassembly.justarod.installed_view",DisassemblyClient.view().carrier()),w+gap+w/2,y-15,0xFFFFCADD);
        g.centeredText(client.font,Component.translatable("disassembly.justarod.dual_hint"),g.guiWidth()/2,y+h+10,0xFFDED0E2);
    }
    public static void close() {
        if (body!=null) body.destroyBuffers(); if (eye!=null) eye.destroyBuffers();
        body=null; eye=null; bodyReady=false; eyeReady=false;
    }
}
