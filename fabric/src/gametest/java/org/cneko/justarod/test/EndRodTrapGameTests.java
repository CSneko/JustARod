package org.cneko.justarod.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.cneko.justarod.block.EndRodTrapBlock;
import org.cneko.justarod.block.EndRodTrapBlockEntity;
import org.cneko.justarod.block.JRBlocks;
import org.cneko.justarod.item.JRItems;

import java.util.List;

/** 末地烛陷阱：装填 / 取出 / 伪装 / 踩上去塞副手 / 掉落。 */
public class EndRodTrapGameTests {

    private static EndRodTrapBlock trap() {
        return (EndRodTrapBlock) JRBlocks.END_ROD_TRAP;
    }

    /** 在 (1,1,1) 放一个陷阱，返回它的坐标。 */
    private static BlockPos placeTrap(GameTestHelper h) {
        h.setBlock(1, 1, 1, JRBlocks.END_ROD_TRAP);
        return h.absolutePos(new BlockPos(1, 1, 1));
    }

    private static EndRodTrapBlockEntity be(GameTestHelper h, BlockPos pos) {
        return (EndRodTrapBlockEntity) h.getLevel().getBlockEntity(pos);
    }

    private static BlockHitResult hit(BlockPos pos) {
        return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
    }

    private static ServerPlayer survivalPlayer(GameTestHelper h) {
        ServerPlayer player = MockPlayers.serverPlayerInLevel(h);
        player.setGameMode(GameType.SURVIVAL);
        player.getAbilities().instabuild = false;
        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        return player;
    }

    private static ItemStack filledRod() {
        return new ItemStack(JRItems.Companion.getSLIME_ROD());
    }

