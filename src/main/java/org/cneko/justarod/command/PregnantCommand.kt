package org.cneko.justarod.command

import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.argument.TextArgumentType
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
                    .then(literal("status")
                        .requires { source -> source.hasPermissionLevel(4) }
                        .executes { ctx ->
                            val source = ctx.source.entity
                            if (source is Pregnant) {
                                if (source.isEctopicPregnancy){
                                    source.sendMessage(Text.of("§c当前怀孕状态为宫外孕！"))
                                } else if (source.isHydatidiformMole){
                                    source.sendMessage(Text.of("§c当前怀孕状态为葡萄胎！"))
                                } else{
                                    source.sendMessage(Text.of("§a当前怀孕状态正常"))
                                }
                            }
                            return@executes 1
                        }
                        .then(literal("set")
                            .then(argument("type", StringArgumentType.word())
                                .then(argument("is", BoolArgumentType.bool())
                                    .executes { context ->
                                        val source = context.source.entity
                                        if (source is Pregnant) {
                                            val type = StringArgumentType.getString(context,"type")
                                            if (type.contains("ect")) {
                                                source.isEctopicPregnancy = BoolArgumentType.getBool(context, "is")
                                            }else if (type.contains("hyd") || type.contains("mole")){
                                                source.isHydatidiformMole = BoolArgumentType.getBool(context, "is")
                                            }
                                        }
                                        return@executes 1
                                    }
                                )
                            )
                        )
                    )
                    .then(literal("count")
                        .executes { context ->
                            val source = context.source.entity
                            if (source is Pregnant && source.isPregnant) {
                                source.sendMessage(Text.of("§a你怀了${source.babyCount}胞胎！"))
                            }
                            return@executes 1
                        }
                        .then(literal("set")
                            .then(argument("count", IntegerArgumentType.integer(0, Int.MAX_VALUE))
                                .executes { context ->
                                    val source = context.source.entity
                                    if (source is Pregnant) {
                                        source.babyCount = IntegerArgumentType.getInteger(context, "count")
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
                dispatcher.register(literal("aids")
                    .executes { ctx ->
                        val source = ctx.source.entity
                        if (source is Pregnant){
                            source.sendMessage(Text.of("当前AIDS状态：${if (source.aids > 0) "已经感染${source.aids}" else "没有感染艾滋哦"}"))
                        }
                        return@executes 1
                    }
                    .then(literal("set")
                        .requires { source -> source.hasPermissionLevel(4) }
                        .then(argument("time", IntegerArgumentType.integer(0, Int.MAX_VALUE))
                            .executes {ctx ->
                                val source = ctx.source.entity
                                if (source is Pregnant) {
                                    source.aids = IntegerArgumentType.getInteger(ctx,"time")
                                }
                                return@executes 1
                            }
                        )
                    )
                )
                dispatcher.register(literal("hpv")
                    .executes { ctx ->
                        val source = ctx.source.entity
                        if (source is Pregnant){
                            source.sendMessage(Text.of("当前HPV状态：${if (source.hpv > 0) "已经感染${source.hpv}" else "没有感染HPV哦"}"))
                        }
                        return@executes 1
                    }
                    .then(literal("set")
                        .requires { source -> source.hasPermissionLevel(4) }
                        .then(argument("time", IntegerArgumentType.integer(0, Int.MAX_VALUE))
                            .executes {ctx ->
                                val source = ctx.source.entity
                                if (source is Pregnant) {
                                    source.hpv = IntegerArgumentType.getInteger(ctx,"time")
                                }
                                return@executes 1
                            }
                        )
                        .then(literal("immune")
                            .executes { ctx ->
                                val source = ctx.source.entity
                                if (source is Pregnant) {
                                    source.sendMessage(Text.of("当前HPV免疫状态：${if (source.isImmune2HPV) "已免疫" else "没有免疫"}"))
                                }
                                return@executes 1
                            }
                            .then(literal("set")
                                .requires { source -> source.hasPermissionLevel(4) }
                                .then(argument("is", BoolArgumentType.bool())
                                    .executes {ctx ->
                                        val source = ctx.source.entity
                                        if (source is Pregnant) {
                                            source.isImmune2HPV = BoolArgumentType.getBool(ctx,"is")
                                        }
                                        return@executes 1
                                    }
                                )
                            )
                        )
                    )
                )
                dispatcher.register(literal("hysterectomy")
                    .executes {context ->
                        val source = context.source.entity
                        if (source is Pregnant){
                            source.sendMessage(Text.of("§a子宫切除状态：${if (source.isHysterectomy) "§c已切除" else "§b未切除"}"))
                        }
                        return@executes 1
                    }
                    .then(literal("set")
                        .requires { source -> source.hasPermissionLevel(4) }
                        .then(argument("is", BoolArgumentType.bool())
                            .executes {ctx ->
                                val source = ctx.source.entity
                                if (source is Pregnant) {
                                    source.isHysterectomy = BoolArgumentType.getBool(ctx,"is")
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