package org.cneko.justarod.command

import com.mojang.brigadier.arguments.IntegerArgumentType
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.text.Text
import org.cneko.justarod.entity.Pregnant

class PregnantCommand {
    companion object{
        fun init(){
            CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
                dispatcher.register(literal("pregnant")
                    .executes {ctx ->
                        val source = ctx.source.entity
                        if (source is Pregnant){
                            source.sendMessage(Text.of("剩余孕期：${source.pregnant/20/60/20}天"))
                        }
                        return@executes 1
                    }
                    .then(literal("set")
                        .then(argument("time",IntegerArgumentType.integer(0, Int.MAX_VALUE))
                            .requires { source -> source.isExecutedByPlayer&&source.player?.isCreative==true }
                            .executes { ctx->
                                val source = ctx.source.entity
                                if (source is Pregnant) {
                                    source.pregnant = IntegerArgumentType.getInteger(ctx,"time")
                                }
                                return@executes 1
                            }
                        )
                    )
                )
            }
        }
    }
}