package org.cneko.justarod.command;


import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class JRCommands {
    public static void init() {
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, dedicatedServer, registryAccess) -> {
                    RodCommand.register(dispatcher);
                    org.cneko.justarod.command.SlimeDebugCommand.register(dispatcher);
                    BodyCommand.register(dispatcher);
                    AscarisCommand.register(dispatcher);
                    PetCommand.register(dispatcher);
                    DisassemblyCommand.register(dispatcher);
                    // TEMPORARY: remove this registration and the director package after filming.
                    org.cneko.justarod.director.TemporaryDirectorCommand.register(dispatcher);
                }
        );
        ImpartCommand.Companion.init();
        MateCommand.Companion.init();
        PregnantCommand.Companion.init();
        BDSMCommand.Companion.init();

    }
}
