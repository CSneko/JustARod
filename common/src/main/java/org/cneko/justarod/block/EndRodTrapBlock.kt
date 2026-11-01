package org.cneko.justarod.block

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.phys.BlockHitResult
import org.cneko.justarod.JRIds
import org.cneko.justarod.item.rod.EndRodItem
import org.cneko.justarod.item.rod.GiantRodItem

/**
 * 末地烛陷阱：
 * - 主手拿任意末地烛（巨型除外）右键：把烛装进去（已有一根则把旧的掉出来）。
 * - 主手拿任意完整方块右键：伪装成那个方块（类似机械动力的伪装方块）。
 * - 空手右键：取出里面的末地烛；潜行 + 空手右键：取下伪装。
 * - 装着烛的时候被玩家踩到顶面（Block#stepOn，站在它的上面而不是飞在上方）：
 *   把烛塞进玩家副手——副手空就直接放；副手有东西就先把副手物品挪到背包空位；
 *   背包也满了就把副手物品丢在玩家脚下，再把烛放进副手。
 */
class EndRodTrapBlock : Block(
    JRIds.blockProps("end_rod_trap")
        .strength(3.5f)
        .requiresCorrectToolForDrops()
        .sound(SoundType.STONE)
), EntityBlock {

    init {
        registerDefaultState(stateDefinition.any().setValue(MIMICKED, false))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(MIMICKED)
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        EndRodTrapBlockEntity(JRBlockEntities.END_ROD_TRAP, pos, state)

    // ===== 右键交互 =====

    public override fun useItemOn(
        stack: ItemStack,
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        // 只处理主手，副手的同一次右键不重复执行
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS
        // 潜行时把手上的方块交还给原版：这样才能在陷阱上面继续叠方块
        if (player.isSecondaryUseActive()) return InteractionResult.PASS
        val be = level.getBlockEntity(pos) as? EndRodTrapBlockEntity ?: return InteractionResult.PASS

        // 1. 装填末地烛
        if (isAllowedRod(stack)) {
            if (level.isClientSide) return InteractionResult.SUCCESS
            // 已经有一根：旧的掉在方块位置（不吞东西）
            if (!be.rod.isEmpty) {
                Block.popResource(level, pos, be.rod.copy())
            }
            be.setRod(stack.copyWithCount(1))
            // 陷阱是个容器：装填永远从手上扣一根，创造模式也一样。
            // 否则「空手取出 → 右键装回（不扣）」就能无限刷烛。
            stack.shrink(1)
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8f, 1.2f)
            return InteractionResult.SUCCESS
        }

        // 2. 伪装成手上的完整方块
        val blockItem = stack.item as? BlockItem
        if (blockItem != null) {
            val disguise = blockItem.block.defaultBlockState()
            if (!canMimic(disguise, level, pos)) return InteractionResult.PASS
            if (level.isClientSide) return InteractionResult.SUCCESS
            be.setDisguise(disguise)
            // 同理：伪装用的方块也算存进容器，创造模式一样扣
            stack.shrink(1)
            level.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.9f, 1.1f)
            return InteractionResult.SUCCESS
        }

        return InteractionResult.PASS
    }

    public override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hit: BlockHitResult
    ): InteractionResult {
        val be = level.getBlockEntity(pos) as? EndRodTrapBlockEntity ?: return InteractionResult.PASS

        // 潜行 + 空手：取下伪装，方块还到主手（此时主手本来就是空的）
        if (player.isShiftKeyDown && be.disguise != null) {
            val disguise = be.disguise!!
            if (level.isClientSide) return InteractionResult.SUCCESS
            be.setDisguise(null)
            val item = disguise.block.asItem()
            if (item !== Items.AIR) {
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack(item))
            }
            level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.7f, 1.3f)
            return InteractionResult.SUCCESS
        }

        // 空手：取出里面的末地烛
        if (!be.rod.isEmpty) {
            if (level.isClientSide) return InteractionResult.SUCCESS
            val rod = be.takeRod()
            player.setItemInHand(InteractionHand.MAIN_HAND, rod)
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.9f, 1.0f)
            return InteractionResult.SUCCESS
        }

        return InteractionResult.PASS
    }

    // ===== 踩上去触发 =====

    override fun stepOn(level: Level, pos: BlockPos, state: BlockState, entity: Entity) {
        if (level.isClientSide) return
        if (entity !is ServerPlayer) return
        val be = level.getBlockEntity(pos) as? EndRodTrapBlockEntity ?: return
        if (be.rod.isEmpty) return
        // 同一脚只触发一次：相邻触发间隔太近就直接忽略（防止同一 tick 链式取走多根）
        val now = level.gameTime
        if (now - be.lastTriggerTick < TRIGGER_COOLDOWN_TICKS) return
        be.lastTriggerTick = now

        val rod = be.takeRod()
        giveToOffhand(entity, rod)
        level.playSound(null, pos, SoundEvents.DISPENSER_DISPENSE, SoundSource.BLOCKS, 1.0f, 0.8f)
    }

    /**
     * 把 [rod] 放进玩家副手：
     * 副手空 → 直接放；副手有东西 → 先挪到背包空位；背包满了 → 把副手物品丢在玩家脚下。
     */
    private fun giveToOffhand(player: ServerPlayer, rod: ItemStack) {
        val offhand = player.getItemInHand(InteractionHand.OFF_HAND)
        if (!offhand.isEmpty) {
            val inventory = player.inventory
            val free = inventory.freeSlot
            if (free >= 0) {
                inventory.setItem(free, offhand.copy())
            } else {
                player.drop(offhand.copy(), false)
            }
        }
        player.setItemInHand(InteractionHand.OFF_HAND, rod)
        player.inventory.setChanged()
    }

    // ===== 掉落 =====

    override fun playerDestroy(
        level: Level,
        player: Player,
        pos: BlockPos,
        state: BlockState,
        blockEntity: BlockEntity?,
        tool: ItemStack
    ) {
        if (level.isClientSide) return
        // 创造模式不掉落（和原版一致）
        if (player.hasInfiniteMaterials()) return
        // 没带正确的工具就不掉（像石头一样）
        if (!player.hasCorrectToolForDrops(state)) return

        val be = blockEntity as? EndRodTrapBlockEntity
        // 陷阱本体
        Block.popResource(level, pos, ItemStack(JRBlocks.END_ROD_TRAP.asItem()))
        // 里面装着的末地烛（保留组件）
        if (be != null && !be.rod.isEmpty) Block.popResource(level, pos, be.rod.copy())
        // 伪装用的方块
        be?.disguise?.let { disguise ->
            val item = disguise.block.asItem()
            if (item !== Items.AIR) Block.popResource(level, pos, ItemStack(item))
        }
        be?.clearContents()
    }

    companion object {
        /** 是否伪装中；true 时本体模型隐藏，交给渲染器画伪装方块。 */
        val MIMICKED: BooleanProperty = BooleanProperty.create("mimicked")

        /** 两次触发之间至少间隔的 tick 数（1 秒）：一脚踩下去只会拿走一根。
         *  也能挡住「一边站在上面一边右键补装」这种短时间连续触发。 */
        const val TRIGGER_COOLDOWN_TICKS: Long = 20L

        /** 能不能装进来：任意末地烛，巨型末地烛除外。 */
        fun isAllowedRod(stack: ItemStack): Boolean {
            val item = stack.item
            return item is EndRodItem && item !is GiantRodItem
        }

        /** 能不能当伪装：必须是完整立方体方块，且不能是陷阱自己。 */
        fun canMimic(disguise: BlockState, level: Level, pos: BlockPos): Boolean {
            if (disguise.block is EndRodTrapBlock) return false
            if (disguise.isAir) return false
            return disguise.isCollisionShapeFullBlock(level, pos)
        }
    }
}
