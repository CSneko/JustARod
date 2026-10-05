package org.cneko.justarod.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import org.cneko.justarod.client.gui.JRConfigScreen;

/**
 * ModMenu 集成：在 ModMenu 的 JustARod 条目上提供「设置」按钮入口。
 */
public class JRModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return JRConfigScreen::new;
    }
}
