package org.cneko.justarod.client.feature;

import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 存放 AvatarRenderer extract 阶段生成的口球 ItemStackRenderState，
 * 供提交式 RenderLayer 在 submit 阶段读取。
 */
public final class BallMouthRenderStateCache {

    private static final Map<AvatarRenderState, ItemStackRenderState> CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    private BallMouthRenderStateCache() {
    }

    public static void put(AvatarRenderState state, ItemStackRenderState itemState) {
        CACHE.put(state, itemState);
    }

    public static void remove(AvatarRenderState state) {
        CACHE.remove(state);
    }

    public static ItemStackRenderState get(AvatarRenderState state) {
        return CACHE.get(state);
    }
}
