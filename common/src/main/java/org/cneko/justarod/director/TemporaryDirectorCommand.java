package org.cneko.justarod.director;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.Mob;
import org.cneko.justarod.config.JRConfig;
import org.cneko.justarod.pet.PetController;
import org.cneko.justarod.pet.PetState;

/** TEMPORARY filming adapter. Delete this package and its one registration after filming. */
public final class TemporaryDirectorCommand {
    private TemporaryDirectorCommand() { }
    @FunctionalInterface private interface Action { boolean run(ServerPlayer player); }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("jar_director").requires(source -> JRConfig.isAbsurdityEnabled()
                && source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER));
        root.then(Commands.literal("tame").requires(source -> JRConfig.isPlayerTamingEnabled())
                .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("owner", EntityArgument.entity()).executes(c -> {
                    if (!enabled(c) || !JRConfig.isPlayerTamingEnabled()) return 0;
                    var p = EntityArgument.getPlayer(c, "player");
                    var npc = EntityArgument.getEntity(c, "owner");
                    if (!PetController.validOwner(npc) || PetState.of(p).owner != null || npc.level() != p.level()
                            || p.distanceToSqr(npc) > 36) return result(c, false);
                    PetController.open(p, npc.getId(), true);
                    return result(c, true);
                }))));
        var order = Commands.literal("order").then(Commands.argument("player", EntityArgument.player())
                .then(Commands.literal("sit").executes(c -> run(c, p -> PetController.order(p, PetState.Mode.SIT))))
                .then(Commands.literal("stand").executes(c -> run(c, p -> PetController.order(p, PetState.Mode.FREE))))
                .then(Commands.literal("follow").executes(c -> run(c, p -> PetController.order(p, PetState.Mode.FOLLOW)))));
        root.then(order);
        root.then(action("pet", p -> PetController.stroke(p, true)));
        root.then(action("tempt", PetController::tempt));
        root.then(action("recall", p -> PetController.recall(p, true)));
        root.then(action("reset", p -> {
            PetController.release(p);
            var s = PetState.of(p);
            s.nextOffer = s.nextInteraction = s.nextFeed = s.nextInstinct = 0;
            return true;
        }));
        root.then(Commands.literal("transfer").then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("owner", EntityArgument.entity()).executes(c -> {
                    if (!enabled(c)) return 0;
                    var npc = EntityArgument.getEntity(c, "owner");
                    return result(c, npc instanceof Mob mob && PetController.requestTransfer(EntityArgument.getPlayer(c, "player"), mob));
                }))));
        dispatcher.register(root);
    }
    private static LiteralArgumentBuilder<CommandSourceStack> action(String name, Action action) {
        return Commands.literal(name).then(Commands.argument("player", EntityArgument.player()).executes(c -> run(c, action)));
    }
    private static int run(CommandContext<CommandSourceStack> c, Action action) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return enabled(c) ? result(c, action.run(EntityArgument.getPlayer(c, "player"))) : 0;
    }
    private static boolean enabled(CommandContext<CommandSourceStack> c) {
        return JRConfig.isAbsurdityEnabled() && c.getSource().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }
    private static int result(CommandContext<CommandSourceStack> c, boolean success) {
        c.getSource().sendSuccess(() -> Component.translatable("pet.justarod.director." + (success ? "success" : "failed")), false);
        return success ? 1 : 0;
    }
}
