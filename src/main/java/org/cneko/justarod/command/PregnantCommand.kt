package org.cneko.justarod.command

import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.FloatArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import org.cneko.justarod.client.gui.ScanType
import org.cneko.justarod.entity.Pregnant
import org.cneko.justarod.item.JRItems
import org.cneko.justarod.packet.XRayScanScreenPayload
import org.cneko.justarod.property.JRRegistry

class PregnantCommand {
    companion object {

        // ==================== 帮助系统数据结构 ====================
        private data class CommandHelp(val displayName: String, val usages: List<String>)
        private val helpData = mutableMapOf<String, CommandHelp>()

        private fun addHelp(name: String, displayName: String, vararg usages: String) {
            // 强制转换为小写，防止大写字母导致客户端崩溃
            helpData[name.lowercase()] = CommandHelp(displayName, usages.toList())
        }

        // ==================== 初始化入口 ====================
        fun init() {
            CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
                // 修复：每次 reload 时清空帮助数据，防止重复堆积
                helpData.clear()

                val baseCmd = literal("jr")

                // 1. 注册基础属性命令，并自动生成帮助信息
                JRRegistry.PROPERTIES.forEach { property ->
                    val argument = property.registerCommand()
                    if (argument != null) {
                        baseCmd.then(argument)

                        // 强制将名称转换为小写，这是防止进服崩溃的关键防御之一
                        val safeName = property.name.lowercase()
                        addHelp(safeName, property.displayName,
                            "/jr $safeName [target] §7- 查看状态",
                            "/jr $safeName set <value> [target] §7- 修改状态 (需要OP)"
                        )
                    }
                }

                // 2. 注册具体业务模块 (将其全部挂载到 baseCmd 下)
                registerSex(baseCmd)
                registerPregnant(baseCmd)
                registerMenstruation(baseCmd)
                registerExcretion(baseCmd)
                registerUrination(baseCmd)
                registerHymen(baseCmd)
                registerProtogyny(baseCmd)
                registerHormones(baseCmd)
                registerCorpusLuteumRupture(baseCmd)
                // registerXRayScan(baseCmd)
                registerLactation(baseCmd)

                // 3. 注册帮助命令
                registerHelp(baseCmd)

                // 4. 最终将 /jr 注册到 dispatcher
                dispatcher.register(baseCmd)
            }
        }

