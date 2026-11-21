package org.cneko.justarod.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.cneko.justarod.pet.PetController;
import org.cneko.justarod.pet.PetState;

/** Player-owned escape hatches remain available without operator permissions. */
public final class PetCommand {
    private PetCommand() { }
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("jarpet")
                .executes(c -> {
                    var p = c.getSource().getPlayerOrException();
                    var s = PetState.of(p);
                    p.sendSystemMessage(Component.translatable("pet.justarod.status", s.owner == null ? "—" : s.ownerName,
                            Component.translatable("pet.justarod.mode." + s.mode.name().toLowerCase(java.util.Locale.ROOT))));
                    return 1;
                })
                .then(Commands.literal("menu").executes(c -> {
                    PetController.open(c.getSource().getPlayerOrException(), -1, false); return 1;
                }))
                .then(Commands.literal("cancel").executes(c -> {
                    PetController.cancel(c.getSource().getPlayerOrException(), true); return 1;
                }))
                .then(Commands.literal("release").executes(c -> {
                    PetController.release(c.getSource().getPlayerOrException()); return 1;
                })));
    }
}
