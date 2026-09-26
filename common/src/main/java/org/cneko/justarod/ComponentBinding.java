package org.cneko.justarod;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 26.1.2 兼容性修复：
 * 配方解析要求结果物品的默认组件已绑定（Item.CODEC_WITH_BOUND_COMPONENTS →
 * Holder.areComponentsBound()）。组件绑定 pass（DATA_COMPONENT_INITIALIZERS.build(lookup).apply()）
 * 只为“存在于 lookup 中的物品”创建绑定；在 Forgified Fabric API 的环境下，
 * 服务器资源加载使用的 lookup 不含模组物品，导致所有以模组物品为结果的配方
 * 在解析时报 "Item xxx does not have components yet" 而被拒载。
 *
 * 修复：直接基于活注册表（BuiltInRegistries.REGISTRY，含模组物品）构建 RegistryAccess
 * 并执行绑定。PendingComponents.apply() 只是 holder.bindComponents(map)，幂等可重复执行。
 */
public final class ComponentBinding {
    private static final Logger LOGGER = LoggerFactory.getLogger("JustARodComponentBinding");

    private ComponentBinding() {
    }

    public static void bindAll() {
        try {
            var access = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
            var pending = BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(access);
            for (var pc : pending) {
                pc.apply();
            }
            LOGGER.info("Early-bound item components ({} groups).", pending.size());
        } catch (Throwable t) {
            LOGGER.warn("Failed to early-bind item components", t);
        }
    }

    /**
     * 服务器启动完成后触发一次资源重载（等同 /reload）。
     * 世界加载时的配方解析发生在组件绑定完成之前，会被 "does not have components yet"
     * 拒载且无人重试；此时组件已绑定，重载即可让 RecipeManager 重新解析并收下配方。
     */
    public static void reloadRecipesAfterStart(net.minecraft.server.MinecraftServer server) {
        try {
            server.reloadResources(server.getWorldData().getDataConfiguration().dataPacks().getEnabled());
            LOGGER.info("Triggered one-shot resource reload so recipes parse with bound components");
        } catch (Throwable t) {
            LOGGER.warn("Failed to trigger recipe reload", t);
        }
    }

    private static boolean checkBound(String id) {
        try {
            var key = ResourceKey.create(Registries.ITEM, Identifier.parse(id));
            return BuiltInRegistries.ITEM.get(key).map(Holder::areComponentsBound).orElse(false);
        } catch (Throwable t) {
            return false;
        }
    }
}
