package org.cneko.justarod.body;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.cneko.justarod.ascaris.AscarisEntity;
import org.cneko.justarod.ascaris.AscarisHostController;
import org.cneko.justarod.ascaris.AscarisPoseState;
import org.cneko.justarod.block.ascaris.AscarisChymeBlock;
import org.cneko.justarod.config.JRConfig;
import org.cneko.justarod.effect.JREffects;
import org.cneko.justarod.entity.Pregnant;
import org.cneko.justarod.item.JRItems;
import org.cneko.justarod.packet.AscarisVisualEventPayload;
import org.cneko.justarod.packet.BodyTravelPayload;
import org.cneko.justarod.sound.JRSounds;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-authoritative entry, return, reset and confinement for the body dimension. */
public final class BodyWorld {
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION,
            Identifier.fromNamespaceAndPath("justarod", "inside_body"));
    public static final int MOUTH = 0, VAGINA = 1, ANUS = 2, ANYWHERE = 3;
    public static final int FLOOR_Y = 80;
    /** 病娇模式下阻拦自愿离开的执念阈值：需先安抚至低于此值，口腔/肛门出口才会重新松开。 */
    public static final int YANDERE_LOCK_OBSESSION = 50;

    private static final DustParticleOptions PINK_DUST = new DustParticleOptions(0xFF9AC2, 0.95f);
    private static final DustParticleOptions WHITE_DUST = new DustParticleOptions(0xFFF2F7, 0.85f);
    private static final DustParticleOptions YANDERE_DUST = new DustParticleOptions(0xC2184E, 1.0f);
    private static final DustParticleOptions WARM_PEACH = new DustParticleOptions(0xFFD8B0, 0.9f);
    private static final Map<UUID, PendingHostSync> PENDING_HOST_SYNCS = new ConcurrentHashMap<>();

    private static final class PendingHostSync {
        final BodyRegionData.Visit visit;
        final int obsession;
        final int hunger;
        int ttlTicks = 50;

        PendingHostSync(BodyRegionData.Visit visit, int obsession, int hunger) {
            this.visit = visit;
            this.obsession = obsession;
            this.hunger = hunger;
        }
    }

    private BodyWorld() { }

    public static void init() {
        AscarisNeuralLinkController.init();
        ServerPlayNetworking.registerGlobalReceiver(BodyTravelPayload.ID, (payload, context) ->
                context.server().execute(() -> {
                    if (payload.route() == BodyTravelPayload.EXIT) {
                        requestHotkeyExit(context.player());
                    } else {
                        ServerPlayer visitor = context.player();
                        ServerPlayer host = context.server().getPlayerList().getPlayer(payload.target());
                        if (host != null) {
                            enter(visitor, host, payload.route());
                        } else if (visitor.level() instanceof ServerLevel sl) {
                            Entity target = sl.getEntity(payload.target());
                            if (target instanceof AscarisEntity ascaris) {
                                enterAscaris(visitor, ascaris, payload.route(), false);
                            } else {
                                fail(visitor, "body.unavailable");
                            }
                        } else {
                            fail(visitor, "body.unavailable");
                        }
                    }
                }));
        ServerTickEvents.END_SERVER_TICK.register(BodyWorld::tick);
    }

    /** 按下 J 快捷键尝试离开体内：在粉白蛔虫娘体内的生存/冒险模式禁用快捷键，引导从口腔/肛门离开！ */
    public static boolean requestHotkeyExit(ServerPlayer visitor) {
        if (!visitor.isAlive()) return false;
        MinecraftServer server = ((ServerLevel) visitor.level()).getServer();
        BodyRegionData.Visit visit = BodyRegionData.get(server).visit(visitor.getUUID());
        if (visit != null && visit.isAscaris() && !visitor.isCreative() && !visitor.isSpectator()) {
            if (visitor.level() instanceof ServerLevel bodyLevel) {
                bodyLevel.playSound(null, visitor.blockPosition(),
                        SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.PLAYERS, 0.45f, 0.95f);
            }
            AscarisBodyInteractions.silenceZoneHudPublic(visitor, 65);
            visitor.sendOverlayMessage(Component.literal(
                    "§d生存模式下不能按 [J] 直接退出哦~ 请前往前端口腔三唇或尾端肛门空手右键滑出！"));
            return false;
        }
        return exit(visitor);
    }

    /** Called at the end of ServerPlayer.die, which does not invoke LivingEntity.die in 26.1.2. */
    public static void onPlayerDeath(ServerPlayer player) {
        AscarisNeuralLinkController.onPlayerDeath(player);
        MinecraftServer server = ((ServerLevel) player.level()).getServer();
        BodyRegionData data = BodyRegionData.get(server);
        if (JRConfig.isInsideBodyEnabled() && data.currentRegion(player.getUUID()) != null) {
            org.cneko.justarod.ascaris.BodyAscarisBridge.despawnForHost(server, player.getUUID());
            reset(server, player.getUUID());
        }
        data.removeVisit(player.getUUID());
        AscarisBodyInteractions.clearPlayerState(player.getUUID());
        ServerPlayNetworking.send(player, new AscarisVisualEventPayload(
                AscarisVisualEventPayload.EVENT_BODY_WARMTH, player.getId(), 0, 0));
        AscarisPhysiologyEngine.clearClientPhysiology(player);
    }

    /** Called when an Ascaris-chan dies: retires her body region and ejects all online visitors safely. */
    public static void onAscarisDeath(AscarisEntity ascaris) {
        if (ascaris == null || ascaris.getBodyHostUuid() != null || !(ascaris.level() instanceof ServerLevel level)) return;
        MinecraftServer server = level.getServer();
        UUID ownerUuid = ascaris.getBodyRegionOwnerUuid();
        BodyRegionData data = BodyRegionData.get(server);
        if (JRConfig.isInsideBodyEnabled() && data.currentRegion(ownerUuid) != null) {
            reset(server, ownerUuid);
        } else {
            exitVisitorsOfHost(server, ownerUuid, ascaris, "body.ascaris.host_gone");
        }
    }

    /** 是否有在线存活访客正待在该粉白蛔虫娘的体内世界（含神经视窗连接期间） */
    public static boolean hasVisitorsOfAscaris(AscarisEntity ascaris) {
        if (ascaris == null || !(ascaris.level() instanceof ServerLevel level)) return false;
        MinecraftServer server = level.getServer();
        UUID ownerUuid = ascaris.getBodyRegionOwnerUuid();
        BodyRegionData data = BodyRegionData.get(server);
        for (var entry : data.visits().entrySet()) {
            BodyRegionData.Visit visit = entry.getValue();
            if (!visit.isAscaris() || !visit.host().equals(ownerUuid)) continue;
            ServerPlayer visitor = server.getPlayerList().getPlayer(entry.getKey());
            if (visitor != null && visitor.isAlive()) {
                return true;
            }
        }
        return false;
    }

    /** Called when an Ascaris-chan is permanently discarded (not chunk-unloaded): ejects online visitors without wiping region. */
    public static void onAscarisRemoved(AscarisEntity ascaris) {
        if (ascaris == null || !(ascaris.level() instanceof ServerLevel level)) return;
        exitVisitorsOfHost(level.getServer(), ascaris.getBodyRegionOwnerUuid(), ascaris, "body.ascaris.host_gone");
    }

    /** Ejects visitors when Ascaris-chan enters a player host's body (prevents nested vanish). */
    public static void exitVisitorsOfAscaris(AscarisEntity ascaris, @Nullable String reasonKey) {
        if (ascaris == null || !(ascaris.level() instanceof ServerLevel level)) return;
        exitVisitorsOfHost(level.getServer(), ascaris.getBodyRegionOwnerUuid(), ascaris, reasonKey);
    }

    /** Someone outside fed Ascaris-chan a sweet treat (default flavor). */
    public static void onAscarisFedSweet(AscarisEntity ascaris) {
        onAscarisFedSweet(ascaris, ItemStack.EMPTY);
    }

    /** Someone outside fed Ascaris-chan a specific sweet treat -> replenish matching flavor chyme, gently self-heal walls & share sweetness! */
    public static void onAscarisFedSweet(AscarisEntity ascaris, ItemStack treat) {
        if (ascaris == null || !(ascaris.level() instanceof ServerLevel level)) return;
        MinecraftServer server = level.getServer();
        BodyRegionData data = BodyRegionData.get(server);
        UUID ownerUuid = ascaris.getBodyRegionOwnerUuid();
        Long slot = data.currentRegion(ownerUuid);
        ServerLevel bodyDim = server.getLevel(DIMENSION);
        int flavor = AscarisChymeBlock.flavorForItem(treat);
        int healedBlocks = 0;
        if (slot != null && data.isInitialized(slot) && bodyDim != null) {
            AscarisBodyInteractions.replenishSweetChyme(bodyDim, slot, flavor);
            AscarisPhysiologyEngine.onFoodSwallowed(bodyDim, slot, ascaris, flavor);
            healedBlocks = AscarisAnatomyBuilder.healDamagedAnatomy(bodyDim, slot, 24);
            AscarisPhysiologyEngine.syncBreachesAfterHeal(bodyDim, slot);
        }
        String flavorText = AscarisChymeBlock.flavorLabel(flavor);
        for (var entry : data.visits().entrySet()) {
            BodyRegionData.Visit visit = entry.getValue();
            if (!visit.isAscaris() || !visit.host().equals(ownerUuid)) continue;
            // 同步外侧喂食后的最新执念与饱食度
            data.putVisit(entry.getKey(), visit.withHostStats(ascaris.getObsession(), ascaris.getHunger()));
            ServerPlayer visitor = server.getPlayerList().getPlayer(entry.getKey());
            if (visitor == null || !visitor.isAlive() || !visitor.level().dimension().equals(DIMENSION)) continue;
            visitor.getFoodData().eat(4, 0.35f);
            visitor.heal(2.0f);
            ServerLevel bodyLevel = (ServerLevel) visitor.level();
            bodyLevel.sendParticles(ParticleTypes.HEART, visitor.getX(), visitor.getY() + 1.2, visitor.getZ(), 5, 0.35, 0.25, 0.35, 0.02);
            bodyLevel.playSound(null, visitor.blockPosition(), SoundEvents.HONEY_DRINK.value(), SoundSource.PLAYERS, 0.45f, 1.15f);
            String healHint = healedBlocks > 0 ? "（破损组织也随之悄悄愈合了一点~）" : "";
            visitor.sendOverlayMessage(Component.literal(
                    "§d外面给粉白蛔虫娘喂了甜食，肠道涌出了「" + flavorText + "」食糜~" + healHint + "♡"));
        }
    }

    /** Reverse deworming comedy: feeding pagoda candy / albendazole to Ascaris-chan expels visitors inside her! */
    public static int expelVisitorsFromAscaris(AscarisEntity ascaris, @Nullable net.minecraft.world.entity.player.Player medic, boolean harsh) {
        if (ascaris == null || !(ascaris.level() instanceof ServerLevel level)) return 0;
        MinecraftServer server = level.getServer();
        BodyRegionData data = BodyRegionData.get(server);
        UUID ownerUuid = ascaris.getBodyRegionOwnerUuid();
        Long drugSlot = data.currentRegion(ownerUuid);
        ServerLevel bodyDim = server.getLevel(DIMENSION);
        if (drugSlot != null && bodyDim != null) {
            AscarisPhysiologyEngine.administerInternalDrug(bodyDim, drugSlot, ascaris,
                    harsh ? AscarisPhysiologyState.DRUG_ALBENDAZOLE : AscarisPhysiologyState.DRUG_PIPERAZINE,
                    harsh ? 1200 : 900);
        }
        int expelled = 0;
        for (var entry : data.visits().entrySet()) {
            BodyRegionData.Visit visit = entry.getValue();
            if (!visit.isAscaris() || !visit.host().equals(ownerUuid)) continue;
            ServerPlayer visitor = server.getPlayerList().getPlayer(entry.getKey());
            if (visitor == null || !visitor.isAlive()) continue;
            if (forceExit(visitor, ascaris, null, true, harsh)) {
                expelled++;
                String drugName = harsh ? "阿苯达唑" : "宝塔糖";
                visitor.sendSystemMessage(Component.literal(
                        "§d§l有人给粉白蛔虫娘喂了" + drugName + "，你像一条小蛔虫一样被软绵绵地排了出来！"));
                if (medic != null && medic != visitor) {
                    medic.sendSystemMessage(Component.literal(
                            "§d你给粉白蛔虫娘喂下" + drugName + "后，居然把 §f" + visitor.getName().getString() + " §d从她肚子里排了出来！？"));
                }
            }
        }
        return expelled;
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
        org.cneko.justarod.ascaris.BodyAscarisBridge.onVisitorEntered(body, slot, host, visitor);
        return true;
    }

    /** Enter an Ascaris-chan's dedicated pink-white body dimension region (reverse parasitism!). */
    public static boolean enterAscaris(ServerPlayer visitor, AscarisEntity host, int route, boolean bypassRequirements) {
        if (!JRConfig.isInsideBodyEnabled()) return fail(visitor, "body.disabled");
        if (host == null || !host.isAlive() || !visitor.isAlive()
                || visitor.level() != host.level() || visitor.level().dimension().equals(DIMENSION)
                || (!bypassRequirements && visitor.distanceToSqr(host) > 25)
                || (visitor.isPassenger() && visitor.getVehicle() != host)) {
            return fail(visitor, "body.ascaris.unavailable");
        }
        if (route < MOUTH || route > ANYWHERE) return false;
        if (!bypassRequirements && route == ANYWHERE
                && !visitor.getMainHandItem().is(JRItems.Companion.getBODY_INFILTRATOR())
                && !visitor.getOffhandItem().is(JRItems.Companion.getBODY_INFILTRATOR())) {
            return fail(visitor, "body.item_required");
        }
        MinecraftServer server = ((ServerLevel) visitor.level()).getServer();
        ServerLevel body = server.getLevel(DIMENSION);
        if (body == null) return fail(visitor, "body.ascaris.unavailable");
        BodyRegionData data = BodyRegionData.get(server);
        if (data.visit(visitor.getUUID()) != null) return fail(visitor, "body.ascaris.unavailable");

        UUID regionOwner = host.getBodyRegionOwnerUuid();
        long slot;
        try {
            slot = data.regionFor(regionOwner);
        } catch (IllegalStateException e) {
            return fail(visitor, "body.full");
        }
        int cx = BodyRegionData.centerX(slot);
        int cz = BodyRegionData.centerZ(slot);
        if (!data.isInitialized(slot) || !body.getBlockState(new BlockPos(cx, FLOOR_Y - 1, cz)).is(Blocks.BEDROCK)) {
            makeAscarisEntryRoom(body, slot);
            data.markInitialized(slot);
        }

        if (visitor.getVehicle() == host) {
            visitor.stopRiding();
        }
        if (host.getPoseState().isWrapping()) {
            host.releaseTailWrap(false);
        }

        boolean ownedByVisitor = host.isTame() && host.isOwnedBy(visitor);
        boolean yandere = JRConfig.isAscarisYandereEnabled() && ownedByVisitor && host.getObsession() >= YANDERE_LOCK_OBSESSION;
        int projectedObsession = yandere ? Math.min(100, host.getObsession() + 10) : host.getObsession();
        boolean yandereClingEligible = ownedByVisitor && projectedObsession >= YANDERE_LOCK_OBSESSION;

        ServerLevel originLevel = (ServerLevel) visitor.level();
        BodyRegionData.Visit visit = new BodyRegionData.Visit(regionOwner, slot,
                originLevel.dimension().identifier().toString(), visitor.getX(), visitor.getY(),
                visitor.getZ(), visitor.getYRot(), visitor.getXRot(),
                BodyRegionData.HOST_ASCARIS, host.getUUID(), yandereClingEligible,
                projectedObsession, host.getHunger(), ownedByVisitor);
        data.putVisit(visitor.getUUID(), visit);
        server.overworld().getDataStorage().saveAndJoin();

        int dx = switch (route) {
            case MOUTH -> AscarisAnatomyBuilder.MOUTH_DX;
            case VAGINA -> AscarisAnatomyBuilder.VAGINA_DX;
            case ANUS -> AscarisAnatomyBuilder.ANUS_DX;
            default -> AscarisAnatomyBuilder.ANYWHERE_DX;
        };
        int dy = switch (route) {
            case MOUTH -> AscarisAnatomyBuilder.MOUTH_DY;
            case VAGINA -> AscarisAnatomyBuilder.VAGINA_DY;
            case ANUS -> AscarisAnatomyBuilder.ANUS_DY;
            default -> AscarisAnatomyBuilder.ANYWHERE_DY;
        };
        int dz = switch (route) {
            case MOUTH -> AscarisAnatomyBuilder.MOUTH_DZ;
            case VAGINA -> AscarisAnatomyBuilder.VAGINA_DZ;
            case ANUS -> AscarisAnatomyBuilder.ANUS_DZ;
            default -> AscarisAnatomyBuilder.ANYWHERE_DZ;
        };
        float entryYaw = (route == ANUS) ? 90f : -90f;
        boolean moved = visitor.teleportTo(body, cx + dx + 0.5, FLOOR_Y + dy, cz + dz + 0.5,
                Set.of(), entryYaw, 0f, false);
        if (!moved) {
            data.removeVisit(visitor.getUUID());
            server.overworld().getDataStorage().saveAndJoin();
            return fail(visitor, "body.ascaris.unavailable");
        }

        // Spawn welcoming living coelomocytes inside her pseudocoelom & sync initial internal physiology!
        AscarisBodyInteractions.ensureCoelomocytesPresent(body, slot);
        AscarisPhysiologyEngine.initFromExternalHost(server, slot, host);
        AscarisPhysiologyEngine.sendPhysiologySync(visitor, data.physiologyFor(slot));

        // Visual & sound feedback outside on Ascaris-chan and inside her anatomical chamber (no bell chimes inside!)
        originLevel.sendParticles(yandere ? YANDERE_DUST : PINK_DUST,
                host.getX(), host.getY() + 0.85, host.getZ(), 18, 0.35, 0.35, 0.35, 0.04);
        originLevel.sendParticles(WHITE_DUST,
                host.getX(), host.getY() + 0.85, host.getZ(), 12, 0.35, 0.35, 0.35, 0.03);
        originLevel.playSound(null, host.blockPosition(), JRSounds.ASCARIS_SLITHER, SoundSource.NEUTRAL, 0.9f, 1.05f);
        body.playSound(null, visitor.blockPosition(), JRSounds.ASCARIS_SLITHER, SoundSource.PLAYERS, 0.65f, 1.05f);
        body.sendParticles(PINK_DUST, visitor.getX(), visitor.getY() + 0.9, visitor.getZ(), 14, 0.4, 0.4, 0.4, 0.03);

        visitor.sendSystemMessage(Component.translatable("body.ascaris.entered", host.getDisplayName()));
        String anatomyHint = switch (route) {
            case MOUTH -> "§7[解剖位置：品字形三唇 · Y 形肌肉咽（前方围咽神经环可安抚 / 潜行右键开启神经视窗）]";
            case VAGINA -> "§7[解剖位置：前 1/3 腹侧阴门 · 阴道（后方分叉为左右双管子宫与盘绕卵巢）]";
            case ANUS -> "§7[解剖位置：尾端直肠 · 肛门（前方贯穿笔直肠道，四周为盘绕丝状卵巢）]";
            default -> "§7[解剖位置：中央假体腔（暖窝软垫可依偎 / 潜行小睡；离开请走前端口腔或尾端肛门）]";
        };
        visitor.sendSystemMessage(Component.literal(anatomyHint));

        if (yandere) {
            host.setObsession(projectedObsession);
            host.playTransientPose(AscarisPoseState.YANDERE_STARE, 90);
            host.playVoice(JRSounds.ASCARIS_VOICE_YANDERE_GIGGLE, 0.9f, 45, true);
            body.playSound(null, visitor.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.55f, 0.95f);
            visitor.sendSystemMessage(Component.literal(
                    "§c§o「原来主人自己躲进我身体里了呀♡ 太好了……不把我哄开心之前，我可不会放你出去哦♡」"));
        } else {
            if (!host.isMovementLocked()) {
                host.playTransientPose(AscarisPoseState.CURL_UP, 64);
            }
            host.playVoice(JRSounds.ASCARIS_VOICE_HOP, 0.88f, 36, true);
            if (ownedByVisitor) {
                visitor.sendSystemMessage(Component.literal(
                        "§d「呜咿？！不是我钻进去，是主人反过来钻到我肚子里来了吗……好痒呀 >~<」"));
            } else {
                visitor.sendSystemMessage(Component.literal(
                        "§d「咿呀？！怎、怎么有人钻进我肚子里了啦 >~<」"));
            }
        }

        // 立刻下发体内暖桃色体温呼吸微晕初始状态
        ServerPlayNetworking.send(visitor, new AscarisVisualEventPayload(
                AscarisVisualEventPayload.EVENT_BODY_WARMTH, visitor.getId(), yandere ? 3 : 1, projectedObsession));

        // Easter egg: if the visitor already carries a hosted Ascaris inside themselves!
        if (AscarisHostController.hasHostedAscaris(visitor)) {
            visitor.sendSystemMessage(Component.literal(
                    "§c§o「喂……你肚子里明明还住着我呢，居然跑去钻别的蛔虫身体里？！回来再跟你算账♡」"));
        }
        return true;
    }

    /** 有效执念值（兼容单人模式下主世界区块卸载时的 Visit 缓存） */
    public static int getEffectiveObsession(@Nullable AscarisEntity ascaris, BodyRegionData.Visit visit) {
        if (ascaris != null && ascaris.isAlive()) {
            return ascaris.getObsession();
        }
        return visit.hostObsession();
    }

    /** 有效饱食度 */
    public static int getEffectiveHunger(@Nullable AscarisEntity ascaris, BodyRegionData.Visit visit) {
        if (ascaris != null && ascaris.isAlive()) {
            return ascaris.getHunger();
        }
        return visit.hostHunger();
    }

    /** 访客是否为她的主人（兼容单人区块卸载） */
    public static boolean isOwnedByVisitor(@Nullable AscarisEntity ascaris, BodyRegionData.Visit visit, ServerPlayer visitor) {
        if (ascaris != null) {
            return ascaris.isTame() && ascaris.isOwnedBy(visitor);
        }
        return visit.hostOwnedByVisitor();
    }

    /**
     * 统一同步修改宿主执念/饱食/亲密：同时作用于已加载实体与持久化 Visit（保证单人模式下回主世界也能完整继承体内互动结果）。
     *
     * @return 更新后的执念值 (0..100)
     */
    public static int adjustHostStats(MinecraftServer server, ServerPlayer visitor, BodyRegionData.Visit visit,
                                      int deltaObsession, int deltaHunger, int deltaAffection) {
        AscarisEntity ascaris = findAscarisHost(server, visit);
        int curObs = getEffectiveObsession(ascaris, visit);
        int curHunger = getEffectiveHunger(ascaris, visit);
        int nextObs = Mth.clamp(curObs + deltaObsession, 0, 100);
        int nextHunger = Mth.clamp(curHunger + deltaHunger, 0, 100);

        if (ascaris != null && ascaris.isAlive()) {
            ascaris.setObsession(nextObs);
            ascaris.setHunger(nextHunger);
            if (deltaAffection != 0) {
                ascaris.setAffection(ascaris.getAffection() + deltaAffection);
            }
        }
        BodyRegionData data = BodyRegionData.get(server);
        BodyRegionData.Visit updated = visit.withHostStats(nextObs, nextHunger);
        data.putVisit(visitor.getUUID(), updated);
        server.overworld().getDataStorage().saveAndJoin();

        // 实时同步客户端体温/心跳晕影强度
        if (visitor.level().dimension().equals(DIMENSION) && !AscarisNeuralLinkController.isLinked(visitor.getUUID())) {
            int warmthMode = (JRConfig.isAscarisYandereEnabled() && updated.hostOwnedByVisitor() && nextObs >= YANDERE_LOCK_OBSESSION) ? 3 : 1;
            ServerPlayNetworking.send(visitor, new AscarisVisualEventPayload(
                    AscarisVisualEventPayload.EVENT_BODY_WARMTH, visitor.getId(), warmthMode, nextObs));
        }
        return nextObs;
    }

    public static boolean exit(ServerPlayer visitor) {
        if (!visitor.isAlive()) return false;
        MinecraftServer server = ((ServerLevel) visitor.level()).getServer();
        BodyRegionData data = BodyRegionData.get(server);
        BodyRegionData.Visit visit = data.visit(visitor.getUUID());
        if (visit == null && !visitor.level().dimension().equals(DIMENSION)) return false;

        AscarisEntity liveAscaris = (visit != null && visit.isAscaris()) ? findAscarisHost(server, visit) : null;

        // 病娇模式高执念锁定：生存/冒险模式下需先安抚使执念降至 50% 以下；
        // 例外（硬核药理解法）：若体腔处于枸橼酸哌嗪（宝塔糖）神经麻痹或严重失压导致括约肌弛缓松开，则可趁机滑出！
        if (visit != null && visit.isAscaris() && !visitor.isCreative() && !visitor.isSpectator()
                && JRConfig.isAscarisYandereEnabled()) {
            boolean owned = isOwnedByVisitor(liveAscaris, visit, visitor);
            int obsession = getEffectiveObsession(liveAscaris, visit);
            AscarisPhysiologyState phys = data.physiologyFor(visit.slot());
            if (owned && obsession >= YANDERE_LOCK_OBSESSION) {
                if (phys.isSphincterRelaxed()) {
                    adjustHostStats(server, visitor, visit, +12, 0, 0);
                    visitor.sendSystemMessage(Component.literal(
                            "§b[药理弛缓突破] §f出口括约肌在"
                                    + (phys.isParalyzedByPiperazine() ? "枸橼酸哌嗪麻痹" : "静水压严重失压")
                                    + "下失去了张力，你趁机强行滑出了体外！§c§o（她气鼓鼓地记住了这次逃跑……）"));
                } else {
                    ServerLevel bodyLevel = (ServerLevel) visitor.level();
                    // 出口括约肌紧闭，轻柔把玩家推回体内深处并唤醒体腔小细胞看守
                    int dx = (int) Math.floor(visitor.getX()) - BodyRegionData.centerX(visit.slot());
                    double pushX = (dx < 0) ? 0.36 : -0.36;
                    visitor.push(pushX, 0.08, 0.0);
                    visitor.hurtMarked = true;
                    AscarisBodyInteractions.alertCoelomocytesInSlot(bodyLevel, visit.slot(), 90);
                    bodyLevel.playSound(null, visitor.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.65f, 0.92f);
                    bodyLevel.playSound(null, visitor.blockPosition(), JRSounds.ASCARIS_VOICE_YANDERE_CLING, SoundSource.VOICE, 0.9f, 1.0f);
                    bodyLevel.sendParticles(YANDERE_DUST, visitor.getX(), visitor.getY() + 1.0, visitor.getZ(), 18, 0.45, 0.35, 0.45, 0.04);
                    ServerPlayNetworking.send(visitor, new AscarisVisualEventPayload(
                            AscarisVisualEventPayload.EVENT_BELLY_RUMBLE, visitor.getId(), 3, 0));
                    AscarisBodyInteractions.silenceZoneHudPublic(visitor, 80);
                    visitor.sendSystemMessage(Component.literal(
                            "§c§o「不行哦♡ 出口被我紧紧合上了……不把我哄乖之前，怎么能放你走呢？」§7（当前病娇执念 §c"
                                    + obsession + "%§7，需先安抚至 §a" + YANDERE_LOCK_OBSESSION
                                    + "% 以下§7：可右键轻抚围咽神经环、依偎/潜行小睡柔暖软垫或喂甜食；或在咽部用宝塔糖引发哌嗪括约肌麻痹！）"));
                    return false;
                }
            }
        }

        String msgKey = (visit != null && visit.isAscaris()) ? "body.ascaris.exited" : "body.exited";
        return forceExit(visitor, liveAscaris, msgKey, false, false);
    }

    private static boolean forceExit(ServerPlayer visitor, @Nullable AscarisEntity liveAscaris,
                                     @Nullable String messageKey, boolean expelledByDewormer, boolean harshDewormer) {
        if (!visitor.isAlive()) return false;
        MinecraftServer server = ((ServerLevel) visitor.level()).getServer();
        if (AscarisNeuralLinkController.isLinked(visitor.getUUID())) {
            AscarisNeuralLinkController.endLink(visitor, false);
        }
        BodyRegionData data = BodyRegionData.get(server);
        BodyRegionData.Visit visit = data.visit(visitor.getUUID());
        if (visit == null && !visitor.level().dimension().equals(DIMENSION)) return false;
        AscarisBodyInteractions.clearPlayerState(visitor.getUUID());

        ServerLevel destination = null;
        double x = 0, y = 0, z = 0;
        float yaw = visitor.getYRot(), pitch = visitor.getXRot();

        // Prefer returning right next to the live Ascaris-chan if she is currently loaded in a non-body dimension
        if (visit != null && visit.isAscaris()) {
            AscarisEntity host = liveAscaris != null ? liveAscaris : findAscarisHost(server, visit);
            if (host != null && host.level() instanceof ServerLevel hostLevel && !hostLevel.dimension().equals(DIMENSION)) {
                Vec3 fwd = Vec3.directionFromRotation(0, host.getYRot()).scale(expelledByDewormer ? 1.2 : 1.0);
                double candX = host.getX() + fwd.x;
                double candY = host.getY() + 0.05;
                double candZ = host.getZ() + fwd.z;
                if (safe(hostLevel, candX, candY, candZ)) {
                    destination = hostLevel;
                    x = candX; y = candY; z = candZ;
                } else if (safe(hostLevel, host.getX(), host.getY(), host.getZ())) {
                    destination = hostLevel;
                    x = host.getX(); y = host.getY(); z = host.getZ();
                }
            }
        }

        if (destination == null && visit != null) {
            try {
                Identifier id = Identifier.parse(visit.origin());
                ServerLevel orig = server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
                if (orig != null && !orig.dimension().equals(DIMENSION) && safe(orig, visit.x(), visit.y(), visit.z())) {
                    destination = orig;
                    x = visit.x(); y = visit.y(); z = visit.z();
                    yaw = visit.yaw(); pitch = visit.pitch();
                }
            } catch (Exception ignored) { }
        }
        if (destination == null) {
            destination = server.overworld();
            BlockPos spawn = destination.getRespawnData().pos();
            x = spawn.getX() + 0.5; y = spawn.getY() + 1; z = spawn.getZ() + 0.5;
        }

        boolean moved = visitor.teleportTo(destination, x, y, z, Set.of(), yaw, pitch, false);
        if (moved) {
            ServerPlayNetworking.send(visitor, new AscarisVisualEventPayload(
                    AscarisVisualEventPayload.EVENT_BODY_WARMTH, visitor.getId(), 0, 0));
            AscarisPhysiologyEngine.clearClientPhysiology(visitor);

            if (visit != null) {
                ServerLevel body = server.getLevel(DIMENSION);
                if (body != null && !visit.isAscaris()) {
                    org.cneko.justarod.ascaris.BodyAscarisBridge.onVisitorExited(body, visit.slot(), visitor, destination);
                }
            }
            data.removeVisit(visitor.getUUID());
            server.overworld().getDataStorage().saveAndJoin();

            if (visit != null && visit.isAscaris()) {
                // 单人模式下传送回主世界后实体区块可能延迟 1~2 tick 重载，立即尝试同步并在未加载时挂起延迟同步！
                AscarisEntity syncedHost = liveAscaris != null ? liveAscaris : findAscarisHost(server, visit);
                if (syncedHost != null && syncedHost.isAlive()) {
                    syncedHost.setObsession(visit.hostObsession());
                    syncedHost.setHunger(visit.hostHunger());
                } else {
                    PENDING_HOST_SYNCS.put(visit.host(), new PendingHostSync(visit, visit.hostObsession(), visit.hostHunger()));
                }

                destination.playSound(null, visitor.blockPosition(), JRSounds.ASCARIS_SLITHER, SoundSource.PLAYERS, 0.9f, 1.05f);
                destination.sendParticles(PINK_DUST, visitor.getX(), visitor.getY() + 0.7, visitor.getZ(),
                        expelledByDewormer ? 26 : 14, 0.45, 0.4, 0.45, 0.06);
                destination.sendParticles(WHITE_DUST, visitor.getX(), visitor.getY() + 0.7, visitor.getZ(),
                        expelledByDewormer ? 18 : 8, 0.4, 0.35, 0.4, 0.05);
                if (expelledByDewormer) {
                    destination.sendParticles(ParticleTypes.CHERRY_LEAVES, visitor.getX(), visitor.getY() + 0.9, visitor.getZ(),
                            12, 0.5, 0.4, 0.5, 0.04);
                    visitor.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, harshDewormer ? 80 : 50, harshDewormer ? 1 : 0, false, false));
                    ServerPlayNetworking.send(visitor, new AscarisVisualEventPayload(
                            AscarisVisualEventPayload.EVENT_EXPULSION_FINISH, visitor.getId(), harshDewormer ? 2 : 1, 0));
                } else {
                    // 正常离开她的温暖身体 -> 赋予 3 分钟「暖腹余温」增益！
                    visitor.addEffect(new MobEffectInstance(
                            JREffects.Companion.getWARM_BELLY_EFFECT(), 3600, 0, false, true, true));
                    destination.sendParticles(WARM_PEACH, visitor.getX(), visitor.getY() + 0.9, visitor.getZ(),
                            12, 0.4, 0.35, 0.4, 0.02);
                    visitor.sendOverlayMessage(Component.literal("§d带着她暖融融的体温回到了外界，获得了「暖腹余温」~♡"));
                }
            }

            if (messageKey != null) {
                visitor.sendSystemMessage(Component.translatable(messageKey));
            }
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
            if (visitor != null) {
                String msg = entry.getValue().isAscaris() ? "body.ascaris.exited" : "body.exited";
                forceExit(visitor, null, msg, false, false);
            }
        }
        return true;
    }

    private static void exitVisitorsOfHost(MinecraftServer server, UUID hostOwnerUuid,
                                           @Nullable AscarisEntity liveAscaris, @Nullable String reasonKey) {
        BodyRegionData data = BodyRegionData.get(server);
        for (var entry : data.visits().entrySet()) {
            if (!entry.getValue().host().equals(hostOwnerUuid)) continue;
            ServerPlayer visitor = server.getPlayerList().getPlayer(entry.getKey());
            if (visitor != null) {
                forceExit(visitor, liveAscaris, reasonKey, false, false);
            }
        }
    }

    private static void tick(MinecraftServer server) {
        if (!PENDING_HOST_SYNCS.isEmpty()) {
            for (var entry : Map.copyOf(PENDING_HOST_SYNCS).entrySet()) {
                PendingHostSync sync = entry.getValue();
                AscarisEntity reloaded = findAscarisHost(server, sync.visit);
                if (reloaded != null && reloaded.isAlive()) {
                    reloaded.setObsession(sync.obsession);
                    reloaded.setHunger(sync.hunger);
                    PENDING_HOST_SYNCS.remove(entry.getKey());
                } else if (--sync.ttlTicks <= 0) {
                    PENDING_HOST_SYNCS.remove(entry.getKey());
                }
            }
        }
        BodyRegionData data = BodyRegionData.get(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive()) continue;
            BodyRegionData.Visit visit = data.visit(player.getUUID());
            if (player.level().dimension().equals(DIMENSION)) {
                if (!JRConfig.isInsideBodyEnabled() || visit == null
                        || !Long.valueOf(visit.slot()).equals(data.currentRegion(visit.host()))) {
                    forceExit(player, null, (visit != null && visit.isAscaris()) ? "body.ascaris.exited" : "body.exited", false, false);
                    continue;
                }
                if (visit.isAscaris()) {
                    AscarisEntity ascaris = findAscarisHost(server, visit);
                    if (ascaris != null && !ascaris.isAlive()) {
                        forceExit(player, ascaris, "body.ascaris.host_gone", false, false);
                        continue;
                    }
                    tickAscarisInsideAtmosphere(player, ascaris, visit);
                } else {
                    ServerPlayer hostPlayer = server.getPlayerList().getPlayer(visit.host());
                    if (hostPlayer == null) {
                        forceExit(player, null, "body.exited", false, false);
                        continue;
                    }
                    AscarisPhysiologyEngine.tickPlayerHostPhysiology(player, hostPlayer, visit);
                }
                if (!BodyRegionData.contains(visit.slot(), player.getX(), player.getZ())) {
                    player.teleportTo(BodyRegionData.centerX(visit.slot()) + 0.5,
                            FLOOR_Y + 1, BodyRegionData.centerZ(visit.slot()) + 0.5);
                }
            } else if (visit != null && !AscarisNeuralLinkController.isLinked(player.getUUID())) {
                data.removeVisit(player.getUUID());
                AscarisBodyInteractions.clearPlayerState(player.getUUID());
                ServerPlayNetworking.send(player, new AscarisVisualEventPayload(
                        AscarisVisualEventPayload.EVENT_BODY_WARMTH, player.getId(), 0, 0));
                AscarisPhysiologyEngine.clearClientPhysiology(player);
            }
        }
    }

    /** Ambient warmth, live organ interactions & reverse-parasitism nutrition sharing while staying inside Ascaris-chan's body. */
    private static void tickAscarisInsideAtmosphere(ServerPlayer visitor, @Nullable AscarisEntity ascaris,
                                                    BodyRegionData.Visit visit) {
        AscarisBodyInteractions.tickVisitorOrgans(visitor, ascaris, visit);
        ServerLevel bodyLevel = (ServerLevel) visitor.level();
        int ticks = visitor.tickCount;

        // Floating pink-white motes inside her chamber
        if (ticks % 25 == 0) {
            bodyLevel.sendParticles((ticks % 50 == 0) ? PINK_DUST : WHITE_DUST,
                    visitor.getX() + (visitor.getRandom().nextDouble() - 0.5) * 4.5,
                    visitor.getY() + 0.8 + visitor.getRandom().nextDouble() * 2.5,
                    visitor.getZ() + (visitor.getRandom().nextDouble() - 0.5) * 4.5,
                    2, 0.25, 0.2, 0.25, 0.01);
        }

        // Gentle organic fluid murmur / pure heartbeat ambience (no bell chimes inside her body!)
        if (ticks % 220 == 0) {
            boolean highObs = JRConfig.isAscarisYandereEnabled()
                    && isOwnedByVisitor(ascaris, visit, visitor)
                    && getEffectiveObsession(ascaris, visit) >= YANDERE_LOCK_OBSESSION;
            if (highObs) {
                bodyLevel.playSound(null, visitor.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.AMBIENT, 0.35f, 0.95f);
            } else {
                bodyLevel.playSound(null, visitor.blockPosition(), SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.AMBIENT, 0.22f, 1.15f);
            }
        }

        // Reverse parasitism: gently siphon sweet nutrition from Ascaris-chan when hungry or injured!
        boolean liveLoaded = ascaris != null && ascaris.isAlive();
        int interval = liveLoaded ? 160 : 240;
        if (ticks % interval == 0 && getEffectiveHunger(ascaris, visit) > 10) {
            boolean needsFood = visitor.getFoodData().getFoodLevel() < 20;
            boolean needsHeal = visitor.getHealth() < visitor.getMaxHealth();
            if (needsFood || needsHeal) {
                if (needsFood) {
                    visitor.getFoodData().eat(1, 0.25f);
                }
                if (needsHeal) {
                    visitor.heal(1.0f);
                }
                adjustHostStats(bodyLevel.getServer(), visitor, visit, 0, -3, 0);
                bodyLevel.sendParticles(ParticleTypes.HEART, visitor.getX(), visitor.getY() + 1.2, visitor.getZ(), 2, 0.3, 0.2, 0.3, 0.01);
                bodyLevel.playSound(null, visitor.blockPosition(), SoundEvents.HONEY_DRINK.value(), SoundSource.PLAYERS, 0.35f, 1.2f);
                if (liveLoaded && ascaris.level() instanceof ServerLevel hostLevel) {
                    hostLevel.sendParticles(PINK_DUST, ascaris.getX(), ascaris.getY() + 0.8, ascaris.getZ(), 5, 0.25, 0.25, 0.25, 0.02);
                }
                if (visitor.getRandom().nextInt(3) == 0) {
                    visitor.sendOverlayMessage(Component.literal("§d你悄悄吸收了粉白蛔虫娘体内的甜食营养~"));
                }
            }
        }
    }

    @Nullable
    public static AscarisEntity findAscarisHost(MinecraftServer server, BodyRegionData.Visit visit) {
        if (visit == null || !visit.isAscaris()) return null;
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().equals(DIMENSION)) continue;
            Entity byRuntime = level.getEntity(visit.hostEntityUuid());
            if (byRuntime instanceof AscarisEntity a) return a;
            if (!visit.host().equals(visit.hostEntityUuid())) {
                Entity byOwner = level.getEntity(visit.host());
                if (byOwner instanceof AscarisEntity a) return a;
            }
        }
        return null;
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

    /** True female Ascaris lumbricoides anatomical interior (pseudocoelom, triradiate pharynx, nerve ring, straight gut, paired uteri & coiled ovaries). */
    static void makeAscarisEntryRoom(ServerLevel level, long slot) {
        AscarisAnatomyBuilder.build(level, slot);
    }

    private static boolean fail(ServerPlayer player, String key) {
        player.sendSystemMessage(Component.translatable(key));
        return false;
    }
}
