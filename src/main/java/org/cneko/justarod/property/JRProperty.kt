package org.cneko.justarod.property

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.literal
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component
import org.cneko.justarod.entity.Pregnant

abstract class JRProperty<T>(
    val name: String,               // 内部名称，用于命令 (如 hpv)
    val displayName: String,        // UI显示名称 (如 HPV感染)
    val getter: (Pregnant) -> T,    // 如何从实体获取
    val setter: (Pregnant, T) -> Unit // 如何设置到实体
) {
    // === 网络同步部分 ===
    abstract fun writeToBuf(buf: RegistryFriendlyByteBuf, value: T): FriendlyByteBuf?
    abstract fun readFromBuf(buf: RegistryFriendlyByteBuf): T

    // === UI 渲染部分 ===
    abstract fun formatValue(value: T): String
    open fun getValueColor(value: T): Int = 0xFFFFFF

    // === 命令部分 ===
    abstract fun getArgumentType(): ArgumentType<T>

    open fun registerCommand(): LiteralArgumentBuilder<CommandSourceStack>? {
        val cmd = literal(name)

        // 1. 查看状态
        cmd.executes { ctx -> runCommand(ctx, null) }
            .then(argument("target", EntityArgument.entity())
                .executes { ctx -> runCommand(ctx, "target") })

        // 2. 修改状态 (需要 OP 权限)
        val setCmd = literal("set").requires { it.hasPermissions(4) }
            .then(argument("value", getArgumentType())
                .executes { ctx -> runSetCommand(ctx, null) }
                .then(argument("target", EntityArgument.entity())
                    .executes { ctx -> runSetCommand(ctx, "target") }
                )
            )
        cmd.then(setCmd)

        return cmd
    }

    private fun runCommand(ctx: CommandContext<CommandSourceStack>, targetName: String?): Int {
        val source = ctx.source
        val entity = if (targetName != null) EntityArgument.getEntity(ctx, targetName) else source.entity
        if (entity is Pregnant) {
            val value = getter(entity)
            source.sendSystemMessage(Component.literal("§a[状态] §f$displayName: ${formatValue(value)}"))
        }
        return 1
    }

    private fun runSetCommand(ctx: CommandContext<CommandSourceStack>, targetName: String?): Int {
        val source = ctx.source
        val entity = if (targetName != null) EntityArgument.getEntity(ctx, targetName) else source.entity
        if (entity is Pregnant) {
            val value = getArgumentFromContext(ctx)
            setter(entity, value)
            source.sendSystemMessage(Component.literal("§a成功将目标 $displayName 设置为 ${formatValue(value)}"))
        }
        return 1
    }

    abstract fun getArgumentFromContext(ctx: CommandContext<CommandSourceStack>): T
}