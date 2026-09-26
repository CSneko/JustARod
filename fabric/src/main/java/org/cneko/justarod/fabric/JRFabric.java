package org.cneko.justarod.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.cneko.justarod.ComponentBinding;
import org.cneko.justarod.Justarod;

public class JRFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Justarod.init();

        // 26.1.2：主动绑定模组物品的默认组件（配方解析需要，见 ComponentBinding 注释）
        ComponentBinding.bindAll();

        // 26.1.2：组件绑定完成后重载资源，使配方管理器在首次加载后重新解析模组配方
        ServerLifecycleEvents.SERVER_STARTED.register(ComponentBinding::reloadRecipesAfterStart);
    }
}
