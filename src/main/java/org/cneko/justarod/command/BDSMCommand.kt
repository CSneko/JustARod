package org.cneko.justarod.command

import com.mojang.brigadier.arguments.IntegerArgumentType
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.text.Text
import org.cneko.justarod.entity.BDSMable

class BDSMCommand {
    companion object {
        fun init() {
            CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
                dispatcher.register(literal("bdsm")
                    .then(literal("ballmouth")
                        .executes { context ->
                            val entity = context.source.entity
                            if (entity is BDSMable){
                                entity.sendMessage(Text.of("§a口球剩余时间：${entity.ballMouth/20}秒~"))
                            }
                            return@executes 1
                        }
                        .then(argument("target", EntityArgumentType.entity())
                            .executes { context ->
                                val source = context.source
                                val target = EntityArgumentType.getEntity(context, "target")
                                if (target is BDSMable){
                                    source.sendMessage(Text.of("§a对方口球剩余时间：${target.ballMouth/20}秒~"))
                                }
                                return@executes 1
                            }
                        )
                        .then(literal("set")
                            .then(argument("time", IntegerArgumentType.integer())
                                .executes { context ->
                                    val source = context.source.entity
                                    if (source is BDSMable){
                                        val time = IntegerArgumentType.getInteger(context, "time")
                                        source.ballMouth = time
                                    }
                                    return@executes 1
                                }
                                .then(argument("target", EntityArgumentType.entity())
                                    .executes { context ->
                                        val target = EntityArgumentType.getEntity(context, "target")
                                        if (target is BDSMable){
                                            val time = IntegerArgumentType.getInteger(context, "time")
                                            target.ballMouth = time
                                        }
                                        return@executes 1
                                    }
                                )
                            )
                        )
                    )
                    .then(literal("ElectricShock")
                        .executes { context ->
                            val source = context.source.entity
                            if (source is BDSMable){
                                source.sendMessage(Text.of("§a电击剩余时间：${source.electricShock/20}秒~"))
                            }
                            return@executes 1
                        }
                        .then(argument("target", EntityArgumentType.entity())
                            .executes { context ->
                                val target = EntityArgumentType.getEntity(context, "target")
                                if (target is BDSMable){
                                    target.sendMessage(Text.of("§a电击剩余时间：${target.electricShock/20}秒~"))
                                }
                                return@executes 1
                            }
                        )
                        .then(literal("set")
                            .then(argument("time",IntegerArgumentType.integer())
                                .executes{ context ->
                                    val source = context.source.entity
                                    if (source is BDSMable){
                                        val time = IntegerArgumentType.getInteger(context, "time")
                                        source.electricShock = time
                                    }
                                    return@executes 1
                                }
                                .then(argument("target", EntityArgumentType.entity())
                                    .executes { context ->
                                        val target = EntityArgumentType.getEntity(context, "target")
                                        if (target is BDSMable){
                                            val time = IntegerArgumentType.getInteger(context, "time")
                                            target.electricShock = time
                                        }
                                        return@executes 1
                                    }
                                )
                            )
                        )
                    )

                    .then(literal("bundled")
                        .executes { context ->
                            val source = context.source.entity
                            if (source is BDSMable){
                                source.sendMessage(Text.of("§a捆绑剩余时间：${source.bundled/20}秒~"))
                            }
                            return@executes 1
                        }
                        .then(argument("target", EntityArgumentType.entity())
                            .executes { context ->
                                val target = EntityArgumentType.getEntity(context, "target")
                                if (target is BDSMable){
                                    target.sendMessage(Text.of("§a捆绑剩余时间：${target.bundled/20}秒~"))
                                }
                                return@executes 1
                            }
                        )
                        .then(literal("set")
                            .then(argument("time", IntegerArgumentType.integer())
                                .executes { context ->
                                    val source = context.source.entity
                                    if (source is BDSMable){
                                        val time = IntegerArgumentType.getInteger(context, "time")
                                        source.bundled = time
                                    }
                                    return@executes 1
                                }
                                .then(argument("target", EntityArgumentType.entity())
                                    .executes { context ->
                                        val target = EntityArgumentType.getEntity(context, "target")
                                        if (target is BDSMable){
                                            val time = IntegerArgumentType.getInteger(context, "time")
                                            target.bundled = time
                                        }
                                        return@executes 1
                                    }
                                )
                            )
                        )
                    )

                    .then(literal("EyePatch")
                        .executes { context ->
                            val entity = context.source.entity
                            if (entity is BDSMable){
                                entity.sendMessage(Text.of("§a眼罩剩余时间：${entity.eyePatch/20}秒~"))
                            }
                            return@executes 1
                        }
                        .then(argument("target", EntityArgumentType.entity())
                            .executes { context ->
                                val target = EntityArgumentType.getEntity(context, "target")
                                if (target is BDSMable){
                                    target.sendMessage(Text.of("§a眼罩剩余时间：${target.eyePatch/20}秒~"))
                                }
                                return@executes 1
                            }
                        )
                        .then(literal("set")
                            .then(argument("time", IntegerArgumentType.integer())
                                .executes { context ->
                                    val source = context.source.entity
                                    if (source is BDSMable){
                                        val time = IntegerArgumentType.getInteger(context, "time")
                                        source.eyePatch = time
                                    }
                                    return@executes 1
                                }
                                .then(argument("target", EntityArgumentType.entity())
                                    .executes { context ->
                                        val target = EntityArgumentType.getEntity(context, "target")
                                        if (target is BDSMable){
                                            val time = IntegerArgumentType.getInteger(context, "time")
                                            target.eyePatch = time
                                        }
                                        return@executes 1
                                    }
                                )
                            )
                        )
                    )

                    .then(literal("earplug")
                        .executes { context ->
                            val entity = context.source.entity
                            if (entity is BDSMable){
                                entity.sendMessage(Text.of("§a耳塞剩余时间：${entity.earplug/20}秒~"))
                            }
                            return@executes 1
                        }
                        .then(argument("target", EntityArgumentType.entity())
                            .executes { context ->
                                val target = EntityArgumentType.getEntity(context, "target")
                                if (target is BDSMable){
                                    target.sendMessage(Text.of("§a耳塞剩余时间：${target.earplug/20}秒~"))
                                }
                                return@executes 1
                            }
                        )
                        .then(literal("set")
                            .then(argument("time", IntegerArgumentType.integer())
                                .executes { context ->
                                    val source = context.source.entity
                                    if (source is BDSMable){
                                        val time = IntegerArgumentType.getInteger(context, "time")
                                        source.earplug = time
                                    }
                                    return@executes 1
                                }
                                .then(argument("target", EntityArgumentType.entity())
                                    .executes { context ->
                                        val target = EntityArgumentType.getEntity(context, "target")
                                        if (target is BDSMable){
                                            val time = IntegerArgumentType.getInteger(context, "time")
                                            target.earplug = time
                                        }
                                        return@executes 1
                                    }
                                )
                            )
                        )

                    )

                )
            }
        }
    }
}