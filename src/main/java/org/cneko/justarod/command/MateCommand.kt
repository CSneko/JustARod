package org.cneko.justarod.command

import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.player.Player
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.literal
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.level.ServerLevel
import net.minecraft.network.chat.*
import net.minecraft.network.chat.contents.PlainTextContents.LiteralContents
import net.minecraft.ChatFormatting
import org.cneko.justarod.entity.JREntities
import org.cneko.justarod.entity.SeeeeexNekoEntity
import org.cneko.toneko.common.api.TickTasks
import org.cneko.toneko.common.mod.entities.AdventurerNeko
import org.cneko.toneko.common.mod.util.TickTaskQueue

class MateCommand {
    companion object {
        private val mateRequests: MutableMap<Player, Player> = HashMap() // 请求者为值，被请求者为键
        fun init() {
            CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
                dispatcher.register(literal("mate")
                    .then(literal("send")
                        .then(argument("target", EntityArgument.player())
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

        private fun mateCommand(ctx: CommandContext<CommandSourceStack>?, force: Boolean): Int {

            val requester = ctx?.source?.player ?: return 0
            val target = EntityArgument.getPlayer(ctx, "target")

            // 验证逻辑
            if (mateRequests.containsKey(target)) {
                requester.sendSystemMessage(Component.literal("§c该玩家已有待处理请求"))
                return 1
            }
            if (target == requester) {
                requester.sendSystemMessage(Component.literal("§c不能和自己交配"))
                return 1
            }

            if (target.distanceTo(requester)> 10){
                requester.sendSystemMessage(Component.literal("§c距离太远，无法交配"))
                return 1
            }

            // 创建可变文本对象
            val requestMessage: MutableComponent = MutableComponent.create(LiteralContents("§a${requester.name?.string}想要和您交配，是否同意？ "))

            // 创建同意按钮
            val acceptButton: MutableComponent = MutableComponent.create(LiteralContents("§2[同意]"))
            acceptButton.style = acceptButton.style
                .withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/mate accept"))
                .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("点击同意")))
                .withColor(ChatFormatting.GREEN)

            if (!force) {
                // 创建拒绝按钮
                val denyButton: MutableComponent = MutableComponent.create(LiteralContents(" §c[拒绝]"))
                denyButton.style = denyButton.style
                    .withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/mate deny"))
                    .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("点击拒绝")))
                    .withColor(ChatFormatting.RED)

                // 将按钮添加到消息中
                target?.sendSystemMessage(requestMessage.append(acceptButton).append(denyButton))
                target?.sendSystemMessage(Component.literal("§7 10秒钟不应答则视为拒绝"))
                mateRequests[target] = requester
            } else {
                // 强制模式下只显示同意按钮
                target?.sendSystemMessage(requestMessage.append(acceptButton).append(acceptButton))
                target?.sendSystemMessage(Component.literal("§7 10秒钟不应答则视为接受"))

                mateRequests[target] = requester
            }

            // 添加超时任务
            val queen = TickTaskQueue()
            queen.addTask(10 * 20) {
                if (mateRequests.containsKey(target)) {
                    if (!force) {
                        requester.sendSystemMessage(Component.literal("§c交配请求超时，交配请求已取消"))
                        target.sendSystemMessage(Component.literal("§c你没有应答交配请求，交配请求已取消"))
                    }else{
                        mate(requester, target)
                    }
                    mateRequests.remove(target)
                }
            }
            TickTasks.add(queen)
            return 1
        }

        private fun acceptCommand(ctx: CommandContext<CommandSourceStack>): Int {
            val target: Player? = ctx.source.player
            val requester = mateRequests[target]
            if (requester != null) {
                mateRequests.remove(target)
                target?.let { mate(requester, it) }
            } else {
                target?.sendSystemMessage(Component.literal("§c你没有收到交配请求"))
            }
            return 1
        }

        private fun mate(requester:Player, target:Player){
            requester.sendSystemMessage(Component.literal("§a交配成功！"))
            target.sendSystemMessage(Component.literal("§a交配成功！"))
            val entity = SeeeeexNekoEntity(JREntities.SEEEEEX_NEKO, requester.level())
            val world = requester.level()
            if (world is ServerLevel){
                world.tryAddFreshEntityWithPassengers(entity)
                entity.setPos(requester.x, requester.y, requester.z)
                entity.age = -48000
            }
            // 分别给予虚弱效果
            requester.addEffect(MobEffectInstance(MobEffects.WEAKNESS, 20*60*5, 1, false, false))
            target.addEffect(MobEffectInstance(MobEffects.WEAKNESS, 20*60*5, 1, false, false))
        }

        private fun denyCommand(ctx: CommandContext<CommandSourceStack>): Int {
            val player: Player? = ctx.source.player
            val requester = mateRequests[player]
            if (requester != null) {
                player?.sendSystemMessage(Component.literal("§c你拒绝了${requester.name?.string}的交配请求"))
                requester.sendSystemMessage(Component.literal("§c${player?.name?.string}拒绝了你的交配请求"))
                mateRequests.remove(player)
            } else {
                player?.sendSystemMessage(Component.literal("§c你没有收到交配请求"))
            }
            return 1
        }
    }
}