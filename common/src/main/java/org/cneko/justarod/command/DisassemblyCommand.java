package org.cneko.justarod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;
import org.cneko.justarod.disassembly.*;

/** All filming controls affect only the invoking player's body. */
public final class DisassemblyCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("jarbody").executes(c -> {
            var p = c.getSource().getPlayerOrException();
            p.sendSystemMessage(Component.translatable("disassembly.justarod.command_hint")); return 1;
        });
        for (BodyPart part : BodyPart.values()) {
            root.then(Commands.literal("detach").then(Commands.literal(part.key()).executes(c -> DisassemblyController.detach(c.getSource().getPlayerOrException(),part) ? 1 : 0)));
            root.then(Commands.literal("place").then(Commands.literal(part.key()).executes(c -> { DisassemblyController.deploy(c.getSource().getPlayerOrException(),part); return 1; })));
            root.then(Commands.literal("attach").then(Commands.literal(part.key()).executes(c -> { DisassemblyController.handle(c.getSource().getPlayerOrException(),6,part.ordinal()); return 1; })));
            if (part.camera()) root.then(Commands.literal("view").then(Commands.literal(part.key()).executes(c -> { DisassemblyController.handle(c.getSource().getPlayerOrException(),2,part.ordinal()); return 1; })));
            if (part.arm()) root.then(Commands.literal("mine").then(Commands.literal(part.key()).executes(c -> { DisassemblyController.mine(c.getSource().getPlayerOrException(),part); return 1; })));
            for (String action : new String[]{"run", "strike"}) root.then(Commands.literal(action).then(Commands.literal(part.key()).executes(c -> {
                for (var e : DisassemblyController.entities(c.getSource().getPlayerOrException())) if (e.part() == part) { if (action.equals("run")) e.flee(); else e.strike(); }
                return 1;
            })));
        }
        root.then(Commands.literal("run").then(Commands.literal("all").executes(c -> {
            var p = c.getSource().getPlayerOrException();
            DisassemblyController.exitView(p);
            for (BodyPart part : BodyPart.values()) {
                DisassemblyController.detach(p,part);
                if (p.getInventory().contains(stack -> stack.getItem() == DisassemblyItems.get(part)
                        && p.getUUID().equals(DetachedPartItem.owner(stack)))) DisassemblyController.deploy(p,part);
            }
            for (var e : DisassemblyController.entities(p)) e.flee(); return 1;
        })));
        for (BodyPart part : BodyPart.values()) if (part.eye()) root.then(Commands.literal("dual").then(Commands.literal(part.key()).executes(c -> {
            DisassemblyVision.start(c.getSource().getPlayerOrException(),part,true); return 1;
        })));
        root.then(Commands.literal("unmount").executes(c -> { var p=c.getSource().getPlayerOrException(); DisassemblyVision.remove(p,p); return 1; }));
        root.then(Commands.literal("recall").executes(c -> { DisassemblyController.recall(c.getSource().getPlayerOrException()); return 1; }));
        root.then(Commands.literal("reset").executes(c -> { DisassemblyController.reset(c.getSource().getPlayerOrException()); return 1; }));
        root.then(Commands.literal("exit").executes(c -> { DisassemblyController.exitView(c.getSource().getPlayerOrException()); return 1; }));
        for (String value : new String[]{"on","off"}) root.then(Commands.literal("random").then(Commands.literal(value).executes(c -> {
            var p = c.getSource().getPlayerOrException(); DisassemblyState.of(p).random = value.equals("on"); DisassemblyController.sync(p); return 1;
        })));
        root.then(Commands.literal("say").then(Commands.argument("text", StringArgumentType.greedyString()).executes(c -> {
            for (var e : DisassemblyController.entities(c.getSource().getPlayerOrException())) if (e.part() == BodyPart.HEAD)
                e.say(Component.literal(StringArgumentType.getString(c,"text").substring(0,Math.min(120,StringArgumentType.getString(c,"text").length()))));
            return 1;
        })));
        dispatcher.register(root);
    }
}
