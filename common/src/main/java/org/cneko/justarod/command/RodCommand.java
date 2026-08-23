package org.cneko.justarod.command;

import com.mojang.brigadier.CommandDispatcher;
import java.util.Objects;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static org.cneko.toneko.common.mod.util.PermissionUtil.has;

public class RodCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rod")
                .requires(source-> has(source,Permissions.COMMAND_ROD))
                .then(Commands.literal("item")
                        .requires(source-> has(source,Permissions.COMMAND_ROD_ITEM))
                        .then(Commands.literal("get").executes(context -> {
                            return getRodItem(Objects.requireNonNull(context.getSource().getPlayer()));
                        }))
                        .then(Commands.literal("remove").executes(context -> {
                            return removeRodItem(Objects.requireNonNull(context.getSource().getPlayer()));
                        }))
                        // TODO: 后续添加 set 命令
                )
        );
    }

    private static int getRodItem(ServerPlayer player) {
        ItemStack stack = player.getRodInside();
        if (stack!= null && stack!= ItemStack.EMPTY) {
            Component message = Component.translatable("command.justarod.rod.get.success",stack.getItem().getDescription().getString());
            player.displayClientMessage(message, false);
            return SINGLE_SUCCESS;
        }
        else {
            player.displayClientMessage(Component.translatable("command.justarod.rod.get.failure"), false);
            return 0;
        }
    }

    private static int setRodItem(ServerPlayer player, ItemStack stack) {
        player.setRodInside(stack);
        player.displayClientMessage(Component.translatable("command.justarod.rod.set.success"), false);
        return SINGLE_SUCCESS;
    }

    private static int removeRodItem(ServerPlayer player) {
        player.setRodInside(ItemStack.EMPTY);
        player.displayClientMessage(Component.translatable("command.justarod.rod.remove.success"), false);
        // 出来了出来了
        return SINGLE_SUCCESS;
    }
}
