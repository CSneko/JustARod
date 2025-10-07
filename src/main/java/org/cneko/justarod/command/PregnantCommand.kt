package org.cneko.justarod.command

import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.nbt.NbtCompound
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.text.Text
import org.cneko.justarod.entity.Pregnant
import org.cneko.justarod.item.JRItems

class PregnantCommand {
    companion object{
        fun init(){
            CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->

                dispatcher.register(literal("sex")
                    .executes { context ->
                        val source = context.source.entity
                        if (source is Pregnant){
                            val isMale = source.isMale
                            val isFemale = source.isFemale
                            source.sendMessage(Text.of("§a你的性别为：${if (isMale && isFemale) "§b男§d女" else if (isMale) "§b男" else if(isFemale) "§d女" else "无"}"))
                        }
                        return@executes 1
                    }
                    .then(argument("target", EntityArgumentType.entity())
                        .executes { context ->
                            val source = context.source
                            val target = EntityArgumentType.getEntity(context, "target")
                            if (target is Pregnant){
                                val isMale = target.isMale
                                val isFemale = target.isFemale
                                source.sendMessage(Text.of("§a对方性别为：${if (isMale && isFemale) "§b男§d女" else if (isMale) "§b男" else if(isFemale) "§d女" else "无"}"))
                            }
                            return@executes 1
                        }
                    )
                    .then(literal("male")
                        .then(argument("is",BoolArgumentType.bool())
                            .executes { ctx ->
                                val source = ctx.source.entity
                                if (source is Pregnant){
                                    val isMale = BoolArgumentType.getBool(ctx,"is")
                                    source.isMale = isMale
                                }
                                return@executes 1
                            }
                            .then(argument("target", EntityArgumentType.entity())
                                .executes { ctx ->
                                    val target = EntityArgumentType.getEntity(ctx, "target")
                                    if (target is Pregnant){
                                        val isMale = BoolArgumentType.getBool(ctx,"is")
                                        target.isMale = isMale
                                    }
                                    return@executes 1
                                }
                            )
                        )

                    )
                    .then(literal("female")
                        .then(argument("is",BoolArgumentType.bool())
                            .executes { ctx ->
                                val source = ctx.source.entity
                                if (source is Pregnant){
                                    val isFemale = BoolArgumentType.getBool(ctx,"is")
                                    source.isFemale = isFemale
                                }
                                return@executes 1
                            }
                            .then(argument("target", EntityArgumentType.entity())
                                .executes { ctx ->
                                    val target = EntityArgumentType.getEntity(ctx, "target")
                                    if (target is Pregnant){
                                        val isFemale = BoolArgumentType.getBool(ctx,"is")
                                        target.isFemale = isFemale
                                    }
                                    return@executes 1
                                }
                            )
                        )

                    )
                )
                dispatcher.register(literal("pregnant")
                    .executes {ctx ->
                        val source = ctx.source.entity
                        if (source is Pregnant){
                            source.sendMessage(Text.of("剩余孕期：${source.pregnant/20/60/20}天"))
                        }
                        return@executes 1
                    }
                    .then(argument("target", EntityArgumentType.entity())
                        .executes { ctx ->
                            val source = ctx.source
                            val target = EntityArgumentType.getEntity(ctx, "target")
                            if (target is Pregnant){
                                source.sendMessage(Text.of("剩余孕期：${target.pregnant/20/60/20}天"))
                            }
                            return@executes 1
                        }
                    )
                    .then(literal("set")
                        .then(argument("time",IntegerArgumentType.integer(0, Int.MAX_VALUE))
                            .requires { source -> source.hasPermissionLevel(4) }
                            .executes { ctx->
                                val source = ctx.source.entity
                                if (source is Pregnant) {
                                    source.pregnant = IntegerArgumentType.getInteger(ctx,"time")
                                }
                                return@executes 1
                            }.then(argument("target",EntityArgumentType.entity())
                                .executes { ctx ->
                                    val target = EntityArgumentType.getEntity(ctx, "target")
                                    if (target is Pregnant) {
                                        target.pregnant = IntegerArgumentType.getInteger(ctx,"time")
                                    }
                                    return@executes 1
                                }
                            )
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
                        .then(argument("target", EntityArgumentType.entity())
                            .executes { ctx ->
                                val source = ctx.source
                                val target = EntityArgumentType.getEntity(ctx, "target")
                                if (target is Pregnant) {
                                    if (target.isEctopicPregnancy){
                                        source.sendMessage(Text.of("§c当前怀孕状态为宫外孕！"))
                                    } else if (target.isHydatidiformMole){
                                        source.sendMessage(Text.of("§c当前怀孕状态为葡萄胎！"))
                                    } else{
                                        source.sendMessage(Text.of("§a当前怀孕状态正常"))
                                    }
                                }
                                return@executes 1
                            }
                        )
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
                                    .then(argument("target", EntityArgumentType.entity())
                                        .executes { context ->
                                            val target = EntityArgumentType.getEntity(context, "target")
                                            if (target is Pregnant) {
                                                val type = StringArgumentType.getString(context,"type")
                                                if (type.contains("ect")) {
                                                    target.isEctopicPregnancy = BoolArgumentType.getBool(context, "is")
                                                }else if (type.contains("hyd") || type.contains("mole")){
                                                    target.isHydatidiformMole = BoolArgumentType.getBool(context, "is")
                                                }
                                            }
                                            return@executes 1
                                        }
                                    )
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
                        .then(argument("target", EntityArgumentType.entity())
                            .executes { context ->
                                val source = context.source
                                val target = EntityArgumentType.getEntity(context, "target")
                                if (target is Pregnant && target.isPregnant) {
                                    source.sendMessage(Text.of("§a目标怀了${target.babyCount}胞胎！"))
                                }
                                return@executes 1
                            }
                        )
                        .then(literal("set")
                            .then(argument("count", IntegerArgumentType.integer(0, Int.MAX_VALUE))
                                .executes { context ->
                                    val source = context.source.entity
                                    if (source is Pregnant) {
                                        source.babyCount = IntegerArgumentType.getInteger(context, "count")
                                    }
                                    return@executes 1
                                }
                                .then(argument("target", EntityArgumentType.entity())
                                    .executes { context ->
                                        val target = EntityArgumentType.getEntity(context, "target")
                                        if (target is Pregnant) {
                                            target.babyCount = IntegerArgumentType.getInteger(context, "count")
                                        }
                                        return@executes 1
                                    }
                                )
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
                    .then(argument("target", EntityArgumentType.entity())
                        .executes { context ->
                            val source = context.source
                            val target = EntityArgumentType.getEntity(context, "target")
                            if (target is Pregnant){
                                source.sendMessage(Text.of("当前处于${target.menstruationCycle.text}"))
                            }
                            return@executes 1
                        }
                    )
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
                            .then(argument("target", EntityArgumentType.entity())
                                .executes {ctx ->
                                    val target = EntityArgumentType.getEntity(ctx, "target")
                                    if (target is Pregnant) {
                                        target.menstruation = IntegerArgumentType.getInteger(ctx,"time")
                                    }
                                    return@executes 1
                                }
                            )
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
                        .then(argument("target", EntityArgumentType.entity())
                            .executes { context ->
                                val source = context.source
                                val target = EntityArgumentType.getEntity(context, "target")
                                if (target is Pregnant) {
                                    source.sendMessage(Text.of("卫生巾剩余有效时间：${target.menstruationComfort/20}秒"))
                                }
                                return@executes 1
                            }
                        )
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
                    .then(argument("target", EntityArgumentType.entity())
                        .executes {context ->
                            val source = context.source
                            val target = EntityArgumentType.getEntity(context, "target")
                            if (target is Pregnant){
                                if (target.isSterilization){
                                    source.sendMessage(Text.of("§c当前处于绝育状态中"))
                                }else{
                                    source.sendMessage(Text.of("§a当前处于非绝育状态"))
                                }
                            }
                            return@executes 1
                        }
                    )
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
                            .then(argument("target", EntityArgumentType.entity())
                                .executes {ctx ->
                                    val target = EntityArgumentType.getEntity(ctx, "target")
                                    if (target is Pregnant) {
                                        target.isSterilization = BoolArgumentType.getBool(ctx,"is")
                                    }
                                    return@executes 1
                                }
                            )
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
                    .then(argument("target", EntityArgumentType.entity())
                        .executes { ctx ->
                            val source = ctx.source
                            val target = EntityArgumentType.getEntity(ctx, "target")
                            if (target is Pregnant){
                                source.sendMessage(Text.of("当前AIDS状态：${if (target.aids > 0) "已经感染${target.aids}" else "没有感染艾滋哦"}"))
                            }
                            return@executes 1
                        }
                    )
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
                            .then(argument("target", EntityArgumentType.entity())
                                .executes {ctx ->
                                    val target = EntityArgumentType.getEntity(ctx, "target")
                                    if (target is Pregnant) {
                                        target.aids = IntegerArgumentType.getInteger(ctx,"time")
                                    }
                                    return@executes 1
                                }
                            )
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
                    .then(argument("target", EntityArgumentType.entity())
                        .executes { ctx ->
                            val source = ctx.source
                            val target = EntityArgumentType.getEntity(ctx, "target")
                            if (target is Pregnant){
                                source.sendMessage(Text.of("当前HPV状态：${if (target.hpv > 0) "已经感染${target.hpv}" else "没有感染HPV哦"}"))
                            }
                            return@executes 1
                        }
                    )
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
                            .then(argument("target", EntityArgumentType.entity())
                                .executes {ctx ->
                                    val target = EntityArgumentType.getEntity(ctx, "target")
                                    if (target is Pregnant) {
                                        target.hpv = IntegerArgumentType.getInteger(ctx,"time")
                                    }
                                    return@executes 1
                                }
                            )
                        )
                        .then(literal("immune")
                            .executes { ctx ->
                                val source = ctx.source.entity
                                if (source is Pregnant) {
                                    source.sendMessage(Text.of("当前HPV免疫状态：${if (source.isImmune2HPV) "已免疫" else "没有免疫"}"))
                                }
                                return@executes 1
                            }
                            .then(argument("target", EntityArgumentType.entity())
                                .executes { ctx ->
                                    val source = ctx.source
                                    val target = EntityArgumentType.getEntity(ctx, "target")
                                    if (target is Pregnant) {
                                        source.sendMessage(Text.of("当前HPV免疫状态：${if (target.isImmune2HPV) "已免疫" else "没有免疫"}"))
                                    }
                                    return@executes 1
                                }
                            )
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
                                    .then(argument("target", EntityArgumentType.entity())
                                        .executes {ctx ->
                                            val target = EntityArgumentType.getEntity(ctx, "target")
                                            if (target is Pregnant) {
                                                target.isImmune2HPV = BoolArgumentType.getBool(ctx,"is")
                                            }
                                            return@executes 1
                                        }
                                    )
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
                    .then(argument("target", EntityArgumentType.entity())
                        .executes {context ->
                            val source = context.source
                            val target = EntityArgumentType.getEntity(context, "target")
                            if (target is Pregnant){
                                source.sendMessage(Text.of("§a子宫切除状态：${if (target.isHysterectomy) "§c已切除" else "§b未切除"}"))
                            }
                            return@executes 1
                        }
                    )
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
                            .then(argument("target", EntityArgumentType.entity())
                                .executes {ctx ->
                                    val target = EntityArgumentType.getEntity(ctx, "target")
                                    if (target is Pregnant) {
                                        target.isHysterectomy = BoolArgumentType.getBool(ctx,"is")
                                    }
                                    return@executes 1
                                }
                            )
                        )
                    )
                )

                dispatcher.register(literal("PCOS")
                    .executes { context ->
                        val source = context.source.entity
                        if (source is Pregnant){
                            source.sendMessage(Text.of("当前多囊卵巢综合征状态：${if (source.isPCOS) "§c已患上" else "§a没有患上"}"))
                        }
                        return@executes 1
                    }
                    .then(argument("target", EntityArgumentType.entity())
                        .executes { context ->
                            val source = context.source
                            val target = EntityArgumentType.getEntity(context, "target")
                            if (target is Pregnant){
                                source.sendMessage(Text.of("当前多囊卵巢综合征状态：${if (target.isPCOS) "§c已患上" else "§a没有患上"}"))
                            }
                            return@executes 1
                        }
                    )
                    .then(literal("set")
                        .requires { source -> source.hasPermissionLevel(4) }
                        .then(argument("is", BoolArgumentType.bool())
                            .executes {ctx ->
                                val source = ctx.source.entity
                                if (source is Pregnant) {
                                    source.isPCOS = BoolArgumentType.getBool(ctx,"is")
                                }
                                return@executes 1
                            }
                            .then(argument("target", EntityArgumentType.entity())
                                .executes {ctx ->
                                    val target = EntityArgumentType.getEntity(ctx, "target")
                                    if (target is Pregnant) {
                                        target.isPCOS = BoolArgumentType.getBool(ctx,"is")
                                    }
                                    return@executes 1
                                }
                            )
                        )
                    )
                )

                dispatcher.register(literal("BrithControlling")
                    .executes { context ->
                        val source = context.source.entity
                        if (source is Pregnant){
                            source.sendMessage(Text.of("剩余避孕有效期：${source.brithControlling}"))
                        }
                        return@executes 1
                    }
                    .then(argument("target", EntityArgumentType.entity())
                        .executes { context ->
                            val source = context.source
                            val target = EntityArgumentType.getEntity(context, "target")
                            if (target is Pregnant){
                                source.sendMessage(Text.of("剩余避孕有效期：${target.brithControlling}"))
                            }
                            return@executes 1
                        }
                    )
                    .then(literal("set")
                        .requires { source -> source.hasPermissionLevel(4) }
                        .then(argument("time", IntegerArgumentType.integer(0, Int.MAX_VALUE))
                            .executes {ctx ->
                                val source = ctx.source.entity
                                if (source is Pregnant) {
                                    source.brithControlling = IntegerArgumentType.getInteger(ctx,"time")
                                }
                                return@executes 1
                            }
                            .then(argument("target", EntityArgumentType.entity())
                                .executes {ctx ->
                                    val target = EntityArgumentType.getEntity(ctx, "target")
                                    if (target is Pregnant) {
                                        target.brithControlling = IntegerArgumentType.getInteger(ctx,"time")
                                    }
                                    return@executes 1
                                }
                            )
                        )
                    )
                )
                dispatcher.register(literal("OvarianCancer")
                    .executes { context ->
                        val source = context.source.entity
                        if (source is Pregnant){
                            source.sendMessage(Text.of("当前卵巢癌状态：${if (source.ovarianCancer>0) "§c已患上" else "§a没有患上"}"))
                        }
                        return@executes 1
                    }
                    .then(argument("target", EntityArgumentType.entity())
                        .executes { context ->
                            val source = context.source
                            val target = EntityArgumentType.getEntity(context, "target")
                            if (target is Pregnant){
                                source.sendMessage(Text.of("当前卵巢癌状态：${if (target.ovarianCancer>0) "§c已患上" else "§a没有患上"}"))
                            }
                            return@executes 1
                        }
                    )
                    .then(literal("set")
                        .requires { source -> source.hasPermissionLevel(4) }
                        .then(argument("time", IntegerArgumentType.integer(0, Int.MAX_VALUE))
                            .executes {ctx ->
                                val source = ctx.source.entity
                                if (source is Pregnant) {
                                    source.ovarianCancer = IntegerArgumentType.getInteger(ctx,"time")
                                }
                                return@executes 1
                            }
                            .then(argument("target", EntityArgumentType.entity())
                                .executes {ctx ->
                                    val target = EntityArgumentType.getEntity(ctx, "target")
                                    if (target is Pregnant) {
                                        target.ovarianCancer = IntegerArgumentType.getInteger(ctx,"time")
                                    }
                                    return@executes 1
                                }
                            )
                        )
                    )
                )

                dispatcher.register(literal("BreastCancer")
                    .executes { context ->
                        val source = context.source.entity
                        if (source is Pregnant){
                            source.sendMessage(Text.of("当前乳腺癌状态：${if (source.breastCancer>0) "§c已患上" else "§a没有患上"}"))
                        }
                        return@executes 1
                    }
                    .then(argument("target", EntityArgumentType.entity())
                        .executes { context ->
                            val source = context.source
                            val target = EntityArgumentType.getEntity(context, "target")
                            if (target is Pregnant){
                                source.sendMessage(Text.of("当前乳腺癌状态：${if (target.breastCancer>0) "§c已患上" else "§a没有患上"}"))
                            }
                            return@executes 1
                        }
                    )
                    .then(literal("set")
                        .requires { source -> source.hasPermissionLevel(4) }
                        .then(argument("time", IntegerArgumentType.integer(0, Int.MAX_VALUE))
                            .executes {ctx ->
                                val source = ctx.source.entity
                                if (source is Pregnant) {
                                    source.breastCancer = IntegerArgumentType.getInteger(ctx,"time")
                                }
                                return@executes 1
                            }
                            .then(argument("target", EntityArgumentType.entity())
                                .executes {ctx ->
                                    val target = EntityArgumentType.getEntity(ctx, "target")
                                    if (target is Pregnant) {
                                        target.breastCancer = IntegerArgumentType.getInteger(ctx,"time")
                                    }
                                    return@executes 1
                                }
                            )
                        )
                    )
                )

                dispatcher.register(literal("syphilis")
                    .executes { context ->
                        val source = context.source.entity
                        if (source is Pregnant){
                            source.sendMessage(Text.of("当前梅毒状态：${if (source.syphilis>0) "§c已患上" else "§a没有患上"}"))
                        }
                        return@executes 1
                    }
                    .then(argument("target", EntityArgumentType.entity())
                        .executes { context ->
                            val source = context.source
                            val target = EntityArgumentType.getEntity(context, "target")
                            if (target is Pregnant){
                                source.sendMessage(Text.of("当前梅毒状态：${if (target.syphilis>0) "§c已患上" else "§a没有患上"}"))
                            }
                            return@executes 1
                        }
                    )
                    .then(literal("set")
                        .requires { source -> source.hasPermissionLevel(4) }
                        .then(argument("time", IntegerArgumentType.integer(0, Int.MAX_VALUE))
                            .executes {ctx ->
                                val source = ctx.source.entity
                                if (source is Pregnant) {
                                    source.syphilis = IntegerArgumentType.getInteger(ctx,"time")
                                }
                                return@executes 1
                            }
                            .then(argument("target", EntityArgumentType.entity())
                                .executes {ctx ->
                                    val target = EntityArgumentType.getEntity(ctx, "target")
                                    if (target is Pregnant) {
                                        target.syphilis = IntegerArgumentType.getInteger(ctx,"time")
                                    }
                                    return@executes 1
                                }
                            )
                        )
                    )
                )

                dispatcher.register(literal("excretion")
                    .then(literal("release")
                        .executes { context ->
                            val source = context.source.entity
                            if (source is Pregnant){
                                val exc = source.excretion
                                if (exc > 20*60*10){
                                    source.excretion -= 20*60*10
                                    source.sendMessage(Text.of("你排泄了"))
                                    source.dropStack(JRItems.EXCREMENT.defaultStack)
                                }else{
                                    source.sendMessage(Text.of("你目前无需排泄"))
                                }
                            }
                            return@executes 1
                        }
                    )
                    .executes { context ->
                        val source = context.source.entity
                        if (source is Pregnant){
                            source.sendMessage(Text.of("当前憋粑粑时间：${source.excretion / 20/60}分钟"))
                        }
                        return@executes 1
                    }
                    .then(argument("target", EntityArgumentType.entity())
                        .executes { context ->
                            val source = context.source
                            val target = EntityArgumentType.getEntity(context, "target")
                            if (target is Pregnant){
                                source.sendMessage(Text.of("当前憋粑粑时间：${target.excretion / 20/60}分钟"))
                            }
                            return@executes 1
                        }
                    )
                    .then(literal("set")
                        .requires { source -> source.hasPermissionLevel(4) }
                        .then(argument("time", IntegerArgumentType.integer(0, Int.MAX_VALUE))
                            .executes {ctx ->
                                val source = ctx.source.entity
                                if (source is Pregnant) {
                                    source.excretion = IntegerArgumentType.getInteger(ctx,"time")
                                }
                                return@executes 1
                            }
                            .then(argument("target", EntityArgumentType.entity())
                                .executes {ctx ->
                                    val target = EntityArgumentType.getEntity(ctx, "target")
                                    if (target is Pregnant) {
                                        target.excretion = IntegerArgumentType.getInteger(ctx,"time")
                                    }
                                    return@executes 1
                                }
                            )
                        )
                    )
                )

                dispatcher.register(literal("orchiectomy")
                    .executes {context ->
                        val source = context.source.entity
                        if (source is Pregnant){
                            source.sendMessage(Text.of("§a魔丸切除状态：${if (source.isOrchiectomy) "§c已切除" else "§b未切除"}"))
                        }
                        return@executes 1
                    }
                    .then(argument("target", EntityArgumentType.entity())
                        .executes {context ->
                            val source = context.source
                            val target = EntityArgumentType.getEntity(context, "target")
                            if (target is Pregnant){
                                source.sendMessage(Text.of("§a魔丸切除状态：${if (target.isOrchiectomy) "§c已切除" else "§b未切除"}"))
                            }
                            return@executes 1
                        }
                    )
                    .then(literal("set")
                        .requires { source -> source.hasPermissionLevel(4) }
                        .then(argument("is", BoolArgumentType.bool())
                            .executes {ctx ->
                                val source = ctx.source.entity
                                if (source is Pregnant) {
                                    source.isOrchiectomy = BoolArgumentType.getBool(ctx,"is")
                                }
                                return@executes 1
                            }
                            .then(argument("target", EntityArgumentType.entity())
                                .executes {ctx ->
                                    val target = EntityArgumentType.getEntity(ctx, "target")
                                    if (target is Pregnant) {
                                        target.isOrchiectomy = BoolArgumentType.getBool(ctx,"is")
                                    }
                                    return@executes 1
                                }
                            )
                        )
                    )
                )

                dispatcher.register(literal("amputated")
                    .executes {context ->
                        val source = context.source.entity
                        if (source is Pregnant){
                            source.sendMessage(Text.of("§a截肢状态：${if (source.isAmputated) "§c已截肢" else "§b未截肢"}"))
                        }
                        return@executes 1
                    }
                    .then(argument("target", EntityArgumentType.entity())
                        .executes {context ->
                            val source = context.source
                            val target = EntityArgumentType.getEntity(context, "target")
                            if (target is Pregnant){
                                source.sendMessage(Text.of("§a截肢状态：${if (target.isAmputated) "§c已截肢" else "§b截肢"}"))
                            }
                            return@executes 1
                        }
                    )
                    .then(literal("set")
                        .requires { source -> source.hasPermissionLevel(4) }
                        .then(argument("is", BoolArgumentType.bool())
                            .executes {ctx ->
                                val source = ctx.source.entity
                                if (source is Pregnant) {
                                    source.isAmputated = BoolArgumentType.getBool(ctx,"is")
                                }
                                return@executes 1
                            }
                            .then(argument("target", EntityArgumentType.entity())
                                .executes {ctx ->
                                    val target = EntityArgumentType.getEntity(ctx, "target")
                                    if (target is Pregnant) {
                                        target.isAmputated = BoolArgumentType.getBool(ctx,"is")
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
