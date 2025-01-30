package org.cneko.justarod.command;


import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class JRCommands {
    public static void init() {
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, dedicatedServer, registryAccess) -> RodCommand.register(dispatcher)
        );
        ImpartCommand.Companion.init();
        MateCommand.Companion.init();
    }
}