    /** 把玩家挪到陷阱旁边：模拟玩家真的走到陷阱上（掉落物也要落在已加载的区块里）。 */
    private static void standOnTrap(ServerPlayer player, GameTestHelper h, BlockPos pos) {
        player.teleportTo(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
    }

    /** 玩家身上（36 格主背包 + 副手）某物品的总数。
     *  注意：主手物品本来就占着快捷栏的那一格，不能再单独加一次。 */
    private static int countItem(ServerPlayer player, net.minecraft.world.item.Item item) {
        int total = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) total += stack.getCount();
        }
        if (player.getOffhandItem().is(item)) total += player.getOffhandItem().getCount();
        return total;
    }

    @GameTest
    public void fillsAndTakesOutWithEmptyHand(GameTestHelper h) {
        ServerPlayer player = survivalPlayer(h);
        BlockPos pos = placeTrap(h);
        EndRodTrapBlockEntity be = be(h, pos);
        h.assertTrue(be != null, "The trap block must have a block entity");

        player.setItemInHand(InteractionHand.MAIN_HAND, filledRod());
        InteractionResult result = trap().useItemOn(
                player.getMainHandItem(), h.getLevel().getBlockState(pos), h.getLevel(), pos,
                player, InteractionHand.MAIN_HAND, hit(pos));
        h.assertTrue(result == InteractionResult.SUCCESS, "A rod in the main hand fills the trap, got " + result);
        h.assertTrue(be.getRod().is(JRItems.Companion.getSLIME_ROD()), "The trap now holds the slime rod");
        h.assertTrue(player.getMainHandItem().isEmpty(), "Survival players have the rod consumed");

        // 空手右键取出，放到主手。
        InteractionResult taken = trap().useWithoutItem(
                h.getLevel().getBlockState(pos), h.getLevel(), pos, player, hit(pos));
        h.assertTrue(taken == InteractionResult.SUCCESS, "An empty hand takes the rod out");
        h.assertTrue(be.getRod().isEmpty(), "The trap is empty again");
        h.assertTrue(player.getMainHandItem().is(JRItems.Companion.getSLIME_ROD()), "The rod is back in the main hand");
        h.succeed();
    }

    @GameTest
    public void rejectsGiantRodAndIncompleteBlocks(GameTestHelper h) {
        ServerPlayer player = survivalPlayer(h);
        BlockPos pos = placeTrap(h);
        EndRodTrapBlockEntity be = be(h, pos);

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JRItems.Companion.getGIANT_ROD()));
        h.assertTrue(trap().useItemOn(player.getMainHandItem(), h.getLevel().getBlockState(pos), h.getLevel(), pos,
                        player, InteractionHand.MAIN_HAND, hit(pos)) != InteractionResult.SUCCESS,
                "The giant rod cannot be stored");
        h.assertTrue(be.getRod().isEmpty(), "Nothing was stored");

        // 火把不是完整方块，不能当伪装。
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.TORCH));
        h.assertTrue(trap().useItemOn(player.getMainHandItem(), h.getLevel().getBlockState(pos), h.getLevel(), pos,
                        player, InteractionHand.MAIN_HAND, hit(pos)) != InteractionResult.SUCCESS,
                "A torch is not a full block and cannot be used as a disguise");
        h.assertTrue(be.getDisguise() == null, "No disguise was applied");
        h.succeed();
    }

    @GameTest
    public void mimicsFullBlockAndRestoresIt(GameTestHelper h) {
        ServerPlayer player = survivalPlayer(h);
        BlockPos pos = placeTrap(h);
        EndRodTrapBlockEntity be = be(h, pos);

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE, 2));
        InteractionResult r = trap().useItemOn(
                player.getMainHandItem(), h.getLevel().getBlockState(pos), h.getLevel(), pos,
                player, InteractionHand.MAIN_HAND, hit(pos));
        h.assertTrue(r == InteractionResult.SUCCESS, "Stone disguises the trap");
        BlockState disguise = be.getDisguise();
        h.assertTrue(disguise != null && disguise.is(Blocks.STONE), "The disguise is stone, got " + disguise);
        h.assertTrue(h.getLevel().getBlockState(pos).getValue(EndRodTrapBlock.Companion.getMIMICKED()),
                "The block state switches to mimicked so the dispenser model is hidden");
        h.assertTrue(player.getMainHandItem().getCount() == 1, "Exactly one stone is consumed");

        // 潜行 + 空手取下伪装，方块还到主手。
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setShiftKeyDown(true);
        InteractionResult u = trap().useWithoutItem(
                h.getLevel().getBlockState(pos), h.getLevel(), pos, player, hit(pos));
        h.assertTrue(u == InteractionResult.SUCCESS, "Shift + empty hand removes the disguise");
        h.assertTrue(be.getDisguise() == null, "The disguise is cleared");
        h.assertFalse(h.getLevel().getBlockState(pos).getValue(EndRodTrapBlock.Companion.getMIMICKED()),
                "The block state goes back to the dispenser model");
        h.assertTrue(player.getMainHandItem().is(Items.STONE), "The disguise block is handed back");
        h.succeed();
    }

    /** 踩上去：副手空 → 直接放进副手。 */
    @GameTest
    public void steppingOnTrapPutsRodInEmptyOffhand(GameTestHelper h) {
        ServerPlayer player = survivalPlayer(h);
        BlockPos pos = placeTrap(h);
        EndRodTrapBlockEntity be = be(h, pos);
        be.setRod(filledRod());

        standOnTrap(player, h, pos);
        trap().stepOn(h.getLevel(), pos, h.getLevel().getBlockState(pos), player);

        h.assertTrue(be.getRod().isEmpty(), "The trap is emptied after triggering");
        h.assertTrue(player.getOffhandItem().is(JRItems.Companion.getSLIME_ROD()),
                "The rod lands in the off hand, got " + player.getOffhandItem());
        h.assertTrue(player.getMainHandItem().isEmpty(), "The main hand is untouched");
        h.succeed();
    }

    /** 一直站在陷阱上（原版每 tick 都会调 stepOn）也只能拿到一根。 */
    @GameTest
    public void steppingRepeatedlyOnlyGivesOneRod(GameTestHelper h) {
        ServerPlayer player = survivalPlayer(h);
        BlockPos pos = placeTrap(h);
        EndRodTrapBlockEntity be = be(h, pos);
        be.setRod(filledRod());

        standOnTrap(player, h, pos);
        for (int i = 0; i < 40; i++) {
            trap().stepOn(h.getLevel(), pos, h.getLevel().getBlockState(pos), player);
            if (i == 0) {
                h.assertTrue(be.getRod().isEmpty(), "The trap is emptied on the very first step");
            }
        }

        int rods = countItem(player, JRItems.Companion.getSLIME_ROD());
        h.assertTrue(rods == 1, "Standing on the trap must only ever hand out one rod, got " + rods
                + " offhand=" + player.getOffhandItem());
        h.succeed();
    }

    /** 创造模式不能靠「取出 → 装回」循环复制末地烛：装填在创造模式同样消耗。 */
    @GameTest
    public void creativeCannotDuplicateRods(GameTestHelper h) {
        ServerPlayer player = survivalPlayer(h);
        player.setGameMode(GameType.CREATIVE);
        player.getAbilities().instabuild = true;

        BlockPos pos = placeTrap(h);
        EndRodTrapBlockEntity be = be(h, pos);
        be.setRod(filledRod());
        BlockState state = h.getLevel().getBlockState(pos);

        // 空手取出：得到一根
        trap().useWithoutItem(state, h.getLevel(), pos, player, hit(pos));
        h.assertTrue(countItem(player, JRItems.Companion.getSLIME_ROD()) == 1,
                "Taking the rod out hands out one rod, got " + countItem(player, JRItems.Companion.getSLIME_ROD()));

        // 拿在手上再装回去：创造模式也要扣掉手上那根，不能白送一份进陷阱
        trap().useItemOn(player.getMainHandItem(), state, h.getLevel(), pos, player,
                InteractionHand.MAIN_HAND, hit(pos));
        h.assertTrue(countItem(player, JRItems.Companion.getSLIME_ROD()) == 0,
                "Filling consumes the held rod even in creative, got "
                        + countItem(player, JRItems.Companion.getSLIME_ROD()));
        h.assertFalse(be.getRod().isEmpty(), "The trap now holds the rod");

        // 再取出：整个来回之后净增 0
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        trap().useWithoutItem(state, h.getLevel(), pos, player, hit(pos));
        h.assertTrue(countItem(player, JRItems.Companion.getSLIME_ROD()) == 1,
                "A full out-in-out cycle still leaves exactly one rod, got "
                        + countItem(player, JRItems.Companion.getSLIME_ROD()));
        h.succeed();
    }

    /** 踩上去：副手有东西、背包有空位 → 副手物品进背包，烛进副手。 */
    @GameTest
    public void steppingOnTrapMovesOffhandIntoInventory(GameTestHelper h) {
        ServerPlayer player = survivalPlayer(h);
        BlockPos pos = placeTrap(h);
        EndRodTrapBlockEntity be = be(h, pos);
        be.setRod(filledRod());

        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STICK, 3));
        standOnTrap(player, h, pos);
        trap().stepOn(h.getLevel(), pos, h.getLevel().getBlockState(pos), player);

        h.assertTrue(player.getOffhandItem().is(JRItems.Companion.getSLIME_ROD()),
                "The rod replaces the off hand item");
        h.assertTrue(player.getInventory().contains(new ItemStack(Items.STICK)),
                "The stick was moved into a free inventory slot");
        List<ItemEntity> drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(4));
        h.assertTrue(drops.isEmpty(), "Nothing had to be dropped, got " + drops.size());
        h.succeed();
    }

    /** 踩上去：副手有东西、背包也满 → 副手物品被丢在玩家脚下，烛进副手。 */
    @GameTest
    public void steppingOnTrapDropsOffhandWhenInventoryFull(GameTestHelper h) {
        ServerPlayer player = survivalPlayer(h);
        BlockPos pos = placeTrap(h);
        EndRodTrapBlockEntity be = be(h, pos);
        be.setRod(filledRod());

        for (int i = 0; i < 36; i++) {
            player.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 1));
        }
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STICK, 3));
        standOnTrap(player, h, pos);
        trap().stepOn(h.getLevel(), pos, h.getLevel().getBlockState(pos), player);

        h.assertTrue(player.getOffhandItem().is(JRItems.Companion.getSLIME_ROD()),
                "The rod still ends up in the off hand, got " + player.getOffhandItem());
        h.assertTrue(player.getInventory().getFreeSlot() == -1, "The inventory is still full");
        java.util.List<ItemEntity> all = new java.util.ArrayList<>();
        for (var e : h.getLevel().getAllEntities()) {
            if (e instanceof ItemEntity item) all.add(item);
        }
        boolean stickDropped = all.stream().anyMatch(e -> e.getItem().is(Items.STICK));
        h.assertTrue(stickDropped, "The previous off hand item was dropped, item entities in level=" + all.size()
                + " at " + player.position() + " items=" + all.stream().map(e -> e.getItem().toString()).toList());
        h.succeed();
    }

    /** 挖掉时：陷阱本体、里面的烛、伪装方块分别掉出来。 */
    @GameTest
    public void breakingDropsTrapRodAndDisguise(GameTestHelper h) {
        ServerPlayer player = survivalPlayer(h);
        BlockPos pos = placeTrap(h);
        EndRodTrapBlockEntity be = be(h, pos);
        be.setRod(filledRod());
        be.setDisguise(Blocks.STONE.defaultBlockState());

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
        BlockState state = h.getLevel().getBlockState(pos);
        trap().playerDestroy(h.getLevel(), player, pos, state, be, player.getMainHandItem());

        List<ItemEntity> drops = h.getLevel().getEntitiesOfClass(ItemEntity.class,
                new net.minecraft.world.phys.AABB(pos).inflate(3));
        boolean trapItem = drops.stream().anyMatch(e -> e.getItem().is(JRBlocks.END_ROD_TRAP.asItem()));
        boolean rodItem = drops.stream().anyMatch(e -> e.getItem().is(JRItems.Companion.getSLIME_ROD()));
        boolean stoneItem = drops.stream().anyMatch(e -> e.getItem().is(Items.STONE));
        h.assertTrue(trapItem, "The trap block itself drops");
        h.assertTrue(rodItem, "The stored rod drops, drops=" + drops.size());
        h.assertTrue(stoneItem, "The disguise block drops");
        h.assertTrue(be.getRod().isEmpty() && be.getDisguise() == null, "The block entity is cleared");
        h.succeed();
    }

    /** 空陷阱不会触发；非玩家实体也不触发。 */
    @GameTest
    public void emptyTrapDoesNotTrigger(GameTestHelper h) {
        ServerPlayer player = survivalPlayer(h);
        BlockPos pos = placeTrap(h);
        EndRodTrapBlockEntity be = be(h, pos);

        standOnTrap(player, h, pos);
        trap().stepOn(h.getLevel(), pos, h.getLevel().getBlockState(pos), player);
        h.assertTrue(player.getOffhandItem().isEmpty(), "An empty trap does nothing");

        be.setRod(filledRod());
        var cow = h.spawn(net.minecraft.world.entity.EntityType.COW, new BlockPos(2, 1, 2));
        trap().stepOn(h.getLevel(), pos, h.getLevel().getBlockState(pos), cow);
        h.assertTrue(!be.getRod().isEmpty(), "Only players can trigger the trap");
        h.succeed();
    }
}
