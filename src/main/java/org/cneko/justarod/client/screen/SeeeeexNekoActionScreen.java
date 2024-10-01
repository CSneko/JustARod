package org.cneko.justarod.client.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.cneko.justarod.entity.SeeeeexNekoEntity;
import org.cneko.toneko.fabric.client.screens.INekoScreen;
import org.cneko.toneko.fabric.client.screens.InteractionScreen;
import org.cneko.toneko.fabric.client.screens.NekoActionScreen;
import org.cneko.toneko.fabric.entities.NekoEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class SeeeeexNekoActionScreen extends InteractionScreen implements INekoScreen {
    public SeeeeexNekoEntity neko;
    public SeeeeexNekoActionScreen(@NotNull SeeeeexNekoEntity neko, @Nullable Screen lastScreen) {
        super(Text.empty(),lastScreen, (screen)-> getSeeeeexButtonBuilders(neko));
        this.neko = neko;
    }

    @Override
    public SeeeeexNekoEntity getNeko() {
        return neko;
    }

    public static Map<String, ButtonWidget.Builder> getSeeeeexButtonBuilders(SeeeeexNekoEntity neko){
        Map<String, ButtonWidget.Builder> buttonBuilders = NekoActionScreen.getButtonBuilders(neko);
        buttonBuilders.put("screen.justarod.seeeeex_neko_action.button.mb", ButtonWidget.builder(Text.of("screen.justarod.seeeeex_neko_action.button.mb"), (button)->{
            // TODO 不想做网络发包
            neko.setMasturbation(true);
        }));
        return buttonBuilders;
    }

    public static void open(SeeeeexNekoEntity neko) {
        MinecraftClient.getInstance().setScreen(new SeeeeexNekoActionScreen(neko, MinecraftClient.getInstance().currentScreen));
    }
}
