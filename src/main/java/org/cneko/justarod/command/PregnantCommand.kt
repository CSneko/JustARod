package org.cneko.justarod.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.FloatArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.entity.LivingEntity
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import org.cneko.justarod.entity.Pregnant
import org.cneko.justarod.item.JRItems
import javax.swing.text.html.parser.Entity

class PregnantCommand {
    companion object {
        fun init() {
            CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
                registerSex(dispatcher)
                registerPregnant(dispatcher)
                registerMenstruation(dispatcher)
                registerExcretion(dispatcher)
                registerUrination(dispatcher)
                registerHymen(dispatcher)
                registerProtogyny(dispatcher)
                registerHormones(dispatcher)
                registerCorpusLuteumRupture(dispatcher)

                // 简单的布尔开关/状态类命令
                registerSimpleBool(dispatcher, "sterilization", "绝育", { it.isSterilization }, { e, v -> e.isSterilization = v })
                registerSimpleBool(dispatcher, "uterus", "子宫", { it.hasUterus() }, { e, v -> e.setHasUterus(v) }, trueStr = "有", falseStr = "没有")
                registerSimpleBool(dispatcher, "PCOS", "多囊卵巢综合征", { it.isPCOS }, { e, v -> e.isPCOS = v }, trueStr = "已患上", falseStr = "没有患上", trueColor = "c", falseColor = "a")
                registerSimpleBool(dispatcher, "orchiectomy", "魔丸切除", { it.isOrchiectomy }, { e, v -> e.isOrchiectomy = v }, trueStr = "已切除", falseStr = "未切除", trueColor = "c", falseColor = "b")
                registerSimpleBool(dispatcher, "amputated", "截肢", { it.isAmputated }, { e, v -> e.isAmputated = v }, trueStr = "已截肢", falseStr = "未截肢", trueColor = "c", falseColor = "b")

                // 简单的整数/时间/等级类命令
                registerSimpleInt(dispatcher, "BrithControlling", "避孕有效期", { it.brithControlling }, { e, v -> e.brithControlling = v })
                registerSimpleInt(dispatcher, "OvarianCancer", "卵巢癌", { it.ovarianCancer }, { e, v -> e.ovarianCancer = v }, isDisease = true)
                registerSimpleInt(dispatcher, "BreastCancer", "乳腺癌", { it.breastCancer }, { e, v -> e.breastCancer = v }, isDisease = true)
                registerSimpleInt(dispatcher, "syphilis", "梅毒", { it.syphilis }, { e, v -> e.syphilis = v }, isDisease = true)
                registerSimpleInt(dispatcher, "uterine_cold", "宫寒", { it.uterineCold }, { e, v -> e.uterineCold = v }, isDisease = true)
                registerSimpleInt(dispatcher, "urethritis", "尿道炎", { it.urethritis }, { e, v -> e.urethritis = v }, isDisease = true)
                registerSimpleInt(dispatcher, "prostatitis", "前列腺炎", { it.prostatitis }, { e, v -> e.prostatitis = v }, isDisease = true)
                registerSimpleInt(dispatcher, "hemorrhoids", "痔疮", { it.hemorrhoids }, { e, v -> e.hemorrhoids = v }, isDisease = true)
                registerSimpleInt(dispatcher, "cataract", "白内障", { it.cataract }, { e, v -> e.cataract = v }, isDisease = true)

                // 带有免疫功能的特殊疾病
                registerComplexDisease(dispatcher, "aids", "AIDS", { it.aids }, { e, v -> e.aids = v }, { it.isImmune2Aids }, { e, v -> e.isImmune2Aids = v })
                registerComplexDisease(dispatcher, "hpv", "HPV", { it.hpv }, { e, v -> e.hpv = v }, { it.isImmune2HPV }, { e, v -> e.isImmune2HPV = v })
            }
        }

        // ==================== 核心 Helper ====================

        /**
         * 核心执行逻辑：自动处理 self/target，自动检查 Pregnant 类型
         */
        private fun run(ctx: CommandContext<ServerCommandSource>, targetName: String? = null, action: (Pregnant, ServerCommandSource) -> Unit): Int {
            val source = ctx.source
            val entity = if (targetName != null) EntityArgumentType.getEntity(ctx, targetName) else source.entity

            if (entity is Pregnant) {
                action(entity, source)
            } else {
                // 可选：提示目标不是 Pregnant 实体
            }
            return 1
        }

