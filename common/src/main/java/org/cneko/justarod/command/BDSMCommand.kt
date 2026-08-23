package org.cneko.justarod.command

import com.mojang.brigadier.arguments.IntegerArgumentType
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.literal
import net.minecraft.network.chat.Component
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
                                entity.sendSystemMessage(Component.literal("§a口球剩余时间：${entity.ballMouth/20}秒~"))
                            }
                            return@executes 1
                        }
                        .then(argument("target", EntityArgument.entity())
                            .executes { context ->
                                val source = context.source
                                val target = EntityArgument.getEntity(context, "target")
                                if (target is BDSMable){
                                    source.sendSystemMessage(Component.literal("§a对方口球剩余时间：${target.ballMouth/20}秒~"))
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
                                .then(argument("target", EntityArgument.entity())
                                    .executes { context ->
                                        val target = EntityArgument.getEntity(context, "target")
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
                                source.sendSystemMessage(Component.literal("§a电击剩余时间：${source.electricShock/20}秒~"))
                            }
                            return@executes 1
                        }
                        .then(argument("target", EntityArgument.entity())
                            .executes { context ->
                                val target = EntityArgument.getEntity(context, "target")
                                if (target is BDSMable){
                                    target.sendSystemMessage(Component.literal("§a电击剩余时间：${target.electricShock/20}秒~"))
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
                                .then(argument("target", EntityArgument.entity())
                                    .executes { context ->
                                        val target = EntityArgument.getEntity(context, "target")
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
                                source.sendSystemMessage(Component.literal("§a捆绑剩余时间：${source.bundled/20}秒~"))
                            }
                            return@executes 1
                        }
                        .then(argument("target", EntityArgument.entity())
                            .executes { context ->
                                val target = EntityArgument.getEntity(context, "target")
                                if (target is BDSMable){
                                    target.sendSystemMessage(Component.literal("§a捆绑剩余时间：${target.bundled/20}秒~"))
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
                                .then(argument("target", EntityArgument.entity())
                                    .executes { context ->
                                        val target = EntityArgument.getEntity(context, "target")
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
                                entity.sendSystemMessage(Component.literal("§a眼罩剩余时间：${entity.eyePatch/20}秒~"))
                            }
                            return@executes 1
                        }
                        .then(argument("target", EntityArgument.entity())
                            .executes { context ->
                                val target = EntityArgument.getEntity(context, "target")
                                if (target is BDSMable){
                                    target.sendSystemMessage(Component.literal("§a眼罩剩余时间：${target.eyePatch/20}秒~"))
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
                                .then(argument("target", EntityArgument.entity())
                                    .executes { context ->
                                        val target = EntityArgument.getEntity(context, "target")
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
                                entity.sendSystemMessage(Component.literal("§a耳塞剩余时间：${entity.earplug/20}秒~"))
                            }
                            return@executes 1
                        }
                        .then(argument("target", EntityArgument.entity())
                            .executes { context ->
                                val target = EntityArgument.getEntity(context, "target")
                                if (target is BDSMable){
                                    target.sendSystemMessage(Component.literal("§a耳塞剩余时间：${target.earplug/20}秒~"))
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
                                .then(argument("target", EntityArgument.entity())
                                    .executes { context ->
                                        val target = EntityArgument.getEntity(context, "target")
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

                    .then(literal("handcuffed")
                        .executes { context ->
                            val entity = context.source.entity
                            if (entity is BDSMable){
                                entity.sendSystemMessage(Component.literal("§a手铐剩余时间：${entity.handcuffed/20}秒~"))
                            }
                            return@executes 1
                        }
                        .then(argument("target", EntityArgument.entity())
                            .executes { context ->
                                val target = EntityArgument.getEntity(context, "target")
                                if (target is BDSMable){
                                    target.sendSystemMessage(Component.literal("§a手铐剩余时间：${target.handcuffed/20}秒~"))
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
                                        source.handcuffed = time
                                    }
                                    return@executes 1
                                }
                                .then(argument("target", EntityArgument.entity())
                                    .executes { context ->
                                        val target = EntityArgument.getEntity(context, "target")
                                        if (target is BDSMable){
                                            val time = IntegerArgumentType.getInteger(context, "time")
                                            target.handcuffed = time
                                        }
                                        return@executes 1
                                    }
                                )
                            )
                        )
                    )
                    .then(literal("shackled")
                        .executes { context ->
                            val entity = context.source.entity
                            if (entity is BDSMable){
                                entity.sendSystemMessage(Component.literal("§a脚镣剩余时间：${entity.shackled/20}秒~"))
                            }
                            return@executes 1
                        }
                        .then(argument("target", EntityArgument.entity())
                            .executes { context ->
                                val target = EntityArgument.getEntity(context, "target")
                                if (target is BDSMable){
                                    target.sendSystemMessage(Component.literal("§a脚镣剩余时间：${target.shackled/20}秒~"))
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
                                        source.shackled = time
                                    }
                                    return@executes 1
                                }
                                .then(argument("target", EntityArgument.entity())
                                    .executes { context ->
                                        val target = EntityArgument.getEntity(context, "target")
                                        if (target is BDSMable){
                                            val time = IntegerArgumentType.getInteger(context, "time")
                                            target.shackled = time
                                        }
                                        return@executes 1
                                    }
                                )
                            )
                        )
                    )
                    .then(literal("noMatingPlz")
                        .executes { context ->
                            val entity = context.source.entity
                            if (entity is BDSMable){
                                entity.sendSystemMessage(Component.literal("§a禁交剩余时间：${entity.noMatingPlz / 20}秒~"))
                            }
                            return@executes 1
                        }
                        .then(argument("target", EntityArgument.entity())
                            .executes { context ->
                                val target = EntityArgument.getEntity(context, "target")
                                if (target is BDSMable){
                                    context.source.sendSystemMessage(Component.literal("§a对方禁交剩余时间：${target.noMatingPlz / 20}秒~"))
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
                                        source.noMatingPlz = time
                                        source.sendSystemMessage(Component.literal("§a已设置禁交时间：${time / 20}秒~"))
                                    }
                                    return@executes 1
                                }
                                .then(argument("target", EntityArgument.entity())
                                    .executes { context ->
                                        val target = EntityArgument.getEntity(context, "target")
                                        if (target is BDSMable){
                                            val time = IntegerArgumentType.getInteger(context, "time")
                                            target.noMatingPlz = time
                                            context.source.sendSystemMessage(Component.literal("§a已为目标设置禁交时间：${time / 20}秒~"))
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