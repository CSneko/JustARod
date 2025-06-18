package org.cneko.justarod.command

import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.*
import net.minecraft.text.PlainTextContent.Literal
import net.minecraft.util.Formatting
import org.cneko.justarod.entity.JREntities
import org.cneko.justarod.entity.SeeeeexNekoEntity
import org.cneko.toneko.common.api.TickTasks
import org.cneko.toneko.common.mod.entities.AdventurerNeko
import org.cneko.toneko.common.mod.util.TickTaskQueue

class MateCommand {
    companion object {
        private val mateRequests: MutableMap<PlayerEntity, PlayerEntity> = HashMap() // 请求者为值，被请求者为键
        fun init() {
            CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
                dispatcher.register(literal("mate")
                    .then(literal("send")
                        .then(argument("target", EntityArgumentType.player())
                            .then(literal("force")
                                // 怎么的给你强上了
                                .executes { ctx -> mateCommand(ctx, true) }
                            )
                            .executes { ctx -> mateCommand(ctx, false) }
                        )
                    )
                    .then(literal("accept")
                        .executes { ctx -> acceptCommand(ctx) }
                    )
                    .then(literal("deny")
                        .executes { ctx -> denyCommand(ctx) }
                    )
                )
            }
        }

        private fun mateCommand(ctx: CommandContext<ServerCommandSource>?, force: Boolean): Int {

            val requester = ctx?.source?.player ?: return 0
            val target = EntityArgumentType.getPlayer(ctx, "target")

            // 验证逻辑
            if (mateRequests.containsKey(target)) {
                requester.sendMessage(Text.of("§c该玩家已有待处理请求"))
                return 1
            }
            if (target == requester) {
                requester.sendMessage(Text.of("§c不能和自己交配"))
                return 1
            }

            if (target.distanceTo(requester)> 10){
                requester.sendMessage(Text.of("§c距离太远，无法交配"))
                return 1
            }

            // 创建可变文本对象
            val requestMessage: MutableText = MutableText.of(Literal("§a${requester.name?.string}想要和您交配，是否同意？ "))

            // 创建同意按钮
            val acceptButton: MutableText = MutableText.of(Literal("§2[同意]"))
            acceptButton.style = acceptButton.style
                .withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/mate accept"))
                .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of("点击同意")))
                .withColor(Formatting.GREEN)

            if (!force) {
                // 创建拒绝按钮
                val denyButton: MutableText = MutableText.of(Literal(" §c[拒绝]"))
                denyButton.style = denyButton.style
                    .withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/mate deny"))
                    .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of("点击拒绝")))
                    .withColor(Formatting.RED)

                // 将按钮添加到消息中
                target?.sendMessage(requestMessage.append(acceptButton).append(denyButton))
                target?.sendMessage(Text.of("§7 10秒钟不应答则视为拒绝"))
                mateRequests[target] = requester
            } else {
                // 强制模式下只显示同意按钮
                target?.sendMessage(requestMessage.append(acceptButton).append(acceptButton))
                target?.sendMessage(Text.of("§7 10秒钟不应答则视为接受"))

                mateRequests[target] = requester
            }

            // 添加超时任务
            val queen = TickTaskQueue()
            queen.addTask(10 * 20) {
                if (mateRequests.containsKey(target)) {
                    if (!force) {
                        requester.sendMessage(Text.of("§c交配请求超时，交配请求已取消"))
                        target.sendMessage(Text.of("§c你没有应答交配请求，交配请求已取消"))
                    }else{
                        mate(requester, target)
                    }
                    mateRequests.remove(target)
                }
            }
            TickTasks.add(queen)
            return 1
        }

        private fun acceptCommand(ctx: CommandContext<ServerCommandSource>): Int {
            val target: PlayerEntity? = ctx.source.player
            val requester = mateRequests[target]
            if (requester != null) {
                mateRequests.remove(target)
                target?.let { mate(requester, it) }
            } else {
                target?.sendMessage(Text.of("§c你没有收到交配请求"))
            }
            return 1
        }

        private fun mate(requester:PlayerEntity, target:PlayerEntity){
            requester.sendMessage(Text.of("§a交配成功！"))
            target.sendMessage(Text.of("§a交配成功！"))
            val entity = SeeeeexNekoEntity(JREntities.SEEEEEX_NEKO, requester.world)
            val world = requester.world
            if (world is ServerWorld){
                world.spawnNewEntityAndPassengers(entity)
                entity.setPos(requester.x, requester.y, requester.z)
                entity.age = -48000
            }
            // 分别给予虚弱效果
            requester.addStatusEffect(StatusEffectInstance(StatusEffects.WEAKNESS, 20*60*5, 1, false, false))
            target.addStatusEffect(StatusEffectInstance(StatusEffects.WEAKNESS, 20*60*5, 1, false, false))
        }

        private fun denyCommand(ctx: CommandContext<ServerCommandSource>): Int {
            val player: PlayerEntity? = ctx.source.player
            val requester = mateRequests[player]
            if (requester != null) {
                player?.sendMessage(Text.of("§c你拒绝了${requester.name?.string}的交配请求"))
                requester.sendMessage(Text.of("§c${player?.name?.string}拒绝了你的交配请求"))
                mateRequests.remove(player)
            } else {
                player?.sendMessage(Text.of("§c你没有收到交配请求"))
            }
            return 1
        }
    }
}