        /**
         * 构建通用的 "查看状态" + "针对目标查看状态" 结构
         */
        private fun buildSelfAndTarget(
            builder: LiteralArgumentBuilder<ServerCommandSource>,
            action: (Pregnant, ServerCommandSource) -> Unit
        ): LiteralArgumentBuilder<ServerCommandSource> {
            return builder
                .executes { run(it, null, action) }
                .then(argument("target", EntityArgumentType.entity())
                    .executes { run(it, "target", action) }
                )
        }

        /**
         * 构建通用的 Set 命令 (需要 OP 权限)
         * 结构: ... set <argName> (Self) -> optional target
         */
        private fun <T> buildSetter(
            argName: String,
            argType: com.mojang.brigadier.arguments.ArgumentType<T>,
            getter: (CommandContext<ServerCommandSource>, String) -> T,
            setterAction: (Pregnant, T) -> Unit
        ): LiteralArgumentBuilder<ServerCommandSource> {
            return literal("set")
                .requires { s -> s.hasPermissionLevel(4) }
                .then(argument(argName, argType)
                    .executes { ctx ->
                        val value = getter(ctx, argName)
                        run(ctx, null) { p, _ -> setterAction(p, value) }
                    }
                    .then(argument("target", EntityArgumentType.entity())
                        .executes { ctx ->
                            val value = getter(ctx, argName)
                            run(ctx, "target") { p, _ -> setterAction(p, value) }
                        }
                    )
                )
        }

        // ==================== 通用注册器 ====================

        private fun registerSimpleBool(
            dispatcher: CommandDispatcher<ServerCommandSource>,
            name: String,
            displayName: String,
            getter: (Pregnant) -> Boolean,
            setter: (Pregnant, Boolean) -> Unit,
            trueStr: String = "是",
            falseStr: String = "否",
            trueColor: String = "c", // 默认红色代表某种状态/疾病
            falseColor: String = "a" // 默认绿色代表正常
        ) {
            val cmd = literal(name)
            buildSelfAndTarget(cmd) { p, s ->
                val state = getter(p)
                val color = if (state) trueColor else falseColor
                val text = if (state) trueStr else falseStr
                s.sendMessage(Text.of("§a${displayName}状态：§$color$text"))
            }
            cmd.then(buildSetter("is", BoolArgumentType.bool(), BoolArgumentType::getBool, setter))
            dispatcher.register(cmd)
        }

        private fun registerSimpleInt(
            dispatcher: CommandDispatcher<ServerCommandSource>,
            name: String,
            displayName: String,
            getter: (Pregnant) -> Int,
            setter: (Pregnant, Int) -> Unit,
            isDisease: Boolean = false
        ) {
            val cmd = literal(name)
            buildSelfAndTarget(cmd) { p, s ->
                val value = getter(p)
                if (isDisease) {
                    val status = if (value > 0) "§c已患上 (数值: $value)" else "§a没有患上/健康"
                    s.sendMessage(Text.of("当前${displayName}状态：$status"))
                } else {
                    s.sendMessage(Text.of("当前${displayName}：$value"))
                }
            }
            cmd.then(buildSetter("time", IntegerArgumentType.integer(0), IntegerArgumentType::getInteger, setter))
            dispatcher.register(cmd)
        }

        private fun registerComplexDisease(
            dispatcher: CommandDispatcher<ServerCommandSource>,
            name: String,
            displayName: String,
            valGetter: (Pregnant) -> Int,
            valSetter: (Pregnant, Int) -> Unit,
            immuneGetter: (Pregnant) -> Boolean,
            immuneSetter: (Pregnant, Boolean) -> Unit
        ) {
            val cmd = literal(name)
            // 状态查询
            buildSelfAndTarget(cmd) { p, s ->
                val v = valGetter(p)
                val msg = if (v > 0) "已经感染$v" else "没有感染${displayName}哦"
                s.sendMessage(Text.of("当前${displayName}状态：$msg"))
            }
            // 设置数值
            cmd.then(buildSetter("time", IntegerArgumentType.integer(0), IntegerArgumentType::getInteger, valSetter))

            // 免疫功能
            val immuneCmd = literal("immune")
            buildSelfAndTarget(immuneCmd) { p, s ->
                val state = if (immuneGetter(p)) "已免疫" else "没有免疫"
                s.sendMessage(Text.of("当前${displayName}免疫状态：$state"))
            }
            immuneCmd.then(buildSetter("is", BoolArgumentType.bool(), BoolArgumentType::getBool, immuneSetter))

            cmd.then(immuneCmd)
            dispatcher.register(cmd)
        }

        // ==================== 具体业务模块 ====================

