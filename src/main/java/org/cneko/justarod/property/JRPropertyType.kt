package org.cneko.justarod.property

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.FloatArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.minecraft.entity.EntityType
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodecs
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.command.ServerCommandSource
import org.cneko.justarod.entity.Pregnant
import java.util.Optional
import kotlin.text.get

// --- 布尔类型属性 ---
class JRBoolProperty(
    name: String, displayName: String,
    getter: (Pregnant) -> Boolean, setter: (Pregnant, Boolean) -> Unit,
    val trueText: String = "是", val falseText: String = "否",
    val isDisease: Boolean = false // 如果是疾病，"是"会变成红色
) : JRProperty<Boolean>(name, displayName, getter, setter) {

    override fun writeToBuf(buf: RegistryByteBuf, value: Boolean) = buf.writeBoolean(value)
    override fun readFromBuf(buf: RegistryByteBuf): Boolean = buf.readBoolean()
    override fun getArgumentType() = BoolArgumentType.bool()
    override fun getArgumentFromContext(ctx: CommandContext<ServerCommandSource>) = BoolArgumentType.getBool(ctx, "value")

    override fun formatValue(value: Boolean): String = if (value) trueText else falseText
    override fun getValueColor(value: Boolean): Int {
        if (value && isDisease) return 0xFF5555 // 红色警示
        if (value) return 0xFFFF55 // 黄色高亮
        return 0x55FF55 // 绿色表示否/健康
    }
}

// --- 时间/整数类型属性 ---
class JRTimeProperty(
    name: String, displayName: String,
    getter: (Pregnant) -> Int, setter: (Pregnant, Int) -> Unit,
    val isDisease: Boolean = false
) : JRProperty<Int>(name, displayName, getter, setter) {

    override fun writeToBuf(buf: RegistryByteBuf, value: Int) = buf.writeVarInt(value)
    override fun readFromBuf(buf: RegistryByteBuf): Int = buf.readVarInt()
    override fun getArgumentType() = IntegerArgumentType.integer(0)
    override fun getArgumentFromContext(ctx: CommandContext<ServerCommandSource>) = IntegerArgumentType.getInteger(ctx, "value")

    override fun formatValue(value: Int): String {
        if (value <= 0) return if (isDisease) "健康" else "0"
        val minutes = value / 1200.0
        return if (minutes < 0.1) "< 0.1 分钟" else String.format("%.1f 分钟", minutes)
    }

    override fun getValueColor(value: Int): Int {
        if (value <= 0) return 0x55FF55 // 绿色
        return if (isDisease) 0xFF5555 else 0xFFFF55 // 疾病红，普通黄
    }
}

// --- 普通整数类型 (如胎儿数量) ---
class JRIntProperty(
    name: String, displayName: String,
    getter: (Pregnant) -> Int, setter: (Pregnant, Int) -> Unit
) : JRProperty<Int>(name, displayName, getter, setter) {
    override fun writeToBuf(buf: RegistryByteBuf, value: Int): PacketByteBuf? = buf.writeVarInt(value)
    override fun readFromBuf(buf: RegistryByteBuf): Int = buf.readVarInt()
    override fun getArgumentType(): IntegerArgumentType = IntegerArgumentType.integer(0)
    override fun getArgumentFromContext(ctx: CommandContext<ServerCommandSource>) = IntegerArgumentType.getInteger(ctx, "value")
    override fun formatValue(value: Int): String = value.toString()
}

// --- 浮点数类型 (如激素水平) ---
class JRFloatProperty(
    name: String, displayName: String,
    getter: (Pregnant) -> Float, setter: (Pregnant, Float) -> Unit
) : JRProperty<Float>(name, displayName, getter, setter) {
    override fun writeToBuf(buf: RegistryByteBuf, value: Float) = buf.writeFloat(value)
    override fun readFromBuf(buf: RegistryByteBuf): Float = buf.readFloat()
    override fun getArgumentType() = FloatArgumentType.floatArg(0f)
    override fun getArgumentFromContext(ctx: CommandContext<ServerCommandSource>) = FloatArgumentType.getFloat(ctx, "value")
    override fun formatValue(value: Float): String = String.format("%.2f", value)
}

// --- 双精度浮点数 (如体力 power) ---
class JRDoubleProperty(
    name: String, displayName: String,
    getter: (Pregnant) -> Double, setter: (Pregnant, Double) -> Unit
) : JRProperty<Double>(name, displayName, getter, setter) {
    override fun writeToBuf(buf: RegistryByteBuf, value: Double) = buf.writeDouble(value)
    override fun readFromBuf(buf: RegistryByteBuf): Double = buf.readDouble()
    override fun getArgumentType() = DoubleArgumentType.doubleArg(0.0)
    override fun getArgumentFromContext(ctx: CommandContext<ServerCommandSource>) = DoubleArgumentType.getDouble(ctx, "value")
    override fun formatValue(value: Double): String = String.format("%.2f", value)
}

// --- 特殊类型：可选实体类型 (胚胎种类) ---
class JREntityTypeProperty(
    name: String, displayName: String,
    getter: (Pregnant) -> Optional<EntityType<*>>,
    setter: (Pregnant, Optional<EntityType<*>>) -> Unit
) : JRProperty<Optional<EntityType<*>>>(name, displayName, getter, setter) {
    override fun writeToBuf(buf: RegistryByteBuf, value: Optional<EntityType<*>>): PacketByteBuf? {
        PacketCodecs.optional(PacketCodecs.registryValue(RegistryKeys.ENTITY_TYPE))
            .encode(buf, value)
        return buf
    }
    override fun readFromBuf(buf: RegistryByteBuf): Optional<EntityType<*>> {
        return PacketCodecs.optional(PacketCodecs.registryValue(RegistryKeys.ENTITY_TYPE))
            .decode(buf)
    }
    override fun getArgumentType(): ArgumentType<Optional<EntityType<*>>> {
        throw UnsupportedOperationException("实体类型暂不支持自动生成命令参数")
    }
    override fun getArgumentFromContext(ctx: CommandContext<ServerCommandSource>): Optional<EntityType<*>> {
        throw UnsupportedOperationException()
    }
    override fun formatValue(value: Optional<EntityType<*>>): String {
        return if (value.isPresent) value.get().name.string else "无"
    }
    override fun registerCommand(): LiteralArgumentBuilder<ServerCommandSource>? {
        return null
    }
}