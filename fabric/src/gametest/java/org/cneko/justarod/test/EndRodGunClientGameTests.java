package org.cneko.justarod.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.cneko.justarod.client.renderer.EndRodProjectileRenderer;
import org.cneko.justarod.client.renderer.EndRodGunAnimation;
import org.cneko.justarod.entity.EndRodProjectile;
import org.cneko.justarod.item.EndRodGunItem;
import org.cneko.justarod.item.EndRodGunItem.Mode;
import org.cneko.justarod.item.JRItems;

/** Real input/network/rendering regression for continuous dual fire and the spinning player pose. */
public class EndRodGunClientGameTests implements FabricClientGameTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            var server = world.getServer();
            server.runOnServer(game -> {
                var level = game.overworld();
                for (int x = -12; x <= 12; x++) for (int z = -12; z <= 12; z++) {
                    level.setBlockAndUpdate(new BlockPos(x, 79, z), Blocks.STONE.defaultBlockState());
                }
                var player = game.getPlayerList().getPlayers().getFirst();
                player.teleportTo(0.5, 80, -4.5);
                for (InteractionHand hand : InteractionHand.values()) {
                    ItemStack gun = new ItemStack(JRItems.Companion.getEND_ROD_GUN());
                    EndRodGunItem.setMode(gun, Mode.NORMAL);
                    player.setItemInHand(hand, gun);
                }
            });
            context.waitFor(mc -> EndRodGunItem.isDualWielding(mc.player)
                    && EndRodGunItem.getMode(mc.player.getMainHandItem()) == Mode.NORMAL);
            context.getInput().lookAt(0, 0);
            context.runOnClient(mc -> {
                verifyHoldingPose(mc);
                verifyFirstPersonRenderer(mc);
            });
            context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
            context.waitTicks(2);
            context.takeScreenshot("end-rod-gun-holding");
            context.getInput().holdMouse(1);
            context.waitTicks(8);
            context.waitFor(mc -> EndRodGunAnimation.recoil(mc.player, mc.player.getMainArm(), 0.35f) > 0);
            context.runOnClient(mc -> {
                check(EndRodGunAnimation.aimAmount(mc.player, 0.5f) == 1, "The arms finish raising into the aiming pose");
                check(EndRodGunAnimation.recoil(mc.player, mc.player.getMainArm().getOpposite(), 0.35f) > 0,
                        "The shot packet animates both guns in a dual volley");
                verifyFirstPersonRenderer(mc);
            });
            context.getInput().releaseMouse(1);
            context.waitTicks(5);
            context.runOnClient(mc -> check(EndRodGunAnimation.recoil(mc.player, mc.player.getMainArm(), 0.35f) == 0,
                    "Recoil settles when actual firing stops"));
            server.runOnServer(game -> {
                var player = game.getPlayerList().getPlayers().getFirst();
                for (InteractionHand hand : InteractionHand.values()) EndRodGunItem.setMode(player.getItemInHand(hand), Mode.BIG_BROTHER);
            });
            context.waitFor(mc -> EndRodGunItem.getMode(mc.player.getMainHandItem()) == Mode.BIG_BROTHER);
            context.getInput().holdMouse(1);
            context.waitTicks(8);
            context.runOnClient(mc -> {
                check(EndRodGunItem.isSpinning(mc.player), "Holding right click starts spinning through real client input");
                var renderer = (AvatarRenderer) mc.getEntityRenderDispatcher().getRenderer(mc.player);
                var state = (AvatarRenderState) renderer.createRenderState(mc.player, 0.5f);
                check(Math.abs(state.bodyRot - EndRodGunItem.spinAngle(mc.player, 0.5f)) < 0.01,
                        "Player rendering rotates on shared world time");
                var model = (net.minecraft.client.model.player.PlayerModel) renderer.getModel();
                model.setupAnim(state);
                check(Math.abs(model.rightArm.zRot - Math.PI / 2) < 0.01
                                && Math.abs(model.leftArm.zRot + Math.PI / 2) < 0.01,
                        "The actual player model fully spreads both arms");
                check(model.leftSleeve == model.leftArm.getChild("left_sleeve") && model.leftSleeve.zRot == 0,
                        "Child sleeves inherit the spreading arms without a second rotation");
                var itemState = new net.minecraft.client.renderer.item.ItemStackRenderState();
                mc.getItemModelResolver().updateForNonLiving(itemState, mc.player.getMainHandItem(), ItemDisplayContext.GUI, mc.player);
                check(!itemState.isEmpty(), "The new gun model resolves in the inventory");
            });
            var before = server.computeOnServer(game -> game.getPlayerList().getPlayers().getFirst().position());
            context.getInput().holdKey(options -> options.keyUp);
            context.waitTicks(12);
            context.getInput().releaseKey(options -> options.keyUp);
            server.runOnServer(game -> {
                var player = game.getPlayerList().getPlayers().getFirst();
                check(player.position().subtract(before).horizontalDistance() > 1.0, "The spinning player can still move freely");
            });
            context.takeScreenshot("end-rod-gun-big-brother");
            context.getInput().releaseMouse(1);
            context.waitTicks(3);
            server.runOnServer(game -> {
                var player = game.getPlayerList().getPlayers().getFirst();
                player.teleportTo(0.5, 80, -4.5);
                for (InteractionHand hand : InteractionHand.values()) EndRodGunItem.setMode(player.getItemInHand(hand), Mode.FOCUS);
                var cow = EntityType.COW.create(game.overworld(), EntitySpawnReason.COMMAND);
                check(cow != null, "Focus target can be spawned");
                cow.setPos(0.5, 80, 6.5);
                cow.setNoAi(true);
                game.overworld().addFreshEntity(cow);
            });
            context.waitFor(mc -> EndRodGunItem.getMode(mc.player.getMainHandItem()) == Mode.FOCUS);
            context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
            context.getInput().lookAt(0, 0);
            context.getInput().holdMouse(1);
            context.waitTicks(28);
            context.runOnClient(mc -> {
                var rods = mc.level.getEntitiesOfClass(EndRodProjectile.class, mc.player.getBoundingBox().inflate(48), EndRodProjectile::isGathering);
                check(rods.size() >= 10 && rods.size() < 20, "Focus rods synchronize to the client before the salvo is full");
                var rod = rods.getFirst();
                check(rod.shouldRenderAtSqrDistance(48 * 48), "Small collision boxes do not hide rods at normal locking distances");
                check(mc.getEntityRenderDispatcher().getRenderer(rod) instanceof EndRodProjectileRenderer, "Rods use the oriented 3D renderer");
                var state = (EndRodProjectileRenderer.State) mc.getEntityRenderDispatcher().extractEntity(rod, 0.5f);
                check(!state.item.isEmpty(), "The projectile resolves the vanilla end rod model");
            });
            context.takeScreenshot("end-rod-gun-focus-gathering");
            context.getInput().releaseMouse(1);
            context.waitTicks(5);
            context.runOnClient(mc -> {
                var waiting = mc.level.getEntitiesOfClass(EndRodProjectile.class, mc.player.getBoundingBox().inflate(48), EndRodProjectile::isGathering);
                check(waiting.size() >= 10, "Releasing right click preserves the incomplete focus ring on the client");
            });
            context.getInput().holdMouse(1);
            context.waitTicks(22);
            context.getInput().releaseMouse(1);
            server.runOnServer(game -> {
                var cow = game.overworld().getEntitiesOfClass(net.minecraft.world.entity.animal.cow.Cow.class,
                        game.getPlayerList().getPlayers().getFirst().getBoundingBox().inflate(48)).getFirst();
                check(cow.getHealth() < cow.getMaxHealth(), "The resumed focus batch attacks its actual living target");
            });
            context.takeScreenshot("end-rod-gun-focus-hearts");
            context.waitTicks(35);
            // Keeping the trigger held without a target must not invent shot/recoil pulses.
            server.runOnServer(game -> {
                var player = game.getPlayerList().getPlayers().getFirst();
                player.setYRot(90);
            });
            context.getInput().lookAt(90, 0);
            context.getInput().holdMouse(1);
            context.waitTicks(6);
            context.runOnClient(mc -> check(EndRodGunAnimation.recoil(mc.player, mc.player.getMainArm(), 0.35f) == 0,
                    "Focus without a target keeps the hold pose without fake recoil"));
            context.getInput().releaseMouse(1);
        } finally {
            context.getInput().releaseMouse(1);
            context.getInput().releaseKey(options -> options.keyUp);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void verifyHoldingPose(net.minecraft.client.Minecraft mc) {
        var renderer = (AvatarRenderer) mc.getEntityRenderDispatcher().getRenderer(mc.player);
        var state = (AvatarRenderState) renderer.createRenderState(mc.player, 0.5f);
        var model = (net.minecraft.client.model.player.PlayerModel) renderer.getModel();
        model.setupAnim(state);
        check(model.rightArm.xRot < -1 && model.leftArm.xRot < -1, "Idle dual wielding raises both gun arms");
        var main = mc.player.getMainHandItem();
        var off = mc.player.getOffhandItem();
        try {
            mc.player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            state = (AvatarRenderState) renderer.createRenderState(mc.player, 0.5f);
            model.setupAnim(state);
            check(model.rightArm.xRot < -1 && Math.abs(model.leftArm.xRot) < 0.5f, "Single wielding only poses the armed hand");
            mc.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            mc.player.setItemInHand(InteractionHand.OFF_HAND, off);
            state = (AvatarRenderState) renderer.createRenderState(mc.player, 0.5f);
            model.setupAnim(state);
            check(model.leftArm.xRot < -1 && Math.abs(model.rightArm.xRot) < 0.5f, "An offhand-only gun uses the left arm");
        } finally {
            mc.player.setItemInHand(InteractionHand.MAIN_HAND, main);
            mc.player.setItemInHand(InteractionHand.OFF_HAND, off);
        }
    }

    private static void verifyFirstPersonRenderer(net.minecraft.client.Minecraft mc) {
        int[] geometry = {0};
        var collector = (net.minecraft.client.renderer.SubmitNodeCollector) java.lang.reflect.Proxy.newProxyInstance(
                net.minecraft.client.renderer.SubmitNodeCollector.class.getClassLoader(),
                new Class<?>[]{net.minecraft.client.renderer.SubmitNodeCollector.class}, (proxy, method, args) -> {
                    if (method.getName().equals("order")) return proxy;
                    if (method.getName().equals("submitItem")) geometry[0]++;
                    return null;
                });
        var poses = new com.mojang.blaze3d.vertex.PoseStack();
        for (ItemDisplayContext display : new ItemDisplayContext[]{ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, ItemDisplayContext.FIRST_PERSON_LEFT_HAND}) {
            mc.gameRenderer.itemInHandRenderer.renderItem(mc.player, mc.player.getMainHandItem(), display, poses, collector, 0xF000F0);
            check(poses.last().pose().equals(new org.joml.Matrix4f(), 0.0001f), "The first-person gun animation restores the caller's transform");
        }
        check(geometry[0] >= 2, "The real first-person renderer submits both animated gun models");
    }
}
