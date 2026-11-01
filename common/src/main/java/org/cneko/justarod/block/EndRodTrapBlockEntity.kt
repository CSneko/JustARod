package org.cneko.justarod.block

import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.ProblemReporter
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.TagValueOutput
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput

/**
 * 末地烛陷阱的方块实体：
 * - [rod]：里面装着的末地烛（保留原来的组件：耐久 / 主人 / 速度……）
 * - [disguise]：伪装成的方块状态（null = 没伪装，显示朝上的发射器）
 *
 * 两者都要同步到客户端：伪装状态决定客户端渲染器画哪个方块模型。
 */
class EndRodTrapBlockEntity(
    type: BlockEntityType<*>,
    pos: BlockPos,
    state: BlockState
) : BlockEntity(type, pos, state) {

    /** 里面装着的末地烛，可能是空的。 */
    var rod: ItemStack = ItemStack.EMPTY
        private set

    /** 伪装成的方块状态；null 表示没有伪装。 */
    var disguise: BlockState? = null
        private set

    /** 上一次触发的游戏时间，用来挡住同一脚踩出来的连续触发。
     *  用 -1000 而不是 Long.MIN_VALUE：后者减游戏时间会溢出成负数，导致永远不触发。 */
    var lastTriggerTick: Long = -1000L

    // ===== 读写 =====

    /**
     * 装进一根末地烛（调用方负责把旧的那根处理掉）。
     * 不管调用方传进来多大的堆叠，这里都只存一根——陷阱永远只装一根烛。
     */
    fun setRod(stack: ItemStack) {
        rod = if (stack.isEmpty) ItemStack.EMPTY else stack.copyWithCount(1)
        sync()
    }

    /** 取走里面的末地烛；永远只给一根。 */
    fun takeRod(): ItemStack {
        val taken = if (rod.isEmpty) ItemStack.EMPTY else rod.copyWithCount(1)
        rod = ItemStack.EMPTY
        sync()
        return taken
    }

    /** 设置 / 清除伪装；同时更新方块状态的 `mimicked` 让本体模型让位给渲染器。 */
    fun setDisguise(state: BlockState?) {
        disguise = state
        val level = level ?: return
        val current = blockState
        if (level.getBlockState(blockPos).block !== current.block) return
        level.setBlock(blockPos, current.setValue(EndRodTrapBlock.MIMICKED, state != null), Block.UPDATE_ALL)
        setChanged()
    }

    /** 方块被破坏时清空内容，避免掉落逻辑重复执行。 */
    fun clearContents() {
        rod = ItemStack.EMPTY
        disguise = null
    }

    private fun sync() {
        setChanged()
        val level = level ?: return
        if (level is ServerLevel) {
            level.sendBlockUpdated(blockPos, blockState, blockState, Block.UPDATE_ALL)
        }
    }

    override fun saveAdditional(output: ValueOutput) {
        super.saveAdditional(output)
        if (!rod.isEmpty) output.store("rod", ItemStack.CODEC, rod)
        disguise?.let { output.store("disguise", BlockState.CODEC, it) }
    }

    override fun loadAdditional(input: ValueInput) {
        super.loadAdditional(input)
        // 老存档里可能存着整个堆叠，读取时同样只留一根
        val loaded = input.read("rod", ItemStack.CODEC).orElse(ItemStack.EMPTY)
        rod = if (loaded.isEmpty) ItemStack.EMPTY else loaded.copyWithCount(1)
        disguise = input.read("disguise", BlockState.CODEC).orElse(null)
    }

    // ===== 同步到客户端（渲染需要知道伪装成什么） =====

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries)
        saveAdditional(output)
        return output.buildResult()
    }

    override fun getUpdatePacket(): Packet<ClientGamePacketListener> =
        ClientboundBlockEntityDataPacket.create(this)
}
