package org.cneko.justarod.client.command;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.cneko.justarod.entity.slime.SlimeMenu;

/**
 * 客户端诊断命令：{@code /jrslime} 打印客户端菜单的真实状态。
 *
 * <p>用途：定位「服务端明明有物品、GUI 里却看不到」这类两端不一致的问题。
 * 输出里同时包含：containerId、槽位总数、前几个槽位的 {@code Slot#getItem()} 结果、
 * 以及当前屏幕类型。
 */
public final class SlimeDebugClientCommand {

    private SlimeDebugClientCommand() {
    }

    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) ->
                dispatcher.register(
                        net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal("jrslime")
                                .executes(ctx -> {
                                    dump(ctx.getSource());
                                    return 1;
                                })));
    }

    private static void dump(FabricClientCommandSource source) {
        Minecraft minecraft = Minecraft.getInstance();
        StringBuilder sb = new StringBuilder("[SlimeDebug/CLIENT] ");
        sb.append("screen=").append(minecraft.screen == null ? "null" : minecraft.screen.getClass().getSimpleName());
        if (minecraft.player == null) {
            source.sendFeedback(Component.literal(sb.append(" player=null").toString()));
            return;
        }
        sb.append(" menu=").append(minecraft.player.containerMenu.getClass().getSimpleName());
        if (minecraft.player.containerMenu instanceof SlimeMenu menu) {
            sb.append(" containerId=").append(menu.containerId);
            sb.append(" slots=").append(menu.slots.size());
            sb.append(" page=").append(menu.getPage());
            sb.append(" tier=").append(menu.getTierLevel());
            sb.append(" maxPage=").append(menu.getMaxPage());
            sb.append(" currentSlime=")
                    .append(org.cneko.justarod.client.event.JRSlimeClientEvents.currentSlime() == null
                            ? "null" : "ok");
            sb.append(" slotsItem=[");
            for (int i = 0; i < Math.min(6, menu.slots.size()); i++) {
                Slot slot = menu.slots.get(i);
                ItemStack stack = slot.getItem();
                sb.append(i).append(':').append(stack.isEmpty() ? "-" : stack.getHoverName().getString() + "x" + stack.getCount()).append(' ');
            }
            sb.append(']');
        }
        source.sendFeedback(Component.literal(sb.toString()));
    }
}
