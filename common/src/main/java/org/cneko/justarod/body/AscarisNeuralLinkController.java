package org.cneko.justarod.body;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import org.cneko.justarod.ascaris.AscarisEntity;
import org.cneko.justarod.packet.AscarisActionPayload;
import org.cneko.justarod.packet.AscarisVisualEventPayload;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 「借她的眼睛看外界」—— 神经视窗 + 体内遥控总控制器。
 * <p>原理：在体内围咽神经环处潜行空手右键触发，保存玩家体内坐标与原游戏模式，
 * 临时以旁观相机附身到外界粉白蛔虫娘的视野中（异步等待单人世界实体区块重载完成再精准附着），保持 Visit 不断开；
 * 玩家可通过鼠标看四周，W/A/S/D 遥控她挪步、空格轻跳、R 坐下/起身、G 钻 1 格缝爬行；
 * 按 X / Shift 脱离相机、宿主体外死亡或玩家断线时均无缝还原原游戏模式并安全返回围咽神经环。</p>
 */
public final class AscarisNeuralLinkController {

    private static final Map<UUID, NeuralSession> SESSIONS = new ConcurrentHashMap<>();

    private AscarisNeuralLinkController() {
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(AscarisNeuralLinkController::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                onPlayerDisconnect(handler.player));
    }

    public static boolean isLinked(UUID playerUuid) {
        return SESSIONS.containsKey(playerUuid);
    }

    public static boolean isAttached(UUID playerUuid) {
        NeuralSession s = SESSIONS.get(playerUuid);
        return s != null && s.attached;
    }

    public static boolean isEntityLinked(AscarisEntity ascaris) {
        if (ascaris == null) return false;
        UUID entityId = ascaris.getUUID();
        UUID ownerId = ascaris.getBodyRegionOwnerUuid();
        for (NeuralSession s : SESSIONS.values()) {
            if (s.hostEntityUuid.equals(entityId) || s.hostOwnerUuid.equals(ownerId)) {
                return true;
            }
        }
        return false;
    }

    /** 从粉白蛔虫娘体内围咽神经环开启跨维度神经视窗 */
    public static boolean startLink(ServerPlayer player) {
        if (player == null || !player.isAlive() || !player.level().dimension().equals(BodyWorld.DIMENSION)) {
            return false;
        }
        if (SESSIONS.containsKey(player.getUUID())) {
            return false;
        }
        MinecraftServer server = ((ServerLevel) player.level()).getServer();
        BodyRegionData data = BodyRegionData.get(server);
        BodyRegionData.Visit visit = data.visit(player.getUUID());
        if (visit == null || !visit.isAscaris()) {
            return false;
        }

        GameType previousMode = player.gameMode();
        NeuralSession session = new NeuralSession(
                visit.host(),
                visit.hostEntityUuid(),
                visit.slot(),
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYRot(),
                player.getXRot(),
                previousMode
        );
        // 先占位登记，防止 BodyWorld.tick 在跨维度瞬间清掉 Visit！
        SESSIONS.put(player.getUUID(), session);

        ServerLevel originLevel = resolveOriginLevel(server, visit);
        AscarisEntity ascaris = BodyWorld.findAscarisHost(server, visit);

        // 关闭体内体温晕影并切换为旁观模式准备附着相机
        ServerPlayNetworking.send(player, new AscarisVisualEventPayload(
                AscarisVisualEventPayload.EVENT_BODY_WARMTH, player.getId(), 0, 0));
        player.setGameMode(GameType.SPECTATOR);

        if (ascaris != null && ascaris.isAlive() && ascaris.level() instanceof ServerLevel hostLevel
                && !hostLevel.dimension().equals(BodyWorld.DIMENSION)) {
            attachToHost(player, session, ascaris, visit);
            return true;
        }

        // 单人模式下原世界实体区块是异步重载的：先传送至原位唤醒区块，tick() 在 1~3 tick 内实体加载完毕后自动附着！
        originLevel.getChunkSource().getChunk(
                ((int) Math.floor(visit.x())) >> 4,
                ((int) Math.floor(visit.z())) >> 4,
                true);
        player.teleportTo(originLevel, visit.x(), visit.y(), visit.z(),
                Set.of(), visit.yaw(), visit.pitch(), false);

        AscarisEntity immediate = findAscarisAfterChunkLoad(originLevel, visit);
        if (immediate != null && immediate.isAlive()) {
            attachToHost(player, session, immediate, visit);
            return true;
        }

        session.attached = false;
        session.pendingAttachTicks = 40;
        player.sendOverlayMessage(Component.literal("§d正在同步粉白蛔虫娘的神经视野……"));
        return true;
    }