        // ... (保持原有的 registerSex 等不修改) ...
        private fun registerSex(dispatcher: CommandDispatcher<ServerCommandSource>) {
            val cmd = literal("sex")

            val showSex: (Pregnant, ServerCommandSource) -> Unit = { p, s ->
                val gender = when {
                    p.isMale && p.isFemale -> "§b男§d女"
                    p.isMale -> "§b男"
                    p.isFemale -> "§d女"
                    else -> "无"
                }
                s.sendMessage(Text.of("§a性别为：$gender"))
            }
            buildSelfAndTarget(cmd, showSex)

            cmd.then(literal("male").then(
                buildSetter("is", BoolArgumentType.bool(), BoolArgumentType::getBool) { p, v -> p.isMale = v }
            ))
            cmd.then(literal("female").then(
                buildSetter("is", BoolArgumentType.bool(), BoolArgumentType::getBool) { p, v -> p.isFemale = v }
            ))

            dispatcher.register(cmd)
        }

        private fun registerPregnant(dispatcher: CommandDispatcher<ServerCommandSource>) {
            val cmd = literal("pregnant")

            buildSelfAndTarget(cmd) { p, s ->
                s.sendMessage(Text.of("剩余孕期：${p.pregnant / 20 / 60 / 20}天"))
            }
            cmd.then(buildSetter("time", IntegerArgumentType.integer(0), IntegerArgumentType::getInteger) { p, v -> p.pregnant = v })

            val statusCmd = literal("status")
            statusCmd.requires { it.hasPermissionLevel(4) }
            buildSelfAndTarget(statusCmd) { p, s ->
                val msg = when {
                    p.isEctopicPregnancy -> "§c当前怀孕状态为宫外孕！"
                    p.isHydatidiformMole -> "§c当前怀孕状态为葡萄胎！"
                    else -> "§a当前怀孕状态正常"
                }
                s.sendMessage(Text.of(msg))
            }

            val setStatusCmd = literal("set")
                .then(argument("type", StringArgumentType.word())
                    .then(argument("is", BoolArgumentType.bool())
                        .executes { ctx ->
                            run(ctx, null) { p, _ -> setPregnancyStatus(p, ctx) }
                        }
                        .then(argument("target", EntityArgumentType.entity())
                            .executes { ctx ->
                                run(ctx, "target") { p, _ -> setPregnancyStatus(p, ctx) }
                            }
                        )
                    )
                )
            statusCmd.then(setStatusCmd)
            cmd.then(statusCmd)

            val countCmd = literal("count")
            buildSelfAndTarget(countCmd) { p, s ->
                if (p.isPregnant) s.sendMessage(Text.of("§a怀了${p.babyCount}胞胎！"))
            }
            countCmd.then(buildSetter("count", IntegerArgumentType.integer(0), IntegerArgumentType::getInteger) { p, v -> p.babyCount = v })
            cmd.then(countCmd)

            dispatcher.register(cmd)
        }

        private fun setPregnancyStatus(p: Pregnant, ctx: CommandContext<ServerCommandSource>) {
            val type = StringArgumentType.getString(ctx, "type")
            val value = BoolArgumentType.getBool(ctx, "is")
            if (type.contains("ect")) {
                p.isEctopicPregnancy = value
            } else if (type.contains("hyd") || type.contains("mole")) {
                p.isHydatidiformMole = value
            }
        }

        private fun registerMenstruation(dispatcher: CommandDispatcher<ServerCommandSource>) {
            val cmd = literal("menstruation")

            buildSelfAndTarget(cmd) { p, s ->
                s.sendMessage(Text.of("当前处于${p.menstruationCycle.text}"))
            }

            cmd.then(buildSetter("time", IntegerArgumentType.integer(0), IntegerArgumentType::getInteger) { p, v -> p.menstruation = v })

            val comfortCmd = literal("comfort")
            buildSelfAndTarget(comfortCmd) { p, s ->
                s.sendMessage(Text.of("卫生巾剩余有效时间：${p.menstruationComfort / 20}秒"))
            }
            cmd.then(comfortCmd)

            dispatcher.register(cmd)
        }

        private fun registerExcretion(dispatcher: CommandDispatcher<ServerCommandSource>) {
            val cmd = literal("excretion")

            cmd.then(literal("release").executes { ctx ->
                run(ctx, null) { p, s ->
                    if (p.excretion > 20 * 60 * 10) {
                        p.excretion -= 20 * 60 * 10
                        p.doDefecationPain()
                        s.sendMessage(Text.of("你排泄了"))
                        p as LivingEntity
                        p.dropStack(JRItems.EXCREMENT.defaultStack)
                    } else {
                        s.sendMessage(Text.of("你目前无需排泄"))
                    }
                }
            })

            buildSelfAndTarget(cmd) { p, s ->
                s.sendMessage(Text.of("当前憋粑粑时间：${p.excretion / 20 / 60}分钟"))
            }

            cmd.then(buildSetter("time", IntegerArgumentType.integer(0), IntegerArgumentType::getInteger) { p, v -> p.excretion = v })

            dispatcher.register(cmd)
        }

