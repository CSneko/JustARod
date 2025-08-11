package org.cneko.justarod.command

import com.mojang.brigadier.arguments.IntegerArgumentType
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.text.Text
import org.cneko.justarod.entity.BallMouthable
import org.cneko.justarod.entity.Pregnant

class BallMouthCommand {
    companion object {
        fun init() {
            CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
                dispatcher.register(literal("ballmouth")
                    .executes { context ->
                        val entity = context.source.entity
                        if (entity is BallMouthable){
                            entity.sendMessage(Text.of("§a口球剩余时间：${entity.ballMouth/20}秒~"))
                        }
                        return@executes 1
                    }
                    .then(argument("target", EntityArgumentType.entity())
                        .executes { context ->
                            val source = context.source
                            val target = EntityArgumentType.getEntity(context, "target")
                            if (target is BallMouthable){
                                source.sendMessage(Text.of("§a对方口球剩余时间：${target.ballMouth/20}秒~"))
                            }
                            return@executes 1
                        }
                    )
                    .then(literal("set")
                        .then(argument("time", IntegerArgumentType.integer())
                            .executes { context ->
                                val source = context.source.entity
                                if (source is BallMouthable){
                                    val time = IntegerArgumentType.getInteger(context, "time")
                                    source.ballMouth = time
                                }
                                return@executes 1
                            }
                            .then(argument("target", EntityArgumentType.entity())
                                .executes { context ->
                                    val target = EntityArgumentType.getEntity(context, "target")
                                    if (target is BallMouthable){
                                        val time = IntegerArgumentType.getInteger(context, "time")
                                        target.ballMouth = time
                                    }
                                    return@executes 1
                                }
                            )
                        )
                    )

                )
            }
        }
    }
}