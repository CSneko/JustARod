package org.cneko.justarod.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.cneko.justarod.client.slime.SlimeCaptivityClientState;
import org.cneko.justarod.config.JRConfig;
import org.cneko.justarod.entity.JREntities;
import org.cneko.justarod.entity.slime.TamedSlimeEntity;

/** Exercises riding authority, floating input/rendering and real world post-processing with a connected client. */
public class SlimeClientGameTests implements FabricClientGameTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(mc -> {
            mc.options.pauseOnLostFocus = false;
            mc.options.enableVsync().set(false);
        });
        try (var world = context.worldBuilder().create()) {
            var server = world.getServer();
            int slimeId = server.computeOnServer(game -> {
                var level = game.overworld();
                var player = game.getPlayerList().getPlayers().getFirst();
                player.setGameMode(GameType.SURVIVAL);
                for (int x = -10; x <= 10; x++) for (int z = -10; z <= 10; z++)
                    level.setBlockAndUpdate(new BlockPos(x, 3, z), Blocks.STONE.defaultBlockState());
                player.teleportTo(0, 4, 0);
                var slime = JREntities.TAMED_SLIME.create(level, EntitySpawnReason.COMMAND);
                check(slime != null, "Slime entity can be created");
                slime.setTierLevel(3);
                slime.setOwner(player);
                slime.setCommand(TamedSlimeEntity.COMMAND_STAY);
                slime.setPos(0, 4, 0);
                level.addFreshEntity(slime);
                check(slime.swallowOwner(player), "Owner can enter as driver");
                return slime.getId();
            });
            context.waitFor(mc -> mc.player != null && mc.player.getVehicle() instanceof TamedSlimeEntity slime
                    && slime.getId() == slimeId && slime.isDriver(mc.player));
            context.runOnClient(mc -> {
                var slime = (TamedSlimeEntity) mc.player.getVehicle();
                check(!slime.canSimulateMovement(), "Local mounted client must never simulate slime travel");
                check(!slime.isClientAuthoritative(), "Rider must not grant client movement authority");
            });
            var start = server.computeOnServer(game -> game.overworld().getEntity(slimeId).position());
            context.waitTicks(60);
            server.runOnServer(game -> {
                var slime = (TamedSlimeEntity) game.overworld().getEntity(slimeId);
                check(slime.position().subtract(start).horizontalDistanceSqr() < 0.001,
                        "Mounted slime remains still for 60 ticks without player input");
            });
            // Verify actual key packets still drive the server, then stop after release.
            context.getInput().holdKeyFor(options -> options.keyUp, 20);
            context.waitTicks(3);
            var afterDrive = server.computeOnServer(game -> game.overworld().getEntity(slimeId).position());
            check(afterDrive.subtract(start).horizontalDistanceSqr() > 0.1, "Real client WASD still drives the slime");
            context.waitTicks(40);
            server.runOnServer(game -> {
                var slime = (TamedSlimeEntity) game.overworld().getEntity(slimeId);
                check(slime.position().subtract(afterDrive).horizontalDistanceSqr() < 0.01, "Releasing real input stops movement");
                var player = game.getPlayerList().getPlayers().getFirst();
                slime.releaseEntity(player.getUUID());
                check(slime.trySwallow(player), "Owner can enter passively");
                slime.setCommand(TamedSlimeEntity.COMMAND_FOLLOW);
            });
            context.waitFor(mc -> mc.player.getVehicle() instanceof TamedSlimeEntity slime && slime.isCaptive(mc.player));
            var captiveStart = server.computeOnServer(game -> game.overworld().getEntity(slimeId).position());
            context.waitTicks(60);
            server.runOnServer(game -> {
                var slime = (TamedSlimeEntity) game.overworld().getEntity(slimeId);
                check(slime.position().subtract(captiveStart).horizontalDistanceSqr() < 0.001,
                        "A captive owner does not cause idle follow AI or local client travel to move");
                var player = game.getPlayerList().getPlayers().getFirst();
                slime.releaseEntity(player.getUUID());
                check(slime.swallowOwner(player), "Restore driver before upgrade");
                slime.setTierLevel(5);
            });
            context.waitFor(mc -> mc.player.getVehicle() instanceof TamedSlimeEntity slime && slime.isCaptive(mc.player)
                    && SlimeCaptivityClientState.graceTicks() > 0);
            check(context.computeOnClient(mc -> SlimeCaptivityClientState.blur()) == 0, "Grace period has no digestion blur");
            float originalEyeHeight = context.computeOnClient(mc -> mc.player.getEyeHeight());
            context.getInput().lookAt(0, 0);
            var floatingStart = server.computeOnServer(game -> {
                var slime = (TamedSlimeEntity) game.overworld().getEntity(slimeId);
                return slime.captivity().offset(game.getPlayerList().getPlayers().getFirst());
            });
            var floatingSlimeStart = server.computeOnServer(game -> game.overworld().getEntity(slimeId).position());
            context.getInput().holdKeyFor(options -> options.keyUp, 20);
            context.getInput().holdKeyFor(options -> options.keyJump, 15);
            context.waitTicks(5);
            var floatingMoved = server.computeOnServer(game -> {
                var slime = (TamedSlimeEntity) game.overworld().getEntity(slimeId);
                check(slime.position().subtract(floatingSlimeStart).horizontalDistanceSqr() < 0.001,
                        "Floating input never drives the uncontrolled slime");
                return slime.captivity().offset(game.getPlayerList().getPlayers().getFirst());
            });
            check(floatingMoved.z > floatingStart.z + 0.5 && floatingMoved.y > floatingStart.y + 0.4,
                    "Real WASD and jump packets move the captive inside the body");
            context.runOnClient(mc -> {
                var slime = (TamedSlimeEntity) mc.player.getVehicle();
                check(org.cneko.justarod.client.slime.SlimeFloatingClientState.offset(slime, mc.player, 1)
                        .distanceToSqr(floatingMoved) < 0.01, "Floating positions reach the connected client");
                var state = (net.minecraft.client.renderer.entity.state.AvatarRenderState)
                        mc.getEntityRenderDispatcher().extractEntity(mc.player, 1);
                check(!state.isPassenger && !state.isCrouching, "Floating render state has no seated or crouching pose");
                var renderer = (net.minecraft.client.renderer.entity.player.AvatarRenderer<?>)
                        (Object) mc.getEntityRenderDispatcher().getRenderer(state);
                renderer.getModel().setupAnim(state);
                check(Math.abs(renderer.getModel().rightLeg.xRot) < 0.2 && Math.abs(renderer.getModel().leftLeg.xRot) < 0.2,
                        "Actual player model has relaxed floating legs instead of the seated bend");
                mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
                mc.options.hideGui = true;
            });
            context.waitTicks(5);
            context.takeScreenshot("slime-captivity-floating");
            context.runOnClient(mc -> {
                mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
                mc.options.hideGui = false;
            });
            context.getInput().holdKeyFor(options -> options.keyShift, 10);
            context.waitTicks(5);
            context.runOnClient(mc -> check(mc.player.getVehicle() != null, "Level 5 captive cannot sneak out"));
            server.runOnServer(game -> {
                var slime = (TamedSlimeEntity) game.overworld().getEntity(slimeId);
                var player = game.getPlayerList().getPlayers().getFirst();
                check(slime.captivity().offset(player).y < floatingMoved.y - 0.2, "Sneak input moves the captive downward");
            });
            context.takeScreenshot("slime-captivity-grace");
            int savedMenuBlur = context.computeOnClient(mc -> mc.options.menuBackgroundBlurriness().get());
            server.runOnServer(game -> {
                var slime = (TamedSlimeEntity) game.overworld().getEntity(slimeId);
                var player = game.getPlayerList().getPlayers().getFirst();
                var out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, game.registryAccess());
                out.list("Players", UUIDUtil.CODEC).add(player.getUUID());
                out.list("Ages", com.mojang.serialization.Codec.INT).add(JRConfig.getSlimePlayerGraceTicks()
                        + JRConfig.getSlimePlayerBlurRampTicks() / 2);
                out.list("Offsets", net.minecraft.world.phys.Vec3.CODEC).add(slime.captivity().offset(player));
                out.list("DigestionVitals", com.mojang.serialization.Codec.FLOAT).add(player.getHealth() + player.getAbsorptionAmount());
                slime.captivity().load(TagValueInput.create(ProblemReporter.DISCARDING, game.registryAccess(), out.buildResult()));
            });
            context.waitFor(mc -> SlimeCaptivityClientState.blur() >= 0.5f);
            context.waitFor(mc -> mc.player.getScale() < 0.85);
            context.waitTicks(5);
            context.runOnClient(mc -> {
                check(mc.gameRenderer.getGameRenderState().optionsRenderState.menuBackgroundBlurriness >= 5,
                        "Digestion feeds a growing radius to the real blur shader");
                check(mc.options.menuBackgroundBlurriness().get() == savedMenuBlur, "Digestion does not alter saved user options");
                check(mc.player.getEyeHeight() < originalEyeHeight * 0.85, "Synced digestion scale lowers the real first-person camera");
            });
            context.takeScreenshot("slime-captivity-digestion-blur");
            context.runOnClient(mc -> {
                mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
                mc.options.hideGui = true;
            });
            context.waitTicks(5);
            context.takeScreenshot("slime-captivity-shrinking");
            context.runOnClient(mc -> {
                mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
                mc.options.hideGui = false;
            });
            for (int i = 0; i < 90; i++) {
                context.getInput().pressKey(org.cneko.justarod.client.JRKeyBindings.SLIME_STRUGGLE_KEY);
                context.waitTick();
            }
            context.waitFor(mc -> mc.player.getVehicle() == null && SlimeCaptivityClientState.blur() == 0);
            context.waitFor(mc -> mc.player.getScale() == 1);
            context.waitTicks(20);
            server.runOnServer(game -> {
                var slime = (TamedSlimeEntity) game.overworld().getEntity(slimeId);
                var player = game.getPlayerList().getPlayers().getFirst();
                check(player.getVehicle() == null && !slime.canHunt(player), "Actual R escape is protected against immediate recapture");
                slime.discard();
            });
            context.takeScreenshot("slime-captivity-escaped");
            // Inspect shrinkage in the actual contents layer: fitting must not cancel the native scale modifier.
            int[] preyIds = server.computeOnServer(game -> {
                var level = game.overworld();
                var player = game.getPlayerList().getPlayers().getFirst();
                var slime = JREntities.TAMED_SLIME.create(level, EntitySpawnReason.COMMAND);
                slime.setTierLevel(4);
                slime.setOwner(player);
                slime.setCommand(TamedSlimeEntity.COMMAND_STAY);
                slime.setNoAi(true);
                slime.setDirectDigestion(false);
                slime.setPos(0, 4, 0);
                level.addFreshEntity(slime);
                var cow = net.minecraft.world.entity.EntityType.COW.create(level, EntitySpawnReason.COMMAND);
                cow.setNoAi(true);
                cow.setPos(0, 4, 0);
                level.addFreshEntity(cow);
                check(slime.trySwallow(cow), "Cow enters the client visual test");
                player.teleportTo(0, 4, -6);
                return new int[]{slime.getId(), cow.getId()};
            });
            context.waitFor(mc -> mc.level.getEntity(preyIds[1]) != null && mc.level.getEntity(preyIds[1]).isPassenger());
            context.getInput().lookAt(0, 0);
            context.waitTicks(5);
            context.takeScreenshot("slime-prey-before-digestion");
            server.runOnServer(game -> ((TamedSlimeEntity) game.overworld().getEntity(preyIds[0])).setDirectDigestion(true));
            context.waitTicks(30);
            context.runOnClient(mc -> {
                var cow = (net.minecraft.world.entity.LivingEntity) mc.level.getEntity(preyIds[1]);
                check(cow != null && cow.getScale() < 0.8 && cow.getScale() > 0.3, "Live creature scale shrinks on the real client");
            });
            context.takeScreenshot("slime-prey-shrinking");
            context.waitFor(mc -> ((TamedSlimeEntity) mc.level.getEntity(preyIds[0])).getDigestedExperience() > 0);
            server.runOnServer(game -> {
                var slime = (TamedSlimeEntity) game.overworld().getEntity(preyIds[0]);
                check(slime.storage().items().stream().anyMatch(stack -> stack.is(net.minecraft.world.item.Items.BEEF)),
                        "Digestion loot is collected in a connected client world");
                slime.discard();
            });
        }
    }
}