    private static void attachToHost(ServerPlayer player, NeuralSession session, AscarisEntity ascaris, BodyRegionData.Visit visit) {
        session.attached = true;
        session.pendingAttachTicks = 0;
        session.linkTicks = 0;
        session.hostEntityUuid = ascaris.getUUID();
        // 先将体内期间最新的安抚/投喂状态同步到刚重载的本体上，避免单人世界旧区块数据覆盖体内安抚成果！
        ascaris.setObsession(visit.hostObsession());
        ascaris.setHunger(visit.hostHunger());
        ascaris.refreshNeuralControl(40);

        if (ascaris.level() instanceof ServerLevel hostLevel) {
            player.teleportTo(hostLevel, ascaris.getX(), ascaris.getY(), ascaris.getZ(),
                    Set.of(), ascaris.getYHeadRot(), ascaris.getXRot(), false);
            hostLevel.playSound(null, ascaris.blockPosition(),
                    SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.PLAYERS, 0.35f, 1.3f);
        }
        player.setCamera(ascaris);
        ServerPlayNetworking.send(player, new AscarisVisualEventPayload(
                AscarisVisualEventPayload.EVENT_NEURAL_LINK, ascaris.getId(), 1, packNeuralStatus(ascaris)));
        player.sendSystemMessage(Component.literal(
                "§d✦ 神经视窗已连接！你正透过粉白蛔虫娘的双眼观察外界~ §7（[鼠标] 看四周 · [W/A/S/D] 挪步 · [空格] 轻跳 · [R] 坐/起 · [G] 钻缝爬行 · [X/Shift] 返回体内）"));
    }

    /** 结束神经视窗：恢复游戏模式与视角，安全返回体内围咽神经环（或留在外界若宿主已消亡） */
    public static boolean endLink(ServerPlayer player, boolean returnInsideBody) {
        NeuralSession session = SESSIONS.remove(player.getUUID());
        if (session == null) return false;

        MinecraftServer server = ((ServerLevel) player.level()).getServer();
        player.setCamera(player);
        player.setGameMode(session.previousGameMode);
        ServerPlayNetworking.send(player, new AscarisVisualEventPayload(
                AscarisVisualEventPayload.EVENT_NEURAL_LINK, -1, 0, 0));

        if (returnInsideBody && player.isAlive()) {
            ServerLevel body = server.getLevel(BodyWorld.DIMENSION);
            if (body != null && BodyRegionData.get(server).visit(player.getUUID()) != null) {
                player.teleportTo(body, session.returnX, session.returnY, session.returnZ,
                        Set.of(), session.returnYaw, session.returnPitch, false);
                body.playSound(null, player.blockPosition(),
                        SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.PLAYERS, 0.4f, 1.2f);
                player.sendOverlayMessage(Component.literal("§d神经视窗已断开，你的意识回到了围咽神经环旁~"));
                return true;
            }
        }
        return true;
    }

    /** 玩家死亡或断线时的兜底清理，确保绝不会残留在旁观模式 */
    public static void onPlayerDeath(ServerPlayer player) {
        NeuralSession session = SESSIONS.remove(player.getUUID());
        if (session != null) {
            player.setCamera(player);
            player.setGameMode(session.previousGameMode);
            ServerPlayNetworking.send(player, new AscarisVisualEventPayload(
                    AscarisVisualEventPayload.EVENT_NEURAL_LINK, -1, 0, 0));
        }
    }

    private static void onPlayerDisconnect(ServerPlayer player) {
        NeuralSession session = SESSIONS.remove(player.getUUID());
        if (session == null) return;
        MinecraftServer server = ((ServerLevel) player.level()).getServer();
        player.setCamera(player);
        player.setGameMode(session.previousGameMode);
        ServerLevel body = server.getLevel(BodyWorld.DIMENSION);
        if (body != null && BodyRegionData.get(server).visit(player.getUUID()) != null) {
            player.teleportTo(body, session.returnX, session.returnY, session.returnZ,
                    Set.of(), session.returnYaw, session.returnPitch, false);
        }
    }

