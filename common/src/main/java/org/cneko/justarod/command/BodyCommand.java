package org.cneko.justarod.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.Entity;
import org.cneko.justarod.ascaris.AscarisEntity;
import org.cneko.justarod.body.BodyWorld;

import java.util.UUID;

public final class BodyCommand {
    private BodyCommand() { }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("body")
                .then(Commands.literal("exit").executes(context ->
                        BodyWorld.exit(context.getSource().getPlayerOrException()) ? 1 : 0))
                .then(Commands.literal("reset")
                        .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .then(Commands.argument("host", EntityArgument.entity()).executes(context -> {
                            Entity target = EntityArgument.getEntity(context, "host");
                            UUID ownerUuid;
                            Component displayName;
                            if (target instanceof ServerPlayer sp) {
                                ownerUuid = sp.getUUID();
                                displayName = sp.getName();
                            } else if (target instanceof AscarisEntity ascaris) {
                                ownerUuid = ascaris.getBodyRegionOwnerUuid();
                                displayName = ascaris.getDisplayName();
                            } else {
                                context.getSource().sendSuccess(() -> Component.translatable(
                                        "body.reset.none", target.getDisplayName()), false);
                                return 0;
                            }
                            boolean reset = BodyWorld.reset(context.getSource().getServer(), ownerUuid);
                            context.getSource().sendSuccess(() -> Component.translatable(
                                    reset ? "body.reset.success" : "body.reset.none", displayName), false);
                            return reset ? 1 : 0;
                        }))));
    }
}
