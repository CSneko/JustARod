package org.cneko.justarod.neoforge.client;

import org.cneko.justarod.Justarod;
import org.cneko.justarod.client.gui.JRConfigScreen;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * 仅在客户端加载的入口。
 * 注意：客户端初始化（JustarodClient.init()）依赖通用注册的产物，
 * 因此统一由 JRNeoForge 在 RegisterEvent 窗口期内调用，这里不再重复调用。
 */
@Mod(value = Justarod.MODID, dist = Dist.CLIENT)
public class JRNeoForgeClient {

    public JRNeoForgeClient(ModContainer container) {
        // 注册配置屏幕：NeoForge 设置页 / Mod 列表中的「配置」按钮入口
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new JRConfigScreen());
    }
}