    /** 处理客户端发来的体内遥控指令（转头 / 行走 / 轻跳 / 坐起 / 钻缝爬行 / 断开） */
    public static boolean handleRemoteAction(ServerPlayer player, int action, int packedParam) {
        NeuralSession session = SESSIONS.get(player.getUUID());
        if (session == null) return false;

        if (action == AscarisActionPayload.ACTION_NEURAL_EXIT) {
            return endLink(player, true);
        }
        if (!session.attached) return false;

        MinecraftServer server = ((ServerLevel) player.level()).getServer();
        BodyRegionData.Visit visit = BodyRegionData.get(server).visit(player.getUUID());
        AscarisEntity ascaris = visit != null ? BodyWorld.findAscarisHost(server, visit) : null;
        if (ascaris == null || !ascaris.isAlive()) {
            endLink(player, false);
            return false;
        }

        switch (action) {
            case AscarisActionPayload.ACTION_NEURAL_LOOK -> {
                short yawFixed = (short) (packedParam & 0xFFFF);
                short pitchFixed = (short) ((packedParam >>> 16) & 0xFFFF);
                float yaw = yawFixed / 10.0f;
                float pitch = Mth.clamp(pitchFixed / 10.0f, -65f, 65f);
                ascaris.handleNeuralLook(yaw, pitch);
            }
            case AscarisActionPayload.ACTION_NEURAL_MOVE -> {
                int moveBits = packedParam & 0xF;
                short yawFixed = (short) ((packedParam >>> 4) & 0xFFFF);
                float yaw = yawFixed / 10.0f;
                float forward = ((moveBits & 1) != 0 ? 1f : 0f) - ((moveBits & 2) != 0 ? 1f : 0f);
                float strafe = ((moveBits & 4) != 0 ? 1f : 0f) - ((moveBits & 8) != 0 ? 1f : 0f);
                ascaris.handleNeuralMove(yaw, forward, strafe);
            }
            case AscarisActionPayload.ACTION_NEURAL_HOP -> {
                if (session.hopCooldown <= 0) {
                    session.hopCooldown = 14;
                    ascaris.handleNeuralHop();
                }
            }
            case AscarisActionPayload.ACTION_NEURAL_SIT -> {
                boolean nowSitting = ascaris.handleNeuralToggleSit();
                player.sendOverlayMessage(Component.literal(nowSitting
                        ? "§d[神经遥控] 她乖乖在原地坐了下来~"
                        : "§d[神经遥控] 她起身准备继续溜达啦~"));
            }
            case AscarisActionPayload.ACTION_NEURAL_CRAWL -> {
                boolean nowCrawling = ascaris.handleNeuralToggleCrawl();
                player.sendOverlayMessage(Component.literal(nowCrawling
                        ? "§d[神经遥控] 压低身形进入 1 格钻缝藏身姿态~"
                        : "§d[神经遥控] 恢复正常站立姿态~"));
            }
            default -> {
                return false;
            }
        }
        ServerPlayNetworking.send(player, new AscarisVisualEventPayload(
                AscarisVisualEventPayload.EVENT_NEURAL_LINK, ascaris.getId(), 1, packNeuralStatus(ascaris)));
        return true;
    }

    private static void tick(MinecraftServer server) {
        if (SESSIONS.isEmpty()) return;
        BodyRegionData data = BodyRegionData.get(server);
        for (var entry : Map.copyOf(SESSIONS).entrySet()) {
            UUID playerUuid = entry.getKey();
            NeuralSession session = entry.getValue();
            ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
            if (player == null) {
                SESSIONS.remove(playerUuid);
                continue;
            }
            if (!player.isAlive()) {
                onPlayerDeath(player);
                continue;
            }
            if (session.hopCooldown > 0) session.hopCooldown--;

            BodyRegionData.Visit visit = data.visit(playerUuid);
            if (visit == null || !visit.isAscaris()) {
                endLink(player, false);
                continue;
            }

            // 阶段 1：等待单人世界实体区块异步加载完毕后附着相机
            if (!session.attached) {
                ServerLevel originLevel = resolveOriginLevel(server, visit);
                AscarisEntity found = BodyWorld.findAscarisHost(server, visit);
                if (found == null) {
                    found = findAscarisAfterChunkLoad(originLevel, visit);
                }
                if (found != null && found.isAlive()) {
                    attachToHost(player, session, found, visit);
                    continue;
                }
                if (--session.pendingAttachTicks <= 0) {
                    endLink(player, true);
                    player.sendOverlayMessage(Component.literal("§e神经视窗信号微弱，暂时感应不到外界的她……"));
                }
                continue;
            }

            // 阶段 2：已附着粉白蛔虫娘视野，持续监控存活与脱离信号
            session.linkTicks++;
            AscarisEntity ascaris = BodyWorld.findAscarisHost(server, visit);
            if (ascaris == null || !ascaris.isAlive()) {
                endLink(player, false);
                data.removeVisit(playerUuid);
                player.sendSystemMessage(Component.translatable("body.ascaris.host_gone"));
                continue;
            }

            ascaris.refreshNeuralControl(25);

            // 玩家按原生 Shift 脱离旁观相机 -> 无缝切回体内围咽神经环！
            if (session.linkTicks > 5 && player.getCamera() != ascaris) {
                endLink(player, true);
                continue;
            }

            if (player.level() != ascaris.level() || player.distanceToSqr(ascaris) > 16.0) {
                if (ascaris.level() instanceof ServerLevel hostLevel) {
                    player.teleportTo(hostLevel, ascaris.getX(), ascaris.getY(), ascaris.getZ(),
                            Set.of(), ascaris.getYHeadRot(), ascaris.getXRot(), false);
                    player.setCamera(ascaris);
                }
            }

            if (session.linkTicks % 10 == 0) {
                if (visit.hostObsession() != ascaris.getObsession() || visit.hostHunger() != ascaris.getHunger()) {
                    data.putVisit(playerUuid, visit.withHostStats(ascaris.getObsession(), ascaris.getHunger()));
                }
                ServerPlayNetworking.send(player, new AscarisVisualEventPayload(
                        AscarisVisualEventPayload.EVENT_NEURAL_LINK, ascaris.getId(), 1, packNeuralStatus(ascaris)));
            }
        }
    }

