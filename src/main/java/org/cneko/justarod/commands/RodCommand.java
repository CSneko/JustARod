package org.cneko.justarod.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.Objects;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class RodCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("rod")
                .then(CommandManager.literal("item")
                        .then(CommandManager.literal("get").executes(context -> {
                            return getRodItem(Objects.requireNonNull(context.getSource().getPlayer()));
                        }))
                        .then(CommandManager.literal("remove").executes(context -> {
                            return removeRodItem(Objects.requireNonNull(context.getSource().getPlayer()));
                        }))
                        // TODO: 后续添加 set 命令
                )
        );
    }

    private static int getRodItem(ServerPlayerEntity player) {
        ItemStack stack = player.getRodInside();
        if (stack!= null && stack!= ItemStack.EMPTY) {
            String message = Text.translatable("command.justarod.rod.get.success") + stack.getItem().getName().getString();
            player.sendMessage(Text.of(message), false);
            return SINGLE_SUCCESS;
        }
        else {
            player.sendMessage(Text.translatable("command.justarod.rod.get.failure"), false);
            return 0;
        }
    }

    private static int setRodItem(ServerPlayerEntity player, ItemStack stack) {
        player.setRodInside(stack);
        player.sendMessage(Text.translatable("command.justarod.rod.set.success"), false);
        return SINGLE_SUCCESS;
    }

    private static int removeRodItem(ServerPlayerEntity player) {
        player.setRodInside(null);
        player.sendMessage(Text.translatable("command.justarod.rod.remove.success"), false);
        return SINGLE_SUCCESS;
    }
}
