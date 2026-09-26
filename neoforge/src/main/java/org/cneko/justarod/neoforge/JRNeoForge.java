package org.cneko.justarod.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.cneko.justarod.ComponentBinding;
import org.cneko.justarod.Justarod;
import org.cneko.justarod.client.JustarodClient;

@Mod(Justarod.MODID)
public class JRNeoForge {

    private static boolean initialized = false;

    public JRNeoForge(IEventBus modEventBus) {
        // NeoForge 在 mod 构造阶段注册表已经冻结，不能直接 Registry.register；
        // RegisterEvent 派发期间注册表处于开放状态，因此在第一个 RegisterEvent 时
        // 执行通用初始化（common 里 Fabric 风格的直接注册此时全部合法）。
        // 客户端初始化依赖通用注册的产物（JREntities 等），一并放在此处之后执行。
        modEventBus.addListener(this::onRegister);

        // 26.1.2：主动绑定模组物品的默认组件（配方解析需要，见 ComponentBinding 注释）
        modEventBus.addListener(FMLCommonSetupEvent.class,
                event -> event.enqueueWork(ComponentBinding::bindAll));

        // 26.1.2：服务器启动完成后重载资源，使配方管理器在组件绑定完成后重新解析
        modEventBus.addListener(ServerStartedEvent.class,
                event -> ComponentBinding.reloadRecipesAfterStart(event.getServer()));
    }

    private void onRegister(RegisterEvent event) {
        if (initialized) {
            return;
        }
        initialized = true;

        Justarod.init();
        if (FMLEnvironment.getDist().isClient()) {
            JustarodClient.init();
        }
    }
}
