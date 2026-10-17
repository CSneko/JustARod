package org.cneko.justarod.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.gametest.framework.GameTestHelper;
import org.cneko.justarod.entity.EndRodProjectile;
import org.cneko.justarod.entity.JREntities;
import org.cneko.justarod.event.EndRodGunEvents;
import org.cneko.justarod.item.EndRodGunItem;
import org.cneko.justarod.item.EndRodGunItem.Mode;
import org.cneko.justarod.item.JRItems;

import java.util.List;

public class EndRodGunGameTests {
    private EndRodGunItem gun() { return JRItems.Companion.getEND_ROD_GUN(); }

    private ServerPlayer player(GameTestHelper h, boolean dual, Mode mode) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        // Work above the test structure's enclosing barrier roof.
        Vec3 position = h.absoluteVec(new Vec3(3.5, 12, 1.5));
        player.teleportTo(position.x, position.y, position.z);
        player.setNoGravity(true);
        player.setYRot(0);
        player.setXRot(0);
        // Embedded mock connections do not supply the normal connection-driven player ticks.
        h.onEachTick(player::doTick);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(gun()));
        if (dual) player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(gun()));
        for (InteractionHand hand : InteractionHand.values()) {
            if (player.getItemInHand(hand).getItem() instanceof EndRodGunItem) EndRodGunItem.setMode(player.getItemInHand(hand), mode);
        }
        return player;
    }

    private List<EndRodProjectile> rods(GameTestHelper h, ServerPlayer player) {
        return h.getLevel().getEntitiesOfClass(EndRodProjectile.class, player.getBoundingBox().inflate(160),
                rod -> rod.getOwner() == player && rod.isAlive());
    }

    @GameTest(maxTicks = 30)
    public void dualContinuousFireHasFourTickCooldownAndNoCost(GameTestHelper h) {
        ServerPlayer player = player(h, true, Mode.NORMAL);
        player.setXRot(-90);
        gun().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        h.assertTrue(rods(h, player).size() == 2, "One trigger fires one rod from each hand");
        player.stopUsingItem();
        gun().use(h.getLevel(), player, InteractionHand.OFF_HAND);
        h.assertTrue(rods(h, player).size() == 2, "Re-clicking with the other hand cannot bypass cooldown");
        h.runAfterDelay(3, () -> {
            h.assertTrue(rods(h, player).size() == 2, "No extra shots before four server ticks, actual=" + rods(h, player).size() + ", using=" + player.isUsingItem());
        });
        h.runAfterDelay(9, () -> {
            player.stopUsingItem();
            h.assertTrue(rods(h, player).size() == 6, "Holding use produces a dual volley every four ticks");
            h.assertTrue(!player.getMainHandItem().isDamageableItem() && !player.getOffhandItem().isDamageableItem(), "Both guns have no durability");
            h.assertTrue(player.getMainHandItem().getCount() == 1 && player.getOffhandItem().getCount() == 1,
                    "Firing without ammunition preserves both guns");
            h.assertTrue(rods(h, player).stream().allMatch(rod -> rod.getDeltaMovement().length() > 3), "Rods retain their high flight speed");
            h.succeed();
        });
    }

    @GameTest
    public void modesSwitchBothHandsAndBigBrotherRequiresDualWield(GameTestHelper h) {
        ServerPlayer player = player(h, true, Mode.NORMAL);
        player.setShiftKeyDown(true);
        gun().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        h.assertTrue(EndRodGunItem.getMode(player.getMainHandItem()) == Mode.BIG_BROTHER
                && EndRodGunItem.getMode(player.getOffhandItem()) == Mode.BIG_BROTHER, "Sneak-use changes both guns together");
        player.setShiftKeyDown(false);
        gun().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        List<EndRodProjectile> rods = rods(h, player);
        h.assertTrue(EndRodGunItem.isSpinning(player) && rods.size() == 2, "Big Brother starts the dual-gun animation and fires");
        h.assertTrue(rods.get(0).getDeltaMovement().normalize().dot(rods.get(1).getDeltaMovement().normalize()) < -0.99,
                "The outstretched arms shoot in opposite radial directions");
        h.assertTrue(player.getYRot() == 0 && player.getXRot() == 0, "Spinning preserves movement controls and camera angles");
        player.stopUsingItem();
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        h.assertTrue(gun().use(h.getLevel(), player, InteractionHand.MAIN_HAND) == InteractionResult.FAIL,
                "Big Brother cannot run without a gun in each hand");
        h.succeed();
    }

    @GameTest(maxTicks = 20)
    public void bigBrotherFiresBothGunsEveryTick(GameTestHelper h) {
        ServerPlayer player = player(h, true, Mode.BIG_BROTHER);
        gun().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        // A second use callback in the same tick must not create an extra volley.
        EndRodGunEvents.fire(player, Mode.BIG_BROTHER);
        h.assertTrue(rods(h, player).size() == 2, "Each gun fires once in the initial tick");
        h.runAfterDelay(3, () -> {
            player.stopUsingItem();
            h.assertTrue(rods(h, player).size() == 8, "Both guns fire every tick, with no four-tick delay");
            h.succeed();
        });
    }

    private EndRodProjectile toward(GameTestHelper h, ServerPlayer player, LivingEntity target) {
        EndRodProjectile rod = new EndRodProjectile(JREntities.END_ROD_PROJECTILE, h.getLevel());
        rod.setOwner(player);
        rod.setPos(target.getX(), target.getY() + 0.5, target.getZ() - 1.5);
        rod.shoot(0, 0, 1, (float) EndRodProjectile.SPEED, 0);
        h.getLevel().addFreshEntity(rod);
        return rod;
    }

    @GameTest(maxTicks = 25)
    public void rapidHitsEachDealPointTwoAndDisappear(GameTestHelper h) {
        ServerPlayer player = player(h, false, Mode.NORMAL);
        var cow = h.spawnWithNoFreeWill(EntityType.COW, 3, 12, 6);
        cow.setNoGravity(true);
        float health = cow.getHealth();
        EndRodProjectile first = toward(h, player, cow);
        h.runAfterDelay(2, () -> {
            h.assertTrue(!first.isAlive(), "A hit removes the rod, without creating recoverable ammunition");
            h.assertTrue(Math.abs(cow.getHealth() - health + 0.2f) < 0.001, "A normal hit deals 0.2 damage");
            toward(h, player, cow);
        });
        h.runAfterDelay(4, () -> {
            h.assertTrue(Math.abs(cow.getHealth() - health + 0.4f) < 0.001,
                    "The 2-tick barrage can damage on every hit despite vanilla hurt cooldown");
            h.succeed();
        });
    }

    @GameTest(maxTicks = 20)
    public void fastRodsStopAtWalls(GameTestHelper h) {
        ServerPlayer player = player(h, false, Mode.NORMAL);
        h.setBlock(3, 13, 5, Blocks.STONE);
        EndRodProjectile rod = new EndRodProjectile(JREntities.END_ROD_PROJECTILE, h.getLevel());
        rod.setOwner(player);
        rod.setPos(h.absoluteVec(new Vec3(3.5, 13.5, 3.5)));
        rod.shoot(0, 0, 1, (float) EndRodProjectile.SPEED, 0);
        h.getLevel().addFreshEntity(rod);
        h.runAfterDelay(2, () -> {
            h.assertTrue(!rod.isAlive(), "Swept collision prevents fast rods tunneling through blocks");
            h.assertTrue(h.getBlockState(new BlockPos(3, 13, 5)).is(Blocks.STONE), "Hitting blocks does not place or destroy blocks");
            h.succeed();
        });
    }

    @GameTest(maxTicks = 180)
    public void focusGathersTwentyBeforeTwoTickBarrage(GameTestHelper h) {
        ServerPlayer player = player(h, true, Mode.FOCUS);
        // Nearby slime integration tests actively hunt prey; keep this target outside their arena.
        player.teleportTo(player.getX(), h.absoluteVec(new Vec3(0, 40, 0)).y, player.getZ());
        var cow = h.spawnWithNoFreeWill(EntityType.COW, 3, 40, 7);
        cow.setNoGravity(true);
        // Match the crosshair to the target's center, then let real player/item/server ticks drive the salvo.
        Vec3 aim = cow.getBoundingBox().getCenter().subtract(player.getEyePosition());
        player.setXRot((float) -Math.toDegrees(Math.atan2(aim.y, aim.horizontalDistance())));
        h.assertTrue(EndRodGunEvents.findTarget(player) == cow, "Crosshair acquires a living target");
        float initialHealth = cow.getHealth();
        gun().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        List<EndRodProjectile> tracked = new java.util.ArrayList<>();
        List<Long> releaseTicks = new java.util.ArrayList<>();
        h.onEachTick(() -> {
            for (EndRodProjectile rod : rods(h, player)) if (!tracked.contains(rod)) tracked.add(rod);
            long released = tracked.stream().filter(rod -> !rod.isGathering()).count();
            if (released > releaseTicks.size()) {
                h.assertTrue(tracked.size() == 20, "No rod attacks before twenty have gathered");
                h.assertTrue(released == releaseTicks.size() + 1, "Only one rod is released at a time");
                if (!releaseTicks.isEmpty()) h.assertTrue(h.getTick() - releaseTicks.getLast() == 2, "Successive releases are exactly two ticks apart");
                releaseTicks.add(h.getTick());
            }
        });
        h.runAfterDelay(34, () -> {
            List<EndRodProjectile> rods = rods(h, player);
            h.assertTrue(rods.size() == 18 && rods.stream().allMatch(EndRodProjectile::isGathering), "Eighteen rods wait without attacking early, actual=" + rods.size() + ", using=" + player.isUsingItem());
            h.assertTrue(cow.getHealth() == initialHealth, "Gathering rods do not harm the target");
        });
        h.runAfterDelay(38, () -> player.stopUsingItem());
        h.succeedWhen(() -> {
            h.assertTrue(cow.getHealth() <= initialHealth - 3.99f, "All twenty released rods deal their 0.2 damage; health="
                    + cow.getHealth() + ", releases=" + releaseTicks.size() + ", vehicle=" + cow.getVehicle());
            h.assertTrue(rods(h, player).isEmpty(), "A full salvo completes after releasing use, leaving no stranded rods");
            h.assertTrue(releaseTicks.size() == 20, "All twenty releases were observed at the requested cadence");
        });
    }

    @GameTest(maxTicks = 30)
    public void releasingFocusKeepsRodsAndAllowsResuming(GameTestHelper h) {
        ServerPlayer player = player(h, false, Mode.FOCUS);
        var cow = h.spawnWithNoFreeWill(EntityType.COW, 3, 12, 7);
        cow.setNoGravity(true);
        gun().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        h.assertTrue(rods(h, player).size() == 1, "Focus begins a gathering salvo");
        player.stopUsingItem();
        h.runAfterDelay(6, () -> {
            h.assertTrue(rods(h, player).size() == 1 && rods(h, player).getFirst().isReady(),
                    "Releasing use leaves the first rod hovering over the target");
            gun().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
            player.stopUsingItem();
        });
        h.runAfterDelay(8, () -> {
            h.assertTrue(rods(h, player).size() == 2, "Another tap adds to the same batch instead of restarting it");
            h.assertTrue(cow.getHealth() == cow.getMaxHealth(), "An incomplete batch does not attack early");
            player.setShiftKeyDown(true);
            gun().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
            h.assertTrue(rods(h, player).isEmpty(), "Switching modes still clears an incomplete focus batch");
            h.succeed();
        });
    }

    @GameTest(maxTicks = 410)
    public void pausedFocusSurvivesFlightLifetimeThenCompletes(GameTestHelper h) {
        ServerPlayer player = player(h, false, Mode.FOCUS);
        player.teleportTo(player.getX(), h.absoluteVec(new Vec3(0, 40, 0)).y, player.getZ());
        var cow = h.spawnWithNoFreeWill(EntityType.COW, 3, 40, 7);
        cow.setNoGravity(true);
        float health = cow.getHealth();
        gun().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        player.stopUsingItem();
        h.runAfterDelay(250, () -> {
            h.assertTrue(rods(h, player).size() == 1 && rods(h, player).getFirst().isReady(),
                    "Gathering rods are not removed by the old 240-tick flight lifetime");
            gun().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        });
        h.runAfterDelay(324, () -> player.stopUsingItem());
        h.succeedWhen(() -> {
            h.assertTrue(cow.getHealth() <= health - 3.99f, "The original waiting rod and nineteen new rods all attack successfully");
            h.assertTrue(rods(h, player).isEmpty(), "The resumed complete batch leaves no orphaned projectiles");
        });
    }

    @GameTest
    public void focusAllowsAimToleranceButCannotLockThroughWalls(GameTestHelper h) {
        ServerPlayer player = player(h, false, Mode.FOCUS);
        var cow = h.spawnWithNoFreeWill(EntityType.COW, 4, 12, 7);
        h.assertTrue(EndRodGunEvents.findTarget(player) == cow, "A slightly off-center target is still acquired");
        for (int x = 2; x <= 5; x++) for (int y = 12; y <= 15; y++) h.setBlock(x, y, 4, Blocks.STONE);
        h.assertTrue(EndRodGunEvents.findTarget(player) == null, "A solid wall blocks target acquisition");
        h.succeed();
    }
}
