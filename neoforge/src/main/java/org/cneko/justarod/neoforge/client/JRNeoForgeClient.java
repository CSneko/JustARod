package org.cneko.justarod.neoforge.client;

import org.cneko.justarod.Justarod;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

/**
 * 仅在客户端加载的入口。
 * 注意：客户端初始化（JustarodClient.init()）依赖通用注册的产物，
 * 因此统一由 JRNeoForge 在 RegisterEvent 窗口期内调用，这里不再重复调用。
 */
@Mod(value = Justarod.MODID, dist = Dist.CLIENT)
public class JRNeoForgeClient {

    public JRNeoForgeClient() {
    }
}
