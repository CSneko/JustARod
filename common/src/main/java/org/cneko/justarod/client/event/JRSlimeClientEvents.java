package org.cneko.justarod.client.event;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.entity.Entity;
import org.cneko.justarod.client.renderer.slime.JRTamedSlimeModel;
import org.cneko.justarod.client.renderer.slime.JRTamedSlimeShellModel;
import org.cneko.justarod.client.renderer.slime.JRTamedSlimeRenderer;
import org.cneko.justarod.client.renderer.slime.SlimeClientContents;
import org.cneko.justarod.client.screen.SlimeScreen;
import org.cneko.justarod.client.slime.SlimeDevourClientState;
import org.cneko.justarod.entity.JREntities;
import org.cneko.justarod.entity.slime.JRMenus;
import org.cneko.justarod.entity.slime.SlimeMenu;
import org.cneko.justarod.entity.slime.TamedSlimeEntity;
import org.cneko.justarod.packet.SlimeContentsPayload;
import org.cneko.justarod.packet.SlimeDevourStatePayload;
import org.cneko.justarod.packet.SlimeEntityListPayload;
import org.cneko.justarod.packet.SlimeMenuContextPayload;

/**
 * 驯服史莱姆的客户端注册：模型层 / 渲染器 / 数据包 / 容器界面。
 */
public final class JRSlimeClientEvents {

    /** 当前正在交互的史莱姆（由上下文包提供，供实体页显示；可能为 null） */
    private static TamedSlimeEntity currentSlime;

    private JRSlimeClientEvents() {
    }

    public static void init() {
        // 模型层 + 渲染器
        ModelLayerRegistry.registerModelLayer(JRTamedSlimeModel.LAYER, JRTamedSlimeModel::createBodyLayer);
        ModelLayerRegistry.registerModelLayer(JRTamedSlimeShellModel.LAYER, JRTamedSlimeShellModel::createBodyLayer);
        EntityRendererRegistry.register(JREntities.TAMED_SLIME, JRTamedSlimeRenderer::new);

        // 注册界面工厂：不注册的话原版会打印
        // "Failed to create screen for menu type: justarod:slime_menu" 并留一个打不开的界面。
        // 关键：让原版直接用 SlimeScreen 建界面，**绝不**在 tick 里替换已有界面——
        // 替换会触发原版界面的 removed() → onClose()，把同步中的菜单关掉，
        // 结果就是「服务端有物品、GUI 里永远看不到」。
        MenuScreens.register(JRMenus.SLIME_MENU, SlimeScreen::create);

        // 打开容器前服务端推来的上下文：等级用于建槽位，实体 id 用于实体页
        ClientPlayNetworking.registerGlobalReceiver(SlimeMenuContextPayload.ID,
                (payload, context) -> context.client().execute(() -> {
                    TamedSlimeEntity slime = resolveSlime(payload.slimeEntityId());
                    SlimeMenu.acceptContext(payload.tierLevel(), slime);
                    currentSlime = slime;
                }));

        // 服务端推进来的体内实体列表：缓存起来供界面显示
        ClientPlayNetworking.registerGlobalReceiver(SlimeEntityListPayload.ID,
                (payload, context) -> context.client().execute(() -> SlimeScreen.acceptEntityList(payload)));

        // 服务端推进来的体内物品图标：缓存起来供「体内内容」渲染层使用
        ClientPlayNetworking.registerGlobalReceiver(SlimeContentsPayload.ID,
                (payload, context) -> context.client().execute(() -> SlimeClientContents.accept(payload)));

        // 手动「吞噬」技能状态（冷却 + 附近可吞目标数），供屏幕右侧提示使用
        ClientPlayNetworking.registerGlobalReceiver(SlimeDevourStatePayload.ID,
                (payload, context) -> context.client().execute(() -> SlimeDevourClientState.accept(
                        payload.slimeEntityId(), payload.cooldownTicks(), payload.targetCount())));
    }

    /** 最近一次上下文包对应的史莱姆（可能为 null，仅供调试命令使用） */
    public static TamedSlimeEntity currentSlime() {
        return currentSlime;
    }

    private static TamedSlimeEntity resolveSlime(int entityId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return null;
        Entity entity = minecraft.level.getEntity(entityId);
        return entity instanceof TamedSlimeEntity tamed ? tamed : null;
    }
}