        private fun registerUrination(dispatcher: CommandDispatcher<ServerCommandSource>) {
            val cmd = literal("urination")

            cmd.then(literal("release").executes { ctx ->
                run(ctx, null) { p, s ->
                    if (p.urination > 20 * 60 * 10) {
                        p.urination = 0
                        s.sendMessage(Text.of("你排尿了，感觉一身轻！"))
                    } else {
                        s.sendMessage(Text.of("你目前无需排尿"))
                    }
                }
            })

            buildSelfAndTarget(cmd) { p, s ->
                s.sendMessage(Text.of("当前憋尿时间：${p.urination / 20 / 60}分钟"))
            }

            cmd.then(buildSetter("time", IntegerArgumentType.integer(0), IntegerArgumentType::getInteger) { p, v -> p.urination = v })

            dispatcher.register(cmd)
        }

        private fun registerHymen(dispatcher: CommandDispatcher<ServerCommandSource>) {
            val cmd = literal("hymen")

            buildSelfAndTarget(cmd) { p, s ->
                val has = if (p.hasHymen()) "§a完整" else "§c已破裂"
                val imp = if (p.isImperforateHymen()) "§c是 (严重畸形)" else "§b否 (正常)"
                s.sendMessage(Text.of("§e[生理检查] §f处女膜: $has §f| 闭锁畸形: $imp"))
            }

            val setCmd = literal("set").requires { it.hasPermissionLevel(4) }

            setCmd.then(literal("has").then(
                argument("value", BoolArgumentType.bool())
                    .executes { ctx ->
                        val v = BoolArgumentType.getBool(ctx, "value")
                        run(ctx, null) { p, _ -> p.setHasHymen(v) }
                    }
                    .then(argument("target", EntityArgumentType.entity())
                        .executes { ctx ->
                            val v = BoolArgumentType.getBool(ctx, "value")
                            run(ctx, "target") { p, s ->
                                p.setHasHymen(v)
                                s.sendMessage(Text.of("§a已设置目标处女膜状态"))
                            }
                        }
                    )
            ))

            setCmd.then(literal("imperforate").then(
                argument("value", BoolArgumentType.bool())
                    .executes { ctx ->
                        val v = BoolArgumentType.getBool(ctx, "value")
                        run(ctx, null) { p, _ -> p.setImperforateHymen(v) }
                    }
                    .then(argument("target", EntityArgumentType.entity())
                        .executes { ctx ->
                            val v = BoolArgumentType.getBool(ctx, "value")
                            run(ctx, "target") { p, s ->
                                p.setImperforateHymen(v)
                                s.sendMessage(Text.of("§a已设置目标闭锁状态"))
                            }
                        }
                    )
            ))

            cmd.then(setCmd)
            dispatcher.register(cmd)
        }

        private fun registerProtogyny(dispatcher: CommandDispatcher<ServerCommandSource>) {
            val cmd = literal("protogyny")

            val enableCmd = literal("enable")
            buildSelfAndTarget(enableCmd) { p, s ->
                val state = if (p.isProtogynyEnabled) "§a是" else "§c否"
                s.sendMessage(Text.of("§e[性别特征] §f雌转雄启用: $state"))
            }
            enableCmd.then(buildSetter("is", BoolArgumentType.bool(), BoolArgumentType::getBool) { p, v ->
                p.isProtogynyEnabled = v
            })
            cmd.then(enableCmd)

            val undergoingCmd = literal("undergoing")
            buildSelfAndTarget(undergoingCmd) { p, s ->
                val state = if (p.isUndergoingProtogyny) "§a是" else "§c否"
                s.sendMessage(Text.of("§e[性别特征] §f正在雌转雄: $state"))
            }
            undergoingCmd.then(buildSetter("is", BoolArgumentType.bool(), BoolArgumentType::getBool) { p, v ->
                p.isUndergoingProtogyny = v
            })
            cmd.then(undergoingCmd)

            val progressCmd = literal("progress")
            buildSelfAndTarget(progressCmd) { p, s ->
                val percent = (p.protogynyProgress.toDouble() / Pregnant.PROTOGYNY_TOTAL_DURATION * 100).toInt()
                s.sendMessage(Text.of("§e[性别特征] §f雌转雄进度: $percent% (${p.protogynyProgress}/${Pregnant.PROTOGYNY_TOTAL_DURATION})"))
            }
            progressCmd.then(buildSetter("val", IntegerArgumentType.integer(0, Pregnant.PROTOGYNY_TOTAL_DURATION), IntegerArgumentType::getInteger) { p, v ->
                p.protogynyProgress = v
            })
            cmd.then(progressCmd)

            dispatcher.register(cmd)
        }

