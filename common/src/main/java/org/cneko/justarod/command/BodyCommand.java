package org.cneko.justarod.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
import org.cneko.justarod.body.BodyWorld;

public final class BodyCommand {
    private BodyCommand() { }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("body")
                .then(Commands.literal("exit").executes(context ->
                        BodyWorld.exit(context.getSource().getPlayerOrException()) ? 1 : 0))
                .then(Commands.literal("reset")
                        .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .then(Commands.argument("host", EntityArgument.player()).executes(context -> {
                            var host = EntityArgument.getPlayer(context, "host");
                            boolean reset = BodyWorld.reset(context.getSource().getServer(), host.getUUID());
                            context.getSource().sendSuccess(() -> Component.translatable(
                                    reset ? "body.reset.success" : "body.reset.none", host.getName()), false);
                            return reset ? 1 : 0;
                        }))));
    }
}
