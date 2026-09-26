package org.cneko.justarod.client.renderer.slime;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.cneko.justarod.packet.SlimeContentsPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端缓存：史莱姆体内物品的**图标列表**（按实体 id）。
 *
 * <p>服务端通过 {@link SlimeContentsPayload} 推送，渲染时由
 * {@link SlimeContentsLayer} 取用。客户端实体自身的 {@code storage()} 永远是空的
 * （那份数据不在实体同步范围内），所以必须走这个缓存。
 */
public final class SlimeClientContents {

    private static final Map<Integer, List<ItemStack>> CACHE = new ConcurrentHashMap<>();

    private SlimeClientContents() {
    }

    /** 收到服务端推送（渲染线程） */
    public static void accept(SlimeContentsPayload payload) {
        List<ItemStack> stacks = new ArrayList<>(payload.itemIds().size());
        for (int id : payload.itemIds()) {
            Item item = BuiltInRegistries.ITEM.byId(id);
            if (item != null && item != Items.AIR) {
                stacks.add(new ItemStack(item));
            }
        }
        if (stacks.isEmpty()) {
            CACHE.remove(payload.entityId());
        } else {
            CACHE.put(payload.entityId(), stacks);
        }
    }

    /** 该史莱姆体内要画的物品图标（没有就是空列表） */
    public static List<ItemStack> itemsFor(int entityId) {
        return CACHE.getOrDefault(entityId, List.of());
    }

    /** 实体被卸载/移除时清掉，避免 id 被复用后串味 */
    public static void forget(int entityId) {
        CACHE.remove(entityId);
    }
}