        // ==================== 帮助系统命令实现 ====================
        private fun registerHelp(baseCmd: LiteralArgumentBuilder<ServerCommandSource>) {
            val helpCmd = literal("help")

            // /jr help
            helpCmd.executes { ctx ->
                val source = ctx.source
                source.sendMessage(Text.of("§e=== JustARod 命令帮助列表 ==="))
                source.sendMessage(Text.of("§7(提示：点击绿色命令即可快速查看用法)"))

                helpData.toSortedMap().forEach { (name, info) ->
                    // 创建可点击的文本
                    val text = Text.literal("§a/jr $name §f- ${info.displayName}")
                        .styled { style ->
                            style.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/jr help $name"))
                                .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of("点击查看该命令具体用法")))
                        }
                    source.sendMessage(text)
                }
                1
            }

            // /jr help <子命令>
            helpCmd.then(argument("command", StringArgumentType.word())
                .executes { ctx ->
                    val cmdName = StringArgumentType.getString(ctx, "command").lowercase()
                    val source = ctx.source
                    val info = helpData[cmdName]

                    if (info == null) {
                        source.sendMessage(Text.of("§c未找到子命令: $cmdName。请输入 /jr help 查看列表。"))
                        return@executes 0
                    }

                    source.sendMessage(Text.of("§e=== /jr $cmdName (${info.displayName}) ==="))
                    info.usages.forEach { usage ->
                        source.sendMessage(Text.of("§b$usage"))
                    }
                    1
                }
            )

            baseCmd.then(helpCmd)
        }


        // ==================== 核心 Helper ====================
        private fun run(ctx: CommandContext<ServerCommandSource>, targetName: String? = null, action: (Pregnant, ServerCommandSource) -> Unit): Int {
            val source = ctx.source
            val entity = if (targetName != null) EntityArgumentType.getEntity(ctx, targetName) else source.entity
            if (entity is Pregnant) action(entity, source)
            return 1
        }

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

        private fun <T> buildSetter(
            argName: String,
            argType: com.mojang.brigadier.arguments.ArgumentType<T>,
            getter: (CommandContext<ServerCommandSource>, String) -> T,
            setterAction: (Pregnant, T) -> Unit
        ): LiteralArgumentBuilder<ServerCommandSource> {
            return literal("set")
                // 修复：原为 4(控制台)，现改为 2(普通OP)，否则管理员无法使用
                .requires { s -> s.hasPermissionLevel(2) }
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

        // ==================== 具体业务模块 ====================

        private fun registerSex(baseCmd: LiteralArgumentBuilder<ServerCommandSource>) {
            val cmd = literal("sex")
            addHelp("sex", "基础性别管理",
                "/jr sex [target] §7- 查看性别",
                "/jr sex male set <true/false> [target] §7- 设置男性状态",
                "/jr sex female set <true/false> [target] §7- 设置女性状态"
            )

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

            baseCmd.then(cmd)
        }

        private fun registerPregnant(baseCmd: LiteralArgumentBuilder<ServerCommandSource>) {
            val cmd = literal("pregnant")
            addHelp("pregnant", "怀孕详细管理",
                "/jr pregnant [target] §7- 查看剩余孕期",
                "/jr pregnant set time <ticks> [target] §7- 设置孕期",
                "/jr pregnant count [target] §7- 查看胎儿数量",
                "/jr pregnant count set <val> [target] §7- 设置胎儿数量",
                "/jr pregnant status [target] §7- 查看是否有宫外孕/葡萄胎等异常",
                "/jr pregnant status set <ect/hyd> <is_true> [target] §7- 设置异常状态"
            )

            buildSelfAndTarget(cmd) { p, s ->
                s.sendMessage(Text.of("剩余孕期：${p.pregnant / 20 / 60 / 20}天"))
            }
            cmd.then(buildSetter("time", IntegerArgumentType.integer(0), IntegerArgumentType::getInteger) { p, v -> p.pregnant = v })

            val statusCmd = literal("status")
            statusCmd.requires { it.hasPermissionLevel(2) }
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
                        .executes { ctx -> run(ctx, null) { p, _ -> setPregnancyStatus(p, ctx) } }
                        .then(argument("target", EntityArgumentType.entity())
                            .executes { ctx -> run(ctx, "target") { p, _ -> setPregnancyStatus(p, ctx) } }
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

            baseCmd.then(cmd)
        }

        private fun setPregnancyStatus(p: Pregnant, ctx: CommandContext<ServerCommandSource>) {
            val type = StringArgumentType.getString(ctx, "type").lowercase()
            val value = BoolArgumentType.getBool(ctx, "is")
            if (type.contains("ect")) p.isEctopicPregnancy = value
            else if (type.contains("hyd") || type.contains("mole")) p.isHydatidiformMole = value
        }


        private fun registerExcretion(baseCmd: LiteralArgumentBuilder<ServerCommandSource>) {
            val cmd = literal("excretion")
            addHelp("excretion", "排泄管理",
                "/jr excretion [target] §7- 查看憋屎时间",
                "/jr excretion set time <ticks> [target] §7- 设置憋屎时间",
                "/jr excretion release §7- 释放排泄物 (玩家自己)"
            )

            cmd.then(literal("release").executes { ctx ->
                run(ctx, null) { p, s ->
                    if (p.excretion > 20 * 60 * 10) {
                        p.excretion -= 20 * 60 * 10
                        p.doDefecationPain()
                        s.sendMessage(Text.of("你排泄了"))
                        (p as LivingEntity).dropStack(JRItems.EXCREMENT.defaultStack)
                    } else {
                        s.sendMessage(Text.of("你目前无需排泄"))
                    }
                }
            })

            buildSelfAndTarget(cmd) { p, s -> s.sendMessage(Text.of("当前憋粑粑时间：${p.excretion / 20 / 60}分钟")) }
            cmd.then(buildSetter("time", IntegerArgumentType.integer(0), IntegerArgumentType::getInteger) { p, v -> p.excretion = v })

            baseCmd.then(cmd)
        }

        private fun registerUrination(baseCmd: LiteralArgumentBuilder<ServerCommandSource>) {
            val cmd = literal("urination")
            addHelp("urination", "排尿与括约肌管理",
                "/jr urination [target] §7- 查看憋尿时间与括约肌状态",
                "/jr urination set time <ticks> [target] §7- 设置憋尿时间",
                "/jr urination release §7- 释放排尿 (玩家自己)",
                "/jr urination incontinence set <ticks> [target] §7- 设置尿失禁严重程度(tick)"
            )

            // 1. 玩家自己排空膀胱
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

            // 2. 综合状态查询 (憋尿量 + 括约肌健康度)
            buildSelfAndTarget(cmd) { p, s ->
                val urineMinutes = p.urination / 20 / 60

                // 尿失禁分级判定 (与 Tick 逻辑保持一致)
                val incTime = p.urinaryIncontinence
                val stage = when {
                    incTime <= 0 -> "§a正常 (括约肌健康)"
                    incTime <= 20 * 60 * 20 * 2 -> "§e潜伏期 (轻微受损)"
                    incTime <= 20 * 60 * 20 * 6 -> "§6轻度 (压力性漏尿 - 剧烈运动易漏)"
                    incTime <= 20 * 60 * 20 * 12 -> "§c中度 (急迫性失禁 - 憋不住尿)"
                    else -> "§4重度 (完全失禁 - 随时都在滴答)"
                }

                s.sendMessage(Text.of("§e[排尿系统] §7===================="))
                s.sendMessage(Text.of("§f 当前憋尿时间：§b$urineMinutes 分钟"))
                s.sendMessage(Text.of("§f 括约肌状态：$stage §7(积累值: $incTime tick)"))
            }

            // 3. 设置憋尿时间
            cmd.then(buildSetter("time", IntegerArgumentType.integer(0), IntegerArgumentType::getInteger) { p, v -> p.urination = v })

            // 4. 设置尿失禁数值 (挂载在 /jr urination incontinence 下)
            val incontinenceCmd = literal("incontinence")
            incontinenceCmd.then(
                buildSetter("val", IntegerArgumentType.integer(0), IntegerArgumentType::getInteger) { p, v -> p.urinaryIncontinence = v }
            )
            cmd.then(incontinenceCmd)

            baseCmd.then(cmd)
        }

        private fun registerHymen(baseCmd: LiteralArgumentBuilder<ServerCommandSource>) {
            val cmd = literal("hymen")
            addHelp("hymen", "处女膜状态",
                "/jr hymen [target] §7- 查看状态与是否畸形",
                "/jr hymen set has <true/false> [target] §7- 设置完整度",
                "/jr hymen set imperforate <true/false> [target] §7- 设置是否闭锁(畸形)"
            )

            buildSelfAndTarget(cmd) { p, s ->
                val has = if (p.hasHymen()) "§a完整" else "§c已破裂"
                val imp = if (p.isImperforateHymen) "§c是 (严重畸形)" else "§b否 (正常)"
                s.sendMessage(Text.of("§e[生理检查] §f处女膜: $has §f| 闭锁畸形: $imp"))
            }

            val setCmd = literal("set").requires { it.hasPermissionLevel(2) }

            setCmd.then(literal("has").then(argument("value", BoolArgumentType.bool())
                .executes { ctx -> run(ctx, null) { p, _ -> p.setHasHymen(BoolArgumentType.getBool(ctx, "value")) } }
                .then(argument("target", EntityArgumentType.entity())
                    .executes { ctx -> run(ctx, "target") { p, s ->
                        p.setHasHymen(BoolArgumentType.getBool(ctx, "value")); s.sendMessage(Text.of("§a已设置"))
                    } }
                )
            ))

            setCmd.then(literal("imperforate").then(argument("value", BoolArgumentType.bool())
                .executes { ctx -> run(ctx, null) { p, _ -> p.setImperforateHymen(BoolArgumentType.getBool(ctx, "value")) } }
                .then(argument("target", EntityArgumentType.entity())
                    .executes { ctx -> run(ctx, "target") { p, s ->
                        p.setImperforateHymen(BoolArgumentType.getBool(ctx, "value")); s.sendMessage(Text.of("§a已设置"))
                    } }
                )
            ))

            cmd.then(setCmd)
            baseCmd.then(cmd)
        }

        private fun registerProtogyny(baseCmd: LiteralArgumentBuilder<ServerCommandSource>) {
            val cmd = literal("protogyny")
            addHelp("protogyny", "雌转雄机制",
                "/jr protogyny enable [target] §7- 查看是否激活雌转雄",
                "/jr protogyny enable set is <true/false> [target] §7- 更改激活状态",
                "/jr protogyny undergoing [target] §7- 查看是否正在转换",
                "/jr protogyny progress [target] §7- 查看转换进度"
            )

            val enableCmd = literal("enable")
            buildSelfAndTarget(enableCmd) { p, s -> s.sendMessage(Text.of("§e[性别特征] §f雌转雄启用: ${if (p.isProtogynyEnabled) "§a是" else "§c否"}")) }
            enableCmd.then(buildSetter("is", BoolArgumentType.bool(), BoolArgumentType::getBool) { p, v -> p.isProtogynyEnabled = v })
            cmd.then(enableCmd)

            val undergoingCmd = literal("undergoing")
            buildSelfAndTarget(undergoingCmd) { p, s -> s.sendMessage(Text.of("§e[性别特征] §f正在雌转雄: ${if (p.isUndergoingProtogyny) "§a是" else "§c否"}")) }
            undergoingCmd.then(buildSetter("is", BoolArgumentType.bool(), BoolArgumentType::getBool) { p, v -> p.isUndergoingProtogyny = v })
            cmd.then(undergoingCmd)

            val progressCmd = literal("progress")
            buildSelfAndTarget(progressCmd) { p, s ->
                val percent = (p.protogynyProgress.toDouble() / Pregnant.PROTOGYNY_TOTAL_DURATION * 100).toInt()
                s.sendMessage(Text.of("§e[性别特征] §f雌转雄进度: $percent% (${p.protogynyProgress}/${Pregnant.PROTOGYNY_TOTAL_DURATION})"))
            }
            progressCmd.then(buildSetter("val", IntegerArgumentType.integer(0, Pregnant.PROTOGYNY_TOTAL_DURATION), IntegerArgumentType::getInteger) { p, v -> p.protogynyProgress = v })
            cmd.then(progressCmd)

            baseCmd.then(cmd)
        }

        // ====================================================
        // 生理周期与子宫内膜 管理
        // ====================================================
        private fun registerMenstruation(baseCmd: LiteralArgumentBuilder<ServerCommandSource>) {
            val cmd = literal("menstruation")
            addHelp("menstruation", "生理周期与内膜管理",
                "/jr menstruation [target] §7- 查看当前周期、时钟与内膜状态",
                "/jr menstruation clock set <ticks> [target] §7- 设置卵巢时钟(0~336000)",
                "/jr menstruation thickness set <val> [target] §7- 设置子宫内膜厚度",
                "/jr menstruation comfort [target] §7- 查看卫生巾剩余有效时间"
            )

            // 查看生理周期综合面板
            buildSelfAndTarget(cmd) { p, s ->
                if (!p.isFemale || !p.hasUterus()) {
                    s.sendMessage(Text.of("§c目标不具备女性生理特征或子宫，没有生理周期。"))
                    return@buildSelfAndTarget
                }

                val clockDays = String.format("%.1f", p.ovarianClock / 24000.0f)
                val thick = String.format("%.2f", p.uterineThickness)
                val cycleName = p.menstruationCycle.text // 如果你的Java改成了 getCurrentCycle()，这里对应 currentCycle.text

                s.sendMessage(Text.of("§d[生理周期面板] §7===================="))
                s.sendMessage(Text.of("§f 当前状态: §b$cycleName"))
                s.sendMessage(Text.of("§f 卵巢时钟: 第 §e$clockDays §f天 §7(满14天一循环)"))
                s.sendMessage(Text.of("§f 内膜厚度: §c$thick mm §7(跌破厚度且激素撤退时出血)"))
            }

            // 修改卵巢时钟 (0 ~ 14天)
            cmd.then(literal("clock").then(
                buildSetter("ticks", IntegerArgumentType.integer(0, 336000), IntegerArgumentType::getInteger) { p, v -> p.ovarianClock = v }
            ))

            // 修改内膜厚度
            cmd.then(literal("thickness").then(
                buildSetter("val", FloatArgumentType.floatArg(0f, 100f), FloatArgumentType::getFloat) { p, v -> p.uterineThickness = v }
            ))

            // 卫生巾相关 (保留原逻辑)
            val comfortCmd = literal("comfort")
            buildSelfAndTarget(comfortCmd) { p, s -> s.sendMessage(Text.of("卫生巾剩余有效时间：${p.menstruationComfort / 20}秒")) }
            cmd.then(comfortCmd)

            baseCmd.then(cmd)
        }

        // ====================================================
        // 内外源激素 管理
        // ====================================================
        private fun registerHormones(baseCmd: LiteralArgumentBuilder<ServerCommandSource>) {
            val cmd = literal("hormone")
            addHelp("hormone", "内分泌与激素系统",
                "/jr hormone [target] §7- 查看激素综合面板(内外源分离)",
                "/jr hormone exo_e2 set <val> [target] §7- 注射外源雌二醇 (吃药)",
                "/jr hormone exo_p set <val> [target] §7- 注射外源孕酮 (吃药)",
                "/jr hormone exo_t set <val> [target] §7- 注射外源睾酮 (吃药)",
                "/jr hormone exo_blocker set <val> [target] §7- 注射/服用激素阻断剂 (抗雄/抗雌)",
                "/jr hormone hrt_mtf set <val> [target] §7- 修改男转女变性进度 (Tick)",
                "/jr hormone hrt_ftm set <val> [target] §7- 修改女转男变性进度 (Tick)",
                "/jr hormone atrophy set <val> [target] §7- 修改生殖道萎缩病程 (Tick)"
            )

            // 查看激素分离面板
            buildSelfAndTarget(cmd) { p, s ->
                val formatNum = { num: Float -> String.format("%.1f", num) }

                val tE2 = formatNum(p.totalE2)
                val inE2 = formatNum(p.endoE2)
                val exE2 = formatNum(p.exoE2)

                val tP = formatNum(p.totalP)
                val inP = formatNum(p.endoP)
                val exP = formatNum(p.exoP)

                val tT = formatNum(p.totalT)
                val inT = formatNum(p.endoT)
                val exT = formatNum(p.exoT)

                val blocker = formatNum(p.exoBlocker)
                val attr = formatNum(p.attractionScore)

                val mtf = p.hrtMtfProgress
                val ftm = p.hrtFtmProgress
                val atrophy = p.vaginalAtrophy

                s.sendMessage(Text.of("§e[激素浓度面板 (单位:pg/mL或ng/mL)] §7========="))
                s.sendMessage(Text.of("§d 雌二醇(E2): 总 §l$tE2§r §7(内:$inE2 + 外:$exE2)"))
                s.sendMessage(Text.of("§b 孕酮(P):   总 §l$tP§r §7(内:$inP + 外:$exP)"))
                s.sendMessage(Text.of("§c 睾酮(T):   总 §l$tT§r §7(内:$inT + 外:$exT)"))
                s.sendMessage(Text.of("§8 阻断剂(Blocker): §l$blocker§r"))
                s.sendMessage(Text.of("§6 当前散发吸引力: $attr"))
                s.sendMessage(Text.of("§a[HRT 变性与病理状态] §7========="))
                s.sendMessage(Text.of("§d 男转女(MTF) 进度: $mtf Ticks"))
                s.sendMessage(Text.of("§b 女转男(FTM) 进度: $ftm Ticks"))
                if (atrophy > 0) {
                    s.sendMessage(Text.of("§4 缺乏雌激素导致的萎缩症: $atrophy Ticks"))
                }
            }

            // 修改外源雌激素 (外源激素不会被Tick覆盖，只会自然代谢)
            cmd.then(literal("exo_e2").then(
                buildSetter("val", FloatArgumentType.floatArg(0f), FloatArgumentType::getFloat) { p, v -> p.exoE2 = v }
            ))

            // 修改外源孕酮
            cmd.then(literal("exo_p").then(
                buildSetter("val", FloatArgumentType.floatArg(0f), FloatArgumentType::getFloat) { p, v -> p.exoP = v }
            ))

            // 修改外源睾酮
            cmd.then(literal("exo_t").then(
                buildSetter("val", FloatArgumentType.floatArg(0f), FloatArgumentType::getFloat) { p, v -> p.exoT = v }
            ))

            // 修改激素阻断剂
            cmd.then(literal("exo_blocker").then(
                buildSetter("val", FloatArgumentType.floatArg(0f), FloatArgumentType::getFloat) { p, v -> p.exoBlocker = v }
            ))

            // 修改 MTF 男转女进度
            cmd.then(literal("hrt_mtf").then(
                buildSetter("val", IntegerArgumentType.integer(0), IntegerArgumentType::getInteger) { p, v -> p.hrtMtfProgress = v }
            ))

            // 修改 FTM 女转男进度
            cmd.then(literal("hrt_ftm").then(
                buildSetter("val", IntegerArgumentType.integer(0), IntegerArgumentType::getInteger) { p, v -> p.hrtFtmProgress = v }
            ))

            // 修改 萎缩症进度
            cmd.then(literal("atrophy").then(
                buildSetter("val", IntegerArgumentType.integer(0), IntegerArgumentType::getInteger) { p, v -> p.vaginalAtrophy = v }
            ))

            baseCmd.then(cmd)
        }

        private fun registerCorpusLuteumRupture(baseCmd: LiteralArgumentBuilder<ServerCommandSource>) {
            val cmd = literal("corpus_luteum_rupture")
            addHelp("corpus_luteum_rupture", "黄体破裂",
                "/jr corpus_luteum_rupture [target] §7- 查看状态",
                "/jr corpus_luteum_rupture trigger [target] §7- 手动触发破裂",
                "/jr corpus_luteum_rupture cure [target] §7- 治疗破裂",
                "/jr corpus_luteum_rupture time set <val> [target]",
                "/jr corpus_luteum_rupture severe set <true/false> [target] §7- 设定是否大血管破裂(重症)"
            )

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

            cmd.then(literal("time").then(buildSetter("val", IntegerArgumentType.integer(0), IntegerArgumentType::getInteger) { p, v -> p.corpusLuteumRupture = v }))
            cmd.then(literal("severe").then(buildSetter("val", BoolArgumentType.bool(), BoolArgumentType::getBool) { p, v -> p.isSevereCorpusLuteumRupture = v }))

            val triggerCmd = literal("trigger").requires { it.hasPermissionLevel(2) }
            buildSelfAndTarget(triggerCmd) { p, s ->
                if (!p.ruptureCorpusLuteum("")) {
                    s.sendMessage(Text.of("§e触发失败：目标可能并非处于黄体期，或者没有子宫，或已经处于破裂状态。"))
                }
            }
            cmd.then(triggerCmd)

            val cureCmd = literal("cure").requires { it.hasPermissionLevel(2) }
            buildSelfAndTarget(cureCmd) { p, s ->
                if (p.corpusLuteumRupture > 0) p.cureCorpusLuteumRupture()
                else s.sendMessage(Text.of("§a目标没有黄体破裂，无需治疗。"))
            }
            cmd.then(cureCmd)

            baseCmd.then(cmd)
        }

        private fun registerXRayScan(baseCmd: LiteralArgumentBuilder<ServerCommandSource>) {
            val cmd = literal("xray_scan")
            addHelp("xray_scan", "B超扫描",
                "/jr xray_scan uterus [target] §7- 打开B超扫描UI界面"
            )

            val uterusCmd = literal("uterus")
            buildSelfAndTarget(uterusCmd) { p, s ->
                if (p.hasUterus() && s.entity is ServerPlayerEntity) {
                    ServerPlayNetworking.send(s.entity as ServerPlayerEntity, XRayScanScreenPayload(ScanType.UTERUS, (p as Entity).id))
                }
            }
            cmd.then(uterusCmd)
            baseCmd.then(cmd)
        }

        private fun registerLactation(baseCmd: LiteralArgumentBuilder<ServerCommandSource>) {
            val cmd = literal("lactation")

            // 1. 注册帮助信息
            addHelp("lactation", "泌乳系统",
                "/jr lactation [target] §7- 查看当前泌乳状态与储奶量",
                "/jr lactation extract <amount> [target] §7- 手动挤出指定量乳汁",
                "/jr lactation milk set <val> [target] §7- 设置当前奶量",
                "/jr lactation mastitis set <ticks> [target] §7- 设置乳腺炎病程",
                "/jr lactation stimulation set <ticks> [target] §7- 设置泌乳刺激度"
            )

            // 2. 基础查询命令 (/jr lactation [target])
            buildSelfAndTarget(cmd) { p, s ->
                val milk = String.format("%.1f", p.milk)
                val max = String.format("%.1f", p.maxMilk)
                val mastitis = p.mastitis
                val stim = p.lactationStimulation

                s.sendMessage(Text.of("§e[泌乳面板] §7===================="))
                s.sendMessage(Text.of("§f 当前储奶量: §b$milk §f/ §3$max"))
                s.sendMessage(Text.of("§c 乳腺炎病程: $mastitis tick"))
                s.sendMessage(Text.of("§d 泌乳刺激度: $stim tick (一直吸一直有)"))
            }

            // 3. 属性 Setter
            // /jr lactation milk set <val> [target]
            cmd.then(literal("milk").then(
                buildSetter("val", FloatArgumentType.floatArg(0f), FloatArgumentType::getFloat) { p, v -> p.milk = v }
            ))

            // /jr lactation mastitis set <val> [target]
            cmd.then(literal("mastitis").then(
                buildSetter("val", IntegerArgumentType.integer(0), IntegerArgumentType::getInteger) { p, v -> p.mastitis = v }
            ))

            // /jr lactation stimulation set <val> [target]
            cmd.then(literal("stimulation").then(
                buildSetter("val", IntegerArgumentType.integer(0), IntegerArgumentType::getInteger) { p, v -> p.lactationStimulation = v }
            ))

            // 4. 执行动作：手动挤奶 (/jr lactation extract <amount> [target])
            val extractCmd = literal("extract").requires { it.hasPermissionLevel(2) }
                .then(argument("amount", FloatArgumentType.floatArg(0.1f))
                    .executes { ctx ->
                        val amount = FloatArgumentType.getFloat(ctx, "amount")
                        run(ctx, null) { p, s ->
                            val extracted = p.extractMilk(amount)
                            if (extracted > 0) {
                                s.sendMessage(Text.of("§a成功挤出了 ${String.format("%.1f", extracted)} ml乳汁！感觉一阵轻松..."))
                            } else {
                                s.sendMessage(Text.of("§c一滴也没有了..."))
                            }
                        }
                    }
                    .then(argument("target", EntityArgumentType.entity())
                        .executes { ctx ->
                            val amount = FloatArgumentType.getFloat(ctx, "amount")
                            run(ctx, "target") { p, s ->
                                val extracted = p.extractMilk(amount)
                                if (extracted > 0) {
                                    s.sendMessage(Text.of("§a成功从目标身上挤出了 ${String.format("%.1f", extracted)} ml乳汁！"))
                                } else {
                                    s.sendMessage(Text.of("§c目标一滴也没有了..."))
                                }
                            }
                        }
                    )
                )
            cmd.then(extractCmd)

            // 挂载到主命令
            baseCmd.then(cmd)
        }
    }
}