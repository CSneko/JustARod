package org.cneko.justarod.command

import com.mojang.brigadier.arguments.BoolArgumentType
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
                            .requires { source -> source.hasPermissionLevel(4) }
                            .executes { ctx->
                                val source = ctx.source.entity
                                if (source is Pregnant) {
                                    source.pregnant = IntegerArgumentType.getInteger(ctx,"time")
                                }
                                return@executes 1
                            }
                        )
                    )
                    .then(literal("ectopic")
                        .requires { source -> source.hasPermissionLevel(4) }
                        .executes { ctx ->
                            val source = ctx.source.entity
                            if (source is Pregnant) {
                                if (source.isEctopicPregnancy){
                                    source.sendMessage(Text.of("§c当前怀孕状态为宫外孕！"))
                                }else{
                                    source.sendMessage(Text.of("§a当前怀孕状态正常"))
                                }
                            }
                            return@executes 1
                        }
                        .then(literal("set")
                            .then(argument("is", BoolArgumentType.bool())
                                .executes { context ->
                                    val source = context.source.entity
                                    if (source is Pregnant) {
                                        source.isEctopicPregnancy = BoolArgumentType.getBool(context,"is")
                                    }
                                    return@executes 1
                                }
                            )
                        )
                    )
                )

                dispatcher.register(literal("menstruation")
                    .executes { context ->
                        val source = context.source.entity
                        if (source is Pregnant){
                            source.sendMessage(Text.of("当前处于${source.menstruationCycle.text}"))
                        }
                        return@executes 0
                    }
                    .then(literal("set")
                        .requires { source -> source.hasPermissionLevel(4) }
                        .then(argument("time", IntegerArgumentType.integer(0, Int.MAX_VALUE))
                            .executes {ctx ->
                                val source = ctx.source.entity
                                if (source is Pregnant) {
                                    source.menstruation = IntegerArgumentType.getInteger(ctx,"time")
                                }
                                return@executes 1
                            }
                        )
                    )
                    .then(literal("comfort")
                        .executes { context ->
                            val source = context.source.entity
                            if (source is Pregnant) {
                                source.sendMessage(Text.of("卫生巾剩余有效时间：${source.menstruationComfort/20}秒"))
                            }
                            return@executes 1
                        }
                    )
                )

                dispatcher.register(literal("sterilization")
                    .executes {context ->
                        val source = context.source.entity
                        if (source is Pregnant){
                            if (source.isSterilization){
                                source.sendMessage(Text.of("§c当前处于绝育状态中"))
                            }else{
                                source.sendMessage(Text.of("§a当前处于非绝育状态"))
                            }
                        }
                        return@executes 1
                    }
                    .then(literal("set")
                        .requires { source -> source.hasPermissionLevel(4) }
                        .then(argument("is", BoolArgumentType.bool())
                            .executes {ctx ->
                                val source = ctx.source.entity
                                if (source is Pregnant) {
                                    source.isSterilization = BoolArgumentType.getBool(ctx,"is")
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