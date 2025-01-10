package org.cneko.justarod.commands;


import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class JRCommands {
    public static void init() {
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, dedicatedServer, registryAccess) -> RodCommand.register(dispatcher)
        );
    }
}
