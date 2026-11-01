package org.cneko.justarod.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.cneko.justarod.block.EndRodTrapBlock;
import org.cneko.justarod.block.EndRodTrapBlockEntity;
import org.cneko.justarod.block.JRBlocks;
import org.cneko.justarod.client.renderer.EndRodTrapRenderer;
import org.cneko.justarod.item.JRItems;

/**
 * 客户端回归：伪装方块必须真的被渲染出来（本体会把发射器模型藏掉）。
 * 这里建一个伪装成金块的陷阱，旁边放一个真金块做对照，然后截图。
 */
public class EndRodTrapClientGameTests implements FabricClientGameTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            var server = world.getServer();
            server.runOnServer(game -> {
                var level = game.overworld();
                var player = game.getPlayerList().getPlayers().getFirst();
                for (int x = -5; x <= 5; x++) {
                    for (int z = -5; z <= 5; z++) {
                        level.setBlockAndUpdate(new BlockPos(x, 79, z), Blocks.STONE.defaultBlockState());
                    }
                }
                player.teleportTo(0.5, 80.0, -4.5);
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

                BlockPos trapPos = new BlockPos(0, 80, 0);
                level.setBlockAndUpdate(trapPos, JRBlocks.END_ROD_TRAP.defaultBlockState());
                EndRodTrapBlockEntity be = (EndRodTrapBlockEntity) level.getBlockEntity(trapPos);
                check(be != null, "The trap block entity exists on the server");
                be.setRod(new ItemStack(JRItems.Companion.getSLIME_ROD()));
                be.setDisguise(Blocks.GOLD_BLOCK.defaultBlockState());

                // 对照：正常放置的金块
                level.setBlockAndUpdate(new BlockPos(2, 80, 0), Blocks.GOLD_BLOCK.defaultBlockState());

                // 没伪装的陷阱 + 一个真正朝上的发射器做对照
                level.setBlockAndUpdate(new BlockPos(-2, 80, 0), JRBlocks.END_ROD_TRAP.defaultBlockState());
                level.setBlockAndUpdate(new BlockPos(-4, 80, 0), Blocks.DISPENSER.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.DispenserBlock.FACING, net.minecraft.core.Direction.UP));
            });

            // 等区块与方块实体同步到客户端，并且真的渲染几帧
            context.waitFor(mc -> mc.level != null && mc.level.getBlockEntity(new BlockPos(0, 80, 0)) instanceof EndRodTrapBlockEntity);
            context.runOnClient(mc -> {
                var be = (EndRodTrapBlockEntity) mc.level.getBlockEntity(new BlockPos(0, 80, 0));
                check(be.getDisguise() != null && be.getDisguise().is(Blocks.GOLD_BLOCK),
                        "The disguise is synced to the client, got " + be.getDisguise());
                check(mc.level.getBlockState(new BlockPos(0, 80, 0)).getValue(EndRodTrapBlock.Companion.getMIMICKED()),
                        "The client block state is in the mimicked state");
                var renderer = mc.getBlockEntityRenderDispatcher().getRenderer(be);
                check(renderer != null && renderer.getClass() == EndRodTrapRenderer.class,
                        "The disguise renderer is registered, got " + (renderer == null ? "null" : renderer.getClass()));
                var state = mc.getBlockEntityRenderDispatcher().tryExtractRenderState(be, 0.5f, null);
                check(state instanceof org.cneko.justarod.client.renderer.EndRodTrapRenderState trapState
                                && trapState.getDisguise() != null,
                        "The render state extracts the disguise, got " + state);
                var direct = new org.cneko.justarod.client.renderer.EndRodTrapRenderState();
                net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState.extractBase(be, direct, null);
                check(state != null && state.lightCoords != 0,
                        "The disguise render state carries light, got " + (state == null ? "null" : state.lightCoords));
            });

            // 多渲染几帧：渲染器如果写错，这里会直接崩。
            context.waitTicks(20);

            // 让玩家透过准星看着陷阱，再截一张图便于人工确认。
            context.runOnClient(mc -> {
                if (mc.player != null) {
                    mc.player.setYRot(0f);
                    mc.player.setXRot(15f);
                }
            });
            context.waitTicks(5);
            var shot = context.takeScreenshot("end_rod_trap_disguise");
            check(shot != null, "A screenshot of the disguised trap is taken");
            System.out.println("[EndRodTrapClientGameTests] screenshot: " + shot);

            // ===== 真实玩家站在陷阱上（走原版 Entity#move -> stepOn 的路径） =====
            server.runOnServer(game -> {
                var level = game.overworld();
                var player = game.getPlayerList().getPlayers().getFirst();
                player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
                BlockPos p = new BlockPos(0, 80, 2);
                level.setBlockAndUpdate(p, JRBlocks.END_ROD_TRAP.defaultBlockState());
                EndRodTrapBlockEntity be = (EndRodTrapBlockEntity) level.getBlockEntity(p);
                check(be != null, "The trigger test trap has a block entity");

                // 走真实的右键装填路径（创造模式：手上那根不会消耗）
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JRItems.Companion.getSLIME_ROD()));
                var trapBlock = (EndRodTrapBlock) JRBlocks.END_ROD_TRAP;
                trapBlock.useItemOn(player.getMainHandItem(), level.getBlockState(p), level, p, player,
                        InteractionHand.MAIN_HAND,
                        new net.minecraft.world.phys.BlockHitResult(
                                net.minecraft.world.phys.Vec3.atCenterOf(p), net.minecraft.core.Direction.UP, p, false));
                check(!be.getRod().isEmpty(), "Right-clicking with a rod fills the trap");
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                player.teleportTo(0.5, 81.0, 2.5);
                System.out.println("[TRAPDBG] placed trap rod=" + be.getRod() + " player=" + player.position());
            });
            context.waitTicks(30);
            int rodsOnServer = server.computeOnServer(game -> {
                var player = game.getPlayerList().getPlayers().getFirst();
                var level = game.overworld();
                var be = (EndRodTrapBlockEntity) level.getBlockEntity(new BlockPos(0, 80, 2));
                int total = 0;
                for (int i = 0; i < 36; i++) {
                    if (player.getInventory().getItem(i).is(JRItems.Companion.getSLIME_ROD())) {
                        total += player.getInventory().getItem(i).getCount();
                    }
                }
                if (player.getOffhandItem().is(JRItems.Companion.getSLIME_ROD())) total += player.getOffhandItem().getCount();
                if (player.getMainHandItem().is(JRItems.Companion.getSLIME_ROD())) total += player.getMainHandItem().getCount();
                System.out.println("[TRAPDBG] after 30 ticks: trapRod=" + be.getRod()
                        + " playerRods=" + total
                        + " offhand=" + player.getOffhandItem()
                        + " onGround=" + player.onGround()
                        + " pos=" + player.position());
                return total;
            });
            check(rodsOnServer == 1, "Standing on a filled trap gives exactly one rod, got " + rodsOnServer);
        }
    }
}
