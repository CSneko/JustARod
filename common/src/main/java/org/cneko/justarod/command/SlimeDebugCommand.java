package org.cneko.justarod.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.cneko.justarod.entity.slime.SlimeMenu;
import org.cneko.justarod.entity.slime.TamedSlimeEntity;

import java.util.List;

/**
 * 诊断命令：打印史莱姆体内容器的两端状态，用来定位「GUI 里看不到物品」这类同步问题。
 *
 * <p>服务端侧：{@code /justarod slime debug}；客户端侧对应命令是 {@code /jrslime}
 * （见 {@code SlimeDebugClientCommand}）。两端对照即可定位同步问题。
 */
public final class SlimeDebugCommand {

    private SlimeDebugCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("justarod")
                .then(Commands.literal("slime")
                        .then(Commands.literal("debug")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayer();
                                    if (player == null) {
                                        context.getSource().sendFailure(Component.literal("需要玩家执行"));
                                        return 0;
                                    }
                                    dumpServer(player);
                                    // 顺带把结果广播给执行者自己，客户端侧由客户端命令输出
                                    return 1;
                                }))));
    }

    /** 服务端侧状态 */
    public static void dumpServer(ServerPlayer player) {
        StringBuilder sb = new StringBuilder("[SlimeDebug/SERVER] ");
        sb.append("containerMenu=").append(player.containerMenu.getClass().getSimpleName());
        if (player.containerMenu instanceof SlimeMenu menu) {
            TamedSlimeEntity slime = menu.slime();
            sb.append(" containerId=").append(menu.containerId);
            sb.append(" slots=").append(menu.slots.size());
            sb.append(" page=").append(menu.getPage());
            sb.append(" tier=").append(menu.getTierLevel());
            if (slime != null) {
                sb.append(" entityId=").append(slime.getId());
                sb.append(" entityTier=").append(slime.getTierLevel());
                sb.append(" storageSize=").append(slime.storage().items().size());
                sb.append(" used=").append(slime.storage().usedSpace());
                List<ItemStack> items = slime.storage().items();
                sb.append(" items=[");
                for (int i = 0; i < Math.min(items.size(), 12); i++) {
                    ItemStack stack = items.get(i);
                    sb.append(i).append(':').append(stack.isEmpty() ? "-" : stack.getHoverName().getString() + "x" + stack.getCount()).append(' ');
                }
                sb.append(']');
            } else {
                sb.append(" slime=null");
            }
            // 打印前 4 个容器槽位的实际内容（Slot.getItem）
            sb.append(" slotsItem=[");
            for (int i = 0; i < Math.min(4, menu.slots.size()); i++) {
                Slot slot = menu.slots.get(i);
                ItemStack stack = slot.getItem();
                sb.append(i).append(':').append(stack.isEmpty() ? "-" : stack.getHoverName().getString() + "x" + stack.getCount()).append(' ');
            }
            sb.append(']');
        }
        player.sendSystemMessage(Component.literal(sb.toString()));
    }
}
