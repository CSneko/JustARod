package org.cneko.justarod.body;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import org.cneko.justarod.config.JRConfig;
import org.cneko.justarod.item.JRItems;
import org.cneko.justarod.packet.BodyTravelPayload;

import java.util.UUID;

/** Connected-client coverage for the custom dimension and the server entry gate. */
public class BodyClientGameTests implements FabricClientGameTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            var server = world.getServer();
            UUID hostId = server.computeOnServer(game -> {
                check(game.getLevel(BodyWorld.DIMENSION) != null, "Normal worlds must include the body dimension");
                var level = game.overworld();
                var visitor = game.getPlayerList().getPlayers().getFirst();
                for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
                    level.setBlockAndUpdate(new BlockPos(x, 79, z), Blocks.STONE.defaultBlockState());
                }
                visitor.teleportTo(0.5, 80, 0.5);
                var cookie = CommonListenerCookie.createInitial(
                        new GameProfile(UUID.randomUUID(), "body-test-host"), false);
                var host = new ServerPlayer(game, level, cookie.gameProfile(), cookie.clientInformation());
                var connection = new Connection(PacketFlow.SERVERBOUND);
                new EmbeddedChannel(connection);
                game.getPlayerList().placeNewPlayer(connection, host, cookie);
                host.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
                host.teleportTo(2.5, 80, 0.5);
                return host.getUUID();
            });
            context.runOnClient(mc -> ClientPlayNetworking.send(new BodyTravelPayload(hostId, BodyWorld.MOUTH)));
            context.waitFor(mc -> mc.player != null && mc.level != null
                    && mc.level.dimension().equals(BodyWorld.DIMENSION));
            server.runOnServer(game -> {
                var visitor = game.getPlayerList().getPlayers().getFirst();
                var data = BodyRegionData.get(game);
                long old = data.currentRegion(hostId);
                check(BodyRegionData.contains(old, visitor.getX(), visitor.getZ()),
                        "Visitor must land inside the host's unique region");
                check(BodyWorld.exit(visitor), "Visitor must be able to leave");
                check(BodyWorld.reset(game, hostId), "Host's region must be resettable");
                check(data.currentRegion(hostId) != old, "Reset must allocate a fresh region");
            });
            context.waitFor(mc -> mc.player != null && mc.level != null
                    && mc.level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD));
            server.runOnServer(game -> game.getPlayerList().getPlayers().getFirst().setItemInHand(
                    InteractionHand.MAIN_HAND, new ItemStack(JRItems.Companion.getBODY_INFILTRATOR())));
            context.runOnClient(mc -> ClientPlayNetworking.send(new BodyTravelPayload(hostId, BodyWorld.ANYWHERE)));
            context.waitFor(mc -> mc.player != null && mc.level != null
                    && mc.level.dimension().equals(BodyWorld.DIMENSION));
            server.runOnServer(game -> {
                var visitor = game.getPlayerList().getPlayers().getFirst();
                long fresh = BodyRegionData.get(game).currentRegion(hostId);
                check(BodyRegionData.contains(fresh, visitor.getX(), visitor.getZ()),
                        "Special-item entry must use the host's new region");
                check(BodyWorld.exit(visitor), "Special-item visitor must be able to leave");
            });
            context.waitFor(mc -> mc.player != null && mc.level != null
                    && mc.level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD));
            int cowId = server.computeOnServer(game -> {
                var body = game.getLevel(BodyWorld.DIMENSION);
                body.setChunkForced(0, 0, true);
                var cow = EntityType.COW.create(body, EntitySpawnReason.COMMAND);
                check(cow != null, "Tick probe can be created");
                cow.setNoGravity(true);
                cow.setPos(0.5, 120, 0.5);
                body.addFreshEntity(cow);
                return cow.getId();
            });
            context.waitTicks(10);
            int tickingBeforeDisable = server.computeOnServer(game -> {
                var cow = game.getLevel(BodyWorld.DIMENSION).getEntity(cowId);
                check(cow != null && cow.tickCount > 0, "Body dimension must tick while enabled");
                return cow.tickCount;
            });
            try {
                server.runOnServer(game -> {
                    JRConfig.CONFIG_BUILDER.setBoolean("inside_body.enabled", false);
                    check(game.getLevel(BodyWorld.DIMENSION) != null,
                            "Disabling gameplay must keep the dimension registered");
                    var visitor = game.getPlayerList().getPlayers().getFirst();
                    var host = game.getPlayerList().getPlayer(hostId);
                    check(!BodyWorld.enter(visitor, host, BodyWorld.ANUS),
                            "Disabled gameplay must reject direct entry");
                    check(visitor.level().dimension().equals(net.minecraft.world.level.Level.OVERWORLD),
                            "Rejected entry must leave the visitor outside");
                });
                context.waitTicks(12);
                server.runOnServer(game -> {
                    var cow = game.getLevel(BodyWorld.DIMENSION).getEntity(cowId);
                    check(cow != null && cow.tickCount == tickingBeforeDisable,
                            "Disabled body dimension must stop entity ticks");
                });
            } finally {
                server.runOnServer(game -> {
                    JRConfig.CONFIG_BUILDER.setBoolean("inside_body.enabled", true);
                    var body = game.getLevel(BodyWorld.DIMENSION);
                    var cow = body.getEntity(cowId);
                    if (cow != null) cow.discard();
                    body.setChunkForced(0, 0, false);
                });
            }
            server.runOnServer(game -> {
                var visitor = game.getPlayerList().getPlayers().getFirst();
                var host = game.getPlayerList().getPlayer(hostId);
                check(BodyWorld.enter(visitor, host, BodyWorld.ANUS), "Direct anal entry must work");
                long old = BodyRegionData.get(game).currentRegion(hostId);
                host.die(host.damageSources().genericKill());
                check(BodyRegionData.get(game).currentRegion(hostId) != old,
                        "Host death must allocate a new region");
            });
            context.waitFor(mc -> mc.player != null && mc.level != null
                    && mc.level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD));
        }
    }
}
