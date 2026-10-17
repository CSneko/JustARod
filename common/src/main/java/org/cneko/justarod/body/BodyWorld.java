package org.cneko.justarod.body;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.cneko.justarod.config.JRConfig;
import org.cneko.justarod.entity.Pregnant;
import org.cneko.justarod.item.JRItems;
import org.cneko.justarod.packet.BodyTravelPayload;

import java.util.Set;
import java.util.UUID;

/** Server-authoritative entry, return, reset and confinement for the body dimension. */
public final class BodyWorld {
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION,
            Identifier.fromNamespaceAndPath("justarod", "inside_body"));
    public static final int MOUTH = 0, VAGINA = 1, ANUS = 2, ANYWHERE = 3;
    private static final int FLOOR_Y = 80;

    private BodyWorld() { }

    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(BodyTravelPayload.ID, (payload, context) ->
                context.server().execute(() -> {
                    if (payload.route() == BodyTravelPayload.EXIT) {
                        exit(context.player());
                    } else {
                        ServerPlayer host = context.server().getPlayerList().getPlayer(payload.target());
                        enter(context.player(), host, payload.route());
                    }
                }));
        ServerTickEvents.END_SERVER_TICK.register(BodyWorld::tick);
    }

    /** Called at the end of ServerPlayer.die, which does not invoke LivingEntity.die in 26.1.2. */
    public static void onPlayerDeath(ServerPlayer player) {
        MinecraftServer server = ((ServerLevel) player.level()).getServer();
        BodyRegionData data = BodyRegionData.get(server);
        if (JRConfig.isInsideBodyEnabled() && data.currentRegion(player.getUUID()) != null) {
            reset(server, player.getUUID());
        }
        data.removeVisit(player.getUUID());
    }

    public static boolean enter(ServerPlayer visitor, ServerPlayer host, int route) {
        if (!JRConfig.isInsideBodyEnabled()) return fail(visitor, "body.disabled");
        if (host == null || host == visitor || !host.isAlive() || !visitor.isAlive()
                || visitor.level() != host.level() || visitor.level().dimension().equals(DIMENSION)
                || visitor.distanceToSqr(host) > 25 || visitor.isPassenger()) {
            return fail(visitor, "body.unavailable");
        }
        if (route < MOUTH || route > ANYWHERE) return false;
        if (route == VAGINA && !((Pregnant) (Object) host).isFemale()) return fail(visitor, "body.unavailable");
        if (route == ANYWHERE && !visitor.getMainHandItem().is(JRItems.Companion.getBODY_INFILTRATOR())
                && !visitor.getOffhandItem().is(JRItems.Companion.getBODY_INFILTRATOR())) {
            return fail(visitor, "body.item_required");
        }
        MinecraftServer server = ((ServerLevel) visitor.level()).getServer();
        ServerLevel body = server.getLevel(DIMENSION);
        if (body == null) return fail(visitor, "body.unavailable");
        BodyRegionData data = BodyRegionData.get(server);
        if (data.visit(visitor.getUUID()) != null) return fail(visitor, "body.unavailable");
        long slot;
        try {
            slot = data.regionFor(host.getUUID());
        } catch (IllegalStateException e) {
            return fail(visitor, "body.full");
        }
        int cx = BodyRegionData.centerX(slot);
        int cz = BodyRegionData.centerZ(slot);
        if (!data.isInitialized(slot) || !body.getBlockState(new BlockPos(cx, FLOOR_Y - 1, cz)).is(Blocks.BEDROCK)) {
            makeEntryRoom(body, slot);
            data.markInitialized(slot);
        }
        BodyRegionData.Visit visit = new BodyRegionData.Visit(host.getUUID(), slot,
                visitor.level().dimension().identifier().toString(), visitor.getX(), visitor.getY(),
                visitor.getZ(), visitor.getYRot(), visitor.getXRot());
        data.putVisit(visitor.getUUID(), visit);
        // Entry is rare; make the reservation durable before the player is moved.
        server.overworld().getDataStorage().saveAndJoin();
        int dx = switch (route) { case MOUTH -> -18; case ANUS -> 18; default -> 0; };
        int dz = route == ANYWHERE ? 18 : 0;
        boolean moved = visitor.teleportTo(body, cx + dx + 0.5, FLOOR_Y + 1, cz + dz + 0.5,
                Set.of(), visitor.getYRot(), visitor.getXRot(), false);
        if (!moved) {
            data.removeVisit(visitor.getUUID());
            server.overworld().getDataStorage().saveAndJoin();
            return fail(visitor, "body.unavailable");
        }
        visitor.sendSystemMessage(Component.translatable("body.entered", host.getName()));
        return true;
    }

    public static boolean exit(ServerPlayer visitor) {
        if (!visitor.isAlive()) return false;
        MinecraftServer server = ((ServerLevel) visitor.level()).getServer();
        BodyRegionData data = BodyRegionData.get(server);
        BodyRegionData.Visit visit = data.visit(visitor.getUUID());
        if (visit == null && !visitor.level().dimension().equals(DIMENSION)) return false;
        ServerLevel destination = null;
        double x = 0, y = 0, z = 0;
        float yaw = visitor.getYRot(), pitch = visitor.getXRot();
        if (visit != null) {
            try {
                Identifier id = Identifier.parse(visit.origin());
                destination = server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
                x = visit.x(); y = visit.y(); z = visit.z();
                yaw = visit.yaw(); pitch = visit.pitch();
            } catch (Exception ignored) { }
        }
        if (destination == null || destination.dimension().equals(DIMENSION) || !safe(destination, x, y, z)) {
            destination = server.overworld();
            BlockPos spawn = destination.getRespawnData().pos();
            x = spawn.getX() + 0.5; y = spawn.getY() + 1; z = spawn.getZ() + 0.5;
        }
        boolean moved = visitor.teleportTo(destination, x, y, z, Set.of(), yaw, pitch, false);
        if (moved) {
            data.removeVisit(visitor.getUUID());
            server.overworld().getDataStorage().saveAndJoin();
            visitor.sendSystemMessage(Component.translatable("body.exited"));
        }
        return moved;
    }

    public static boolean reset(MinecraftServer server, UUID host) {
        BodyRegionData data = BodyRegionData.get(server);
        if (data.currentRegion(host) == null) return false;
        try {
            data.reset(host);
        } catch (IllegalStateException e) {
            return false;
        }
        server.overworld().getDataStorage().saveAndJoin();
        for (var entry : data.visits().entrySet()) {
            if (!entry.getValue().host().equals(host)) continue;
            ServerPlayer visitor = server.getPlayerList().getPlayer(entry.getKey());
            if (visitor != null) exit(visitor);
        }
        return true;
    }

    private static void tick(MinecraftServer server) {
        BodyRegionData data = BodyRegionData.get(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive()) continue;
            BodyRegionData.Visit visit = data.visit(player.getUUID());
            if (player.level().dimension().equals(DIMENSION)) {
                if (!JRConfig.isInsideBodyEnabled() || visit == null
                        || !Long.valueOf(visit.slot()).equals(data.currentRegion(visit.host()))
                        || server.getPlayerList().getPlayer(visit.host()) == null) {
                    exit(player);
                    continue;
                }
                if (!BodyRegionData.contains(visit.slot(), player.getX(), player.getZ())) {
                    player.teleportTo(BodyRegionData.centerX(visit.slot()) + 0.5,
                            FLOOR_Y + 1, BodyRegionData.centerZ(visit.slot()) + 0.5);
                }
            } else if (visit != null) {
                data.removeVisit(player.getUUID());
            }
        }
    }

    private static boolean safe(ServerLevel level, double x, double y, double z) {
        BlockPos feet = BlockPos.containing(x, y, z);
        return level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir()
                && level.getBlockState(feet.below()).isCollisionShapeFullBlock(level, feet.below());
    }

    private static void makeEntryRoom(ServerLevel level, long slot) {
        int cx = BodyRegionData.centerX(slot);
        int cz = BodyRegionData.centerZ(slot);
        for (int x = -32; x <= 32; x++) {
            for (int z = -24; z <= 24; z++) {
                level.setBlock(new BlockPos(cx + x, FLOOR_Y, cz + z), Blocks.CRIMSON_NYLIUM.defaultBlockState(), 3);
                if (Math.abs(x) == 32 || Math.abs(z) == 24) {
                    for (int y = FLOOR_Y + 1; y <= FLOOR_Y + 5; y++) {
                        level.setBlock(new BlockPos(cx + x, y, cz + z), Blocks.NETHER_WART_BLOCK.defaultBlockState(), 3);
                    }
                }
            }
        }
        for (int[] marker : new int[][] {{-18, 0}, {0, 0}, {18, 0}, {0, 18}}) {
            level.setBlock(new BlockPos(cx + marker[0], FLOOR_Y, cz + marker[1]),
                    Blocks.SHROOMLIGHT.defaultBlockState(), 3);
        }
        level.setBlock(new BlockPos(cx, FLOOR_Y - 1, cz), Blocks.BEDROCK.defaultBlockState(), 3);
    }

    private static boolean fail(ServerPlayer player, String key) {
        player.sendSystemMessage(Component.translatable(key));
        return false;
    }
}
