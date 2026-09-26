package org.cneko.justarod.client.event

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents

/**
 * 26.1.2 迁移说明（原 ~530 行 HUD 覆盖层已被移除）：
 *
 * 原实现依赖 1.21.1 的旧 HUD 渲染管线：
 *   - Fabric `HudRenderCallback`（fabric-rendering-v1 23.x 中已删除，新 API 是
 *     net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry + HudElement，
 *     回调签名变为 extractRenderState(GuiGraphicsExtractor, DeltaTracker)）
 *   - net.minecraft.client.gui.GuiGraphics（26.x 已被 GuiGraphicsExtractor 取代）
 *   - RenderSystem.enableBlend/setShaderColor/setShaderTexture 等即时渲染调用（已删除）
 *   - GameRenderer.loadEffect 后处理着色器（白内障模糊，已删除）
 *
 * 如需恢复视觉效果，请基于 HudElementRegistry.addLast(Identifier, HudElement) 重写，
 * 并使用 GuiGraphicsExtractor 的新抽取式渲染 API（参考 toNeko 的 HUD/screen 实现）。
 * 现状：HUD 覆盖层（粉嫩GUI、高潮抖动、电击波纹、体力条、白内障模糊等）为 no-op。
 */
class ClientTickEvent {
    companion object {
        fun init() {
            // 保留 tick 钩子（当前无逻辑），原有的 END_CLIENT_TICK 白内障着色器
            // 更新逻辑因 GameRenderer#loadEffect 在 26.x 中被移除而一并禁用。
            ClientTickEvents.END_CLIENT_TICK.register {
                val player = it.player ?: return@register
                val vehicle = player.vehicle

                // 没骑史莱姆时清掉技能状态镜像：下马/换乘后右侧提示要立刻消失，
                // 而不是拿着上一头史莱姆的冷却与目标数继续显示。
                if (vehicle !is org.cneko.justarod.entity.slime.TamedSlimeEntity) {
                    org.cneko.justarod.client.slime.SlimeDevourClientState.reset()
                }

                // 界面打开时不发技能包：否则在背包里整理东西时按 G 会隔着界面把技能放掉
                val noScreen = it.screen == null

                // 被史莱姆吞入体内时，连点挣扎键积累逃脱进度
                while (org.cneko.justarod.client.JRKeyBindings.SLIME_STRUGGLE_KEY.consumeClick()) {
                    if (vehicle is org.cneko.justarod.entity.slime.TamedSlimeEntity) {
                        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                            org.cneko.justarod.packet.SlimeContainerActionPayload.of(
                                -1,
                                org.cneko.justarod.packet.SlimeContainerActionPayload.ACTION_STRUGGLE,
                                0, 0
                            )
                        )
                    }
                }

                // 在史莱姆体内按背包键：请求服务端打开它的背包。
                //
                // 这里**刻意不做 isOwner 自查**：权限边界必须在服务端
                // （JRSlimeNetworking 里按 UUID 校验），客户端多一道只会帮倒忙——
                // 归属 UUID 从不参与实体同步，拿它做门禁会让按键静默失效。
                while (org.cneko.justarod.client.JRKeyBindings.SLIME_BAG_KEY.consumeClick()) {
                    if (vehicle is org.cneko.justarod.entity.slime.TamedSlimeEntity) {
                        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                            org.cneko.justarod.packet.SlimeContainerActionPayload.of(
                                -1,
                                org.cneko.justarod.packet.SlimeContainerActionPayload.ACTION_OPEN_BAG,
                                0, 0
                            )
                        )
                    }
                }

                // 骑着史莱姆按吞噬键：请求发动手动「吞噬」技能。
                // 同样不做 isOwner 自查：冷却/等级/目标/空间/归属全部由服务端裁决。
                while (org.cneko.justarod.client.JRKeyBindings.SLIME_DEVOUR_KEY.consumeClick()) {
                    if (noScreen && vehicle is org.cneko.justarod.entity.slime.TamedSlimeEntity) {
                        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                            org.cneko.justarod.packet.SlimeContainerActionPayload.of(
                                -1,
                                org.cneko.justarod.packet.SlimeContainerActionPayload.ACTION_DEVOUR,
                                0, 0
                            )
                        )
                    }
                }
            }
        }
    }
}