        private fun registerHormones(dispatcher: CommandDispatcher<ServerCommandSource>) {
            val cmd = literal("hormone")

            buildSelfAndTarget(cmd) { p, s ->
                val t = String.format("%.1f", p.testosterone)
                val e = String.format("%.1f", p.estrogen)
                val prog = String.format("%.1f", p.progesterone)
                val attr = String.format("%.1f", p.attractionScore)

                s.sendMessage(Text.of("§e[激素面板] §7===================="))
                s.sendMessage(Text.of("§c 睾酮(T): $t  §d 雌激素(E): $e"))
                s.sendMessage(Text.of("§b 孕酮(P): $prog §6 吸引力: $attr"))
            }

            val tCmd = literal("testosterone")
            tCmd.then(buildSetter("val", FloatArgumentType.floatArg(0f), FloatArgumentType::getFloat) { p, v -> p.testosterone = v })
            cmd.then(tCmd)

            val eCmd = literal("estrogen")
            eCmd.then(buildSetter("val", FloatArgumentType.floatArg(0f), FloatArgumentType::getFloat) { p, v -> p.estrogen = v })
            cmd.then(eCmd)

            val pCmd = literal("progesterone")
            pCmd.then(buildSetter("val", FloatArgumentType.floatArg(0f), FloatArgumentType::getFloat) { p, v -> p.progesterone = v })
            cmd.then(pCmd)

            dispatcher.register(cmd)
        }

        // ==================== 【新增模块】 黄体破裂指令 ====================
        private fun registerCorpusLuteumRupture(dispatcher: CommandDispatcher<ServerCommandSource>) {
            val cmd = literal("corpus_luteum_rupture")

            // 1. 查看状态
            // /corpus_luteum_rupture [target]
            buildSelfAndTarget(cmd) { p, s ->
                val time = p.corpusLuteumRupture
                val severe = p.isSevereCorpusLuteumRupture
                if (time > 0) {
                    val sevStr = if (severe) "§c重症 (大血管破裂)" else "§e轻症"
                    s.sendMessage(Text.of("§c[生理检查] §f黄体破裂: $sevStr §f| 积血时间: $time tick"))
                } else {
                    s.sendMessage(Text.of("§a[生理检查] §f黄体完好 (无破裂内出血)"))
                }
            }

            // 2. 更改数值与重症状态
            // /corpus_luteum_rupture time set <val> [target]
            val timeCmd = literal("time")
            timeCmd.then(buildSetter("val", IntegerArgumentType.integer(0), IntegerArgumentType::getInteger) { p, v ->
                p.corpusLuteumRupture = v
            })
            cmd.then(timeCmd)

            // /corpus_luteum_rupture severe set <val> [target]
            val severeCmd = literal("severe")
            severeCmd.then(buildSetter("val", BoolArgumentType.bool(), BoolArgumentType::getBool) { p, v ->
                p.isSevereCorpusLuteumRupture = v
            })
            cmd.then(severeCmd)

            // 3. 手动触发
            // /corpus_luteum_rupture trigger [target]
            val triggerCmd = literal("trigger").requires { it.hasPermissionLevel(4) }
            buildSelfAndTarget(triggerCmd) { p, s ->
                // 调用我们在接口里写好的破裂方法
                val success = p.ruptureCorpusLuteum("")
                if (!success){
                    s.sendMessage(Text.of("§e触发失败：目标可能并非处于黄体期，或者没有子宫，或已经处于破裂状态。"))
                }
            }
            cmd.then(triggerCmd)

            // 4. 手动治愈
            // /corpus_luteum_rupture cure [target]
            val cureCmd = literal("cure").requires { it.hasPermissionLevel(4) }
            buildSelfAndTarget(cureCmd) { p, s ->
                if (p.corpusLuteumRupture > 0) {
                    p.cureCorpusLuteumRupture()
                } else {
                    s.sendMessage(Text.of("§a目标没有黄体破裂，无需治疗。"))
                }
            }
            cmd.then(cureCmd)

            dispatcher.register(cmd)
        }
    }
}