    /** 将神经视窗状态打包进单个 int：bit0=坐姿, bit1=爬行, bits2..8=饱食(0..100), bits9..15=血量%(0..100) */
    public static int packNeuralStatus(AscarisEntity ascaris) {
        int sitBit = ascaris.isOrderedToSit() ? 1 : 0;
        int crawlBit = ascaris.isCrawling() ? 2 : 0;
        int hunger = Mth.clamp(ascaris.getHunger(), 0, 100);
        int hpPct = Mth.clamp(Math.round((ascaris.getHealth() / Math.max(1f, ascaris.getMaxHealth())) * 100f), 0, 100);
        return sitBit | crawlBit | (hunger << 2) | (hpPct << 9);
    }

    private static ServerLevel resolveOriginLevel(MinecraftServer server, BodyRegionData.Visit visit) {
        try {
            Identifier id = Identifier.parse(visit.origin());
            ServerLevel orig = server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
            if (orig != null && !orig.dimension().equals(BodyWorld.DIMENSION)) {
                return orig;
            }
        } catch (Exception ignored) {
        }
        return server.overworld();
    }

    @Nullable
    private static AscarisEntity findAscarisAfterChunkLoad(ServerLevel level, BodyRegionData.Visit visit) {
        Entity exact = level.getEntity(visit.hostEntityUuid());
        if (exact instanceof AscarisEntity a && a.isAlive()) return a;
        if (!visit.host().equals(visit.hostEntityUuid())) {
            Entity byOwner = level.getEntity(visit.host());
            if (byOwner instanceof AscarisEntity a && a.isAlive()) return a;
        }
        AABB box = new AABB(visit.x() - 48, visit.y() - 32, visit.z() - 48,
                visit.x() + 48, visit.y() + 32, visit.z() + 48);
        List<AscarisEntity> nearby = level.getEntitiesOfClass(AscarisEntity.class, box,
                a -> a.isAlive() && a.getBodyRegionOwnerUuid().equals(visit.host()));
        return nearby.isEmpty() ? null : nearby.getFirst();
    }

    private static final class NeuralSession {
        final UUID hostOwnerUuid;
        UUID hostEntityUuid;
        final long bodySlot;
        final double returnX, returnY, returnZ;
        final float returnYaw, returnPitch;
        final GameType previousGameMode;
        boolean attached = false;
        int pendingAttachTicks = 0;
        int linkTicks = 0;
        int hopCooldown = 0;

        NeuralSession(UUID hostOwnerUuid, UUID hostEntityUuid, long bodySlot,
                      double returnX, double returnY, double returnZ,
                      float returnYaw, float returnPitch, GameType previousGameMode) {
            this.hostOwnerUuid = hostOwnerUuid;
            this.hostEntityUuid = hostEntityUuid;
            this.bodySlot = bodySlot;
            this.returnX = returnX;
            this.returnY = returnY;
            this.returnZ = returnZ;
            this.returnYaw = returnYaw;
            this.returnPitch = returnPitch;
            this.previousGameMode = previousGameMode;
        }
    }
}
