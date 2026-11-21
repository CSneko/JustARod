package org.cneko.justarod.pet;

import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.cneko.justarod.config.JRConfig;
import org.cneko.justarod.item.JRItems;
import org.cneko.justarod.packet.*;
import org.cneko.justarod.sound.JRSounds;
import org.cneko.toneko.common.mod.entities.NekoEntity;

import java.util.*;

import static org.cneko.justarod.packet.PetActionPayload.*;

/** Shared server-authoritative gameplay for both villagers and catgirls. */
public final class PetController {
    private record Prompt(int kind, UUID npc, UUID candidate, UUID token, int until, boolean absurd, boolean guaranteed) { }
    private record Gesture(Mob npc, ItemStack item, int until) { }
    private static final Map<UUID, Prompt> PROMPTS = new HashMap<>();
    private static final Map<UUID, Gesture> GESTURES = new HashMap<>();
    private static final Map<UUID, UUID> COMPANIONS = new HashMap<>();
    private PetController() { }

    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(PetActionPayload.ID,
                (payload, context) -> context.server().execute(() -> handle(context.player(), payload)));
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (!(player instanceof ServerPlayer p) || hand != InteractionHand.MAIN_HAND || !p.isShiftKeyDown()
                    || !p.getMainHandItem().isEmpty() || !validOwner(entity) || p.isSpectator()
                    || (PetState.of(p).owner == null && !JRConfig.isPlayerTamingEnabled()))
                return InteractionResult.PASS;
            open(p, entity.getId(), false);
            return InteractionResult.SUCCESS;
        });
        ServerTickEvents.END_SERVER_TICK.register(PetController::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            cancel(handler.player, false);
            PetState state = PetState.of(handler.player);
            if (JRConfig.isAbsurdityEnabled() && state.owner != null)
                state.pausedUntil = handler.player.tickCount + 20;
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> cancel(handler.player, false));
        ServerPlayerEvents.COPY_FROM.register((old, replacement, alive) -> {
            PetState from = PetState.of(old), to = PetState.of(replacement);
            to.owner = from.owner;
            to.ownerName = from.ownerName;
            to.ownerActionIntervalTicks = from.ownerActionIntervalTicks;
            cancel(replacement, false);
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((old, replacement, alive) -> cancel(replacement, false));
        EntityTrackingEvents.START_TRACKING.register((entity, observer) -> {
            if (entity instanceof ServerPlayer p) send(observer, snapshot(p, 0));
            Gesture gesture = GESTURES.get(entity.getUUID());
            if (gesture != null) equipment(observer, gesture.npc, gesture.item);
        });
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof NekoEntity neko) neko.getGoalSelector().addGoal(1, new PetNekoFollowGoal(neko));
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            Gesture gesture = GESTURES.remove(entity.getUUID());
            if (gesture != null) restore(gesture);
            COMPANIONS.remove(entity.getUUID());
            if (entity instanceof Mob mob) PetOwnerActivity.remove(mob);
            if (entity instanceof ServerPlayer p) cancel(p, false);
            if ((entity instanceof Villager || entity instanceof NekoEntity) && ((LivingEntity) entity).getHealth() <= 0) {
                for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
                    if (entity.getUUID().equals(PetState.of(p).owner)) release(p);
                }
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            PROMPTS.clear(); GESTURES.clear(); COMPANIONS.clear(); PetOwnerActivity.clear();
        });
    }

    public static boolean validOwner(Entity entity) {
        return (entity instanceof Villager || entity instanceof NekoEntity)
                && entity.isAlive() && !((LivingEntity) entity).isBaby();
    }

    public static Mob owner(ServerPlayer player) {
        UUID id = PetState.of(player).owner;
        if (id == null) return null;
        // Loaded entities only: no forced chunks, no cross-dimension summon.
        for (ServerLevel level : player.level().getServer().getAllLevels()) {
            Entity entity = level.getEntity(id);
            if (entity instanceof Mob mob && validOwner(mob)) return mob;
        }
        return null;
    }

    static boolean close(ServerPlayer p, Entity npc) {
        return p.isAlive() && !p.isSpectator() && validOwner(npc) && p.level() == npc.level()
                && p.distanceToSqr(npc) <= 36 && p.hasLineOfSight(npc);
    }

    private static boolean available(ServerPlayer p) {
        return p.isAlive() && !p.isSpectator() && !p.isPassenger() && !p.isSleeping()
                && !p.isChangingDimension() && !p.getAbilities().flying;
    }

    public static void message(ServerPlayer p, String key, Object... args) {
        p.sendOverlayMessage(Component.translatable("pet.justarod." + key, args));
    }

    public static void open(ServerPlayer p, int entityId, boolean guaranteed) {
        PetState s = PetState.of(p);
        if (s.owner == null && !JRConfig.isPlayerTamingEnabled()) return;
        if (!guaranteed && p.tickCount < s.nextMenu) return;
        s.nextMenu = p.tickCount + 5;
        Entity entity = entityId < 0 ? owner(p) : p.level().getEntity(entityId);
        if (!close(p, entity)) { message(p, "nearby_required"); return; }
        if (s.owner == null) {
            if (!guaranteed && p.tickCount < s.nextOffer) { message(p, "cooldown"); return; }
            show(p, PetMenuPayload.OFFER, (Mob) entity, null, false, guaranteed);
            gesture((Mob) entity, fish((Mob) entity), 100);
            voice((Mob) entity);
        } else if (s.owner.equals(entity.getUUID())) {
            Mob candidate = p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(6),
                    e -> validOwner(e) && e != entity && close(p, e)).stream()
                    .min(Comparator.<Mob>comparingInt(e -> entity instanceof Villager && e instanceof NekoEntity ? 0 : 1)
                            .thenComparingDouble(p::distanceToSqr)).orElse(null);
            boolean absurd = JRConfig.isAbsurdityEnabled() && entity instanceof Villager && candidate instanceof NekoEntity;
            show(p, PetMenuPayload.OWNER, (Mob) entity, candidate, absurd, false);
        } else message(p, "already_owned");
    }

    private static void show(ServerPlayer p, int kind, Mob npc, Mob candidate, boolean absurd, boolean guaranteed) {
        PetOwnerActivity.prepare(p, npc, 600);
        if (kind == PetMenuPayload.TRANSFER && candidate != null) PetOwnerActivity.prepare(p, candidate, 600);
        UUID token = UUID.randomUUID();
        PROMPTS.put(p.getUUID(), new Prompt(kind, npc.getUUID(), candidate == null ? null : candidate.getUUID(),
                token, p.tickCount + 600, absurd, guaranteed));
        if (ServerPlayNetworking.canSend(p, PetMenuPayload.ID))
            ServerPlayNetworking.send(p, new PetMenuPayload(kind, npc.getId(), name(npc),
                    candidate == null ? "" : name(candidate), token, absurd));
    }

    private static String name(Entity entity) {
        String name = entity.getName().getString();
        return name.substring(0, Math.min(128, name.length()));
    }

    /** Public to allow headless interaction tests without a client UI. No validation is skipped. */
    public static void handle(ServerPlayer p, PetActionPayload packet) {
        if (packet.action() == CANCEL) { cancel(p, true); return; }
        if (packet.action() == OPEN) { open(p, packet.entityId(), false); return; }
        Prompt prompt = PROMPTS.get(p.getUUID());
        if (prompt == null || !prompt.token.equals(packet.token())) return;
        PROMPTS.remove(p.getUUID()); // consume before mutation; replay cannot confirm twice
        PetState s = PetState.of(p);
        if (packet.action() == DECLINE) {
            if (prompt.kind == PetMenuPayload.OFFER) s.nextOffer = p.tickCount + 600;
            PetOwnerActivity.endProposal(p);
            return;
        }
        if (prompt.until < p.tickCount || !p.isAlive() || p.isSpectator()) return;
        // Releasing a relationship is possible even when the old NPC is unavailable.
        if (packet.action() == RELEASE && prompt.kind == PetMenuPayload.OWNER) { release(p); return; }
        Entity entity = p.level().getEntity(prompt.npc);
        if (!close(p, entity)) { message(p, "nearby_required"); return; }
        Mob npc = (Mob) entity;
        if (packet.action() == ACCEPT && prompt.kind == PetMenuPayload.OFFER && s.owner == null) {
            if (!JRConfig.isPlayerTamingEnabled()) {
                PetOwnerActivity.endProposal(p);
                return;
            }
            if (p.tickCount < s.nextOffer && !prompt.guaranteed) return;
            s.nextOffer = p.tickCount + 100;
            p.getFoodData().eat(2, 0.3f);
            eat(p, npc);
            if (prompt.guaranteed || p.getRandom().nextInt(3) == 0) bind(p, npc);
            else { particles(p, ParticleTypes.SMOKE); message(p, "try_again"); }
            return;
        }
        if (!npc.getUUID().equals(s.owner)) return;
        if (packet.action() == CONFIRM && prompt.kind == PetMenuPayload.TRANSFER) {
            Entity target = prompt.candidate == null ? null : p.level().getEntity(prompt.candidate);
            if (!close(p, target) || target == npc || (prompt.absurd && !JRConfig.isAbsurdityEnabled())) return;
            bind(p, (Mob) target);
            PetOwnerActivity.prepare(p, npc, 120);
            if (prompt.absurd) {
                // A theatrical exchange, never an inventory transaction or a real trade recipe.
                gesture(npc, new ItemStack(Items.EMERALD, 3), 80);
                sound(p, SoundEvents.VILLAGER_YES);
                message(p, "transferred_absurd", target.getName());
            } else message(p, "transferred", target.getName());
            return;
        }
        if (prompt.kind != PetMenuPayload.OWNER) return;
        PetOwnerActivity.defer(p);
        switch (packet.action()) {
            case SIT -> order(p, PetState.Mode.SIT);
            case STAND -> order(p, PetState.Mode.FREE);
            case FOLLOW -> order(p, PetState.Mode.FOLLOW);
            case STROKE -> stroke(p, false);
            case FEED -> feed(p);
            case TRANSFER -> {
                Entity target = prompt.candidate == null ? null : p.level().getEntity(prompt.candidate);
                if (target instanceof Mob candidate) requestTransfer(p, candidate);
            }
            case NEKO_FOLLOW -> {
                if (npc instanceof NekoEntity) {
                    if (COMPANIONS.containsKey(npc.getUUID())) COMPANIONS.remove(npc.getUUID());
                    else {
                        cancel(p, false);
                        COMPANIONS.put(npc.getUUID(), p.getUUID());
                    }
                    message(p, "neko_follow");
                }
            }
            default -> { }
        }
    }

    public static boolean bind(ServerPlayer p, Mob npc) {
        PetState s = PetState.of(p);
        if ((s.owner == null && !JRConfig.isPlayerTamingEnabled()) || !close(p, npc)) return false;
        PetOwnerActivity.forget(p);
        cancel(p, false);
        s.owner = npc.getUUID(); s.ownerName = name(npc);
        s.nextOrder = p.tickCount + 600;
        s.pausedUntil = p.tickCount + (JRConfig.isAbsurdityEnabled() ? 20 : 100);
        s.nextFeed = p.tickCount + (JRConfig.isAbsurdityEnabled() ? 120 : 1200);
        s.nextOwnerAction = p.tickCount + 20;
        s.ownerActivityPaused = false;
        PetOwnerActivity.maintain(p, npc);
        npc.setPersistenceRequired();
        particles(p, ParticleTypes.HEART);
        voice(npc); broadcast(p, 0);
        message(p, "tamed", npc.getName());
        return true;
    }

    public static boolean requestTransfer(ServerPlayer p, Mob target) {
        Mob npc = owner(p);
        if (!close(p, npc) || !close(p, target) || npc == target) return false;
        boolean absurd = JRConfig.isAbsurdityEnabled() && npc instanceof Villager && target instanceof NekoEntity;
        show(p, PetMenuPayload.TRANSFER, npc, target, absurd, false);
        return true;
    }

    public static boolean order(ServerPlayer p, PetState.Mode mode) {
        boolean applied = issueOrder(p, mode);
        if (applied) PetOwnerActivity.defer(p);
        return applied;
    }

    static boolean issueOrder(ServerPlayer p, PetState.Mode mode) {
        Mob npc = owner(p);
        if (!available(p) || !close(p, npc)) return false;
        PetState s = PetState.of(p);
        s.mode = mode;
        s.sitX = p.getX(); s.sitZ = p.getZ();
        s.nextOrder = p.tickCount + 1200;
        s.pausedUntil = p.tickCount;
        s.nextRecall = p.tickCount + 60;
        COMPANIONS.remove(npc.getUUID());
        voice(npc); broadcast(p, 0);
        message(p, "order." + mode.name().toLowerCase(Locale.ROOT));
        return true;
    }

    public static void cancel(ServerPlayer p, boolean notify) {
        PetState s = PetState.of(p);
        s.mode = PetState.Mode.FREE;
        s.pausedUntil = p.tickCount + 1200;
        s.nextOrder = p.tickCount + 1200;
        PROMPTS.remove(p.getUUID());
        PetOwnerActivity.endProposal(p);
        COMPANIONS.entrySet().removeIf(e -> e.getValue().equals(p.getUUID()));
        broadcast(p, -1);
        if (notify) message(p, "cancelled");
    }

    public static void release(ServerPlayer p) {
        Mob npc = owner(p);
        if (npc != null) stopGesture(npc);
        PetOwnerActivity.forget(p);
        cancel(p, false);
        PetState s = PetState.of(p);
        s.owner = null; s.ownerName = ""; s.nextOffer = p.tickCount + 1200;
        broadcast(p, 0);
        message(p, "released");
    }

    public static boolean stroke(ServerPlayer p, boolean directed) {
        Mob npc = owner(p);
        PetState s = PetState.of(p);
        if (!close(p, npc) || (!directed && p.tickCount < s.nextInteraction)) return false;
        if (directed) PetOwnerActivity.defer(p);
        s.nextInteraction = p.tickCount + 100;
        npc.getLookControl().setLookAt(p, 30, 30);
        npc.swing(InteractionHand.MAIN_HAND);
        particles(p, ParticleTypes.HEART);
        sound(p, JRSounds.PET_PURR);
        voice(npc); broadcast(p, 2);
        return true;
    }

    public static boolean tempt(ServerPlayer p) {
        boolean applied = temptFromOwner(p);
        if (applied) PetOwnerActivity.defer(p);
        return applied;
    }

    static boolean temptFromOwner(ServerPlayer p) {
        return temptFromOwner(p, 80);
    }

    static boolean temptFromOwner(ServerPlayer p, int displayTicks) {
        Mob npc = owner(p);
        if (!close(p, npc)) return false;
        gesture(npc, fish(npc), displayTicks);
        broadcast(p, 1);
        return true;
    }

    static boolean feed(ServerPlayer p) {
        return feed(p, 60);
    }

    static boolean feed(ServerPlayer p, int displayTicks) {
        PetState s = PetState.of(p);
        Mob npc = owner(p);
        if (!close(p, npc)) return false;
        if (p.tickCount < s.nextFeed) { message(p, "cooldown"); return false; }
        s.nextFeed = p.tickCount + (JRConfig.isAbsurdityEnabled() ? 120 : 1200);
        p.getFoodData().eat(4, 0.5f);
        eat(p, npc, displayTicks); particles(p, ParticleTypes.HEART);
        return true;
    }

    private static void eat(ServerPlayer p, Mob npc) {
        eat(p, npc, 60);
    }

    private static void eat(ServerPlayer p, Mob npc, int displayTicks) {
        gesture(npc, fish(npc), displayTicks);
        npc.getLookControl().setLookAt(p, 30, 30);
        npc.swing(InteractionHand.MAIN_HAND);
        sound(p, SoundEvents.GENERIC_EAT.value());
    }

    private static ItemStack fish(Mob npc) {
        return new ItemStack(npc instanceof NekoEntity ? JRItems.Companion.getDRIED_FISH() : Items.COD);
    }

    /** Cosmetic equipment packets never put props into inventories, saves or death drops. */
    private static void gesture(Mob npc, ItemStack item, int ticks) {
        GESTURES.put(npc.getUUID(), new Gesture(npc, item, npc.level().getServer().getTickCount() + ticks));
        PlayerLookup.tracking(npc).forEach(p -> equipment(p, npc, item));
    }

    private static void equipment(ServerPlayer observer, Mob npc, ItemStack item) {
        observer.connection.send(new ClientboundSetEquipmentPacket(npc.getId(), List.of(Pair.of(EquipmentSlot.MAINHAND, item.copy()))));
    }
    private static void restore(Gesture g) {
        PlayerLookup.tracking(g.npc).forEach(p -> equipment(p, g.npc, g.npc.getMainHandItem()));
    }
    static void stopGesture(Mob npc) {
        Gesture gesture = GESTURES.remove(npc.getUUID());
        if (gesture != null) restore(gesture);
    }
    static void voice(Mob npc) {
        npc.level().playSound(null, npc.getX(), npc.getY(), npc.getZ(),
                npc instanceof NekoEntity ? JRSounds.PET_NEKO_VOICE : SoundEvents.VILLAGER_AMBIENT,
                SoundSource.NEUTRAL, 0.65f, 1);
    }
    private static void sound(ServerPlayer p, SoundEvent event) {
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), event, SoundSource.PLAYERS, 0.7f, 1);
    }
    private static void particles(ServerPlayer p, net.minecraft.core.particles.ParticleOptions type) {
        ((ServerLevel) p.level()).sendParticles(type, p.getX(), p.getY() + 1.8, p.getZ(), 7, 0.3, 0.25, 0.3, 0.02);
    }

    private static PetStatePayload snapshot(ServerPlayer p, int reaction) {
        Mob npc = owner(p);
        return new PetStatePayload(p.getId(), PetState.of(p).mode.ordinal(),
                npc == null || npc.level() != p.level() ? -1 : npc.getId(), reaction);
    }
    private static void send(ServerPlayer p, PetStatePayload packet) {
        if (p.connection != null && ServerPlayNetworking.canSend(p, PetStatePayload.ID)) ServerPlayNetworking.send(p, packet);
    }
    private static void broadcast(ServerPlayer p, int reaction) {
        var packet = snapshot(p, reaction);
        var players = new HashSet<>(PlayerLookup.tracking(p)); players.add(p);
        players.forEach(observer -> send(observer, packet));
    }

    public static ServerPlayer companion(NekoEntity npc) {
        UUID id = COMPANIONS.get(npc.getUUID());
        if (id == null || !(npc.level() instanceof ServerLevel level)) return null;
        ServerPlayer p = level.getServer().getPlayerList().getPlayer(id);
        return p != null && available(p) && p.level() == level && npc.getUUID().equals(PetState.of(p).owner)
                && PetState.of(p).mode != PetState.Mode.FOLLOW ? p : null;
    }

    private static void tick(MinecraftServer server) {
        boolean tamingEnabled = JRConfig.isPlayerTamingEnabled();
        PROMPTS.entrySet().removeIf(entry -> {
            ServerPlayer p = server.getPlayerList().getPlayer(entry.getKey());
            Prompt prompt = entry.getValue();
            return p == null || p.tickCount > prompt.until || !p.isAlive()
                    || (!tamingEnabled && prompt.kind == PetMenuPayload.OFFER);
        });
        var it = GESTURES.values().iterator();
        while (it.hasNext()) {
            Gesture g = it.next();
            if (server.getTickCount() >= g.until || g.npc.isRemoved()) { restore(g); it.remove(); }
            else if (server.getTickCount() % 20 == 0)
                PlayerLookup.tracking(g.npc).forEach(p -> equipment(p, g.npc, g.item));
        }
        for (ServerPlayer p : server.getPlayerList().getPlayers()) tickPlayer(p);
        PetOwnerActivity.tick(server);
    }

    private static void tickPlayer(ServerPlayer p) {
        PetState s = PetState.of(p);
        Mob npc = owner(p);
        if (s.mode != PetState.Mode.FREE && (!available(p) || p.hurtTime > 0 || npc == null || npc.level() != p.level()))
            cancel(p, false);
        if (!available(p)) return;
        if (s.mode == PetState.Mode.SIT && (Math.abs(p.getX() - s.sitX) > 0.12 || Math.abs(p.getZ() - s.sitZ) > 0.12))
            cancel(p, true);
        if (npc != null && s.mode == PetState.Mode.FOLLOW && p.distanceToSqr(npc) > 144 && p.tickCount >= s.nextRecall)
            recall(p, false);
        if (p.tickCount % 40 == 0 && s.owner != null) broadcast(p, 0);
        if (npc != null && JRConfig.isAbsurdityEnabled()) {
            PetOwnerActivity.maintain(p, npc);
            if (PetOwnerActivity.directs(p, npc)) return;
        }
        if (p.tickCount % 20 != 0) return;
        if (s.owner == null) {
            if (!JRConfig.isPlayerTamingEnabled() || p.tickCount < s.nextHint || p.tickCount < s.nextOffer
                    || PROMPTS.containsKey(p.getUUID())) return;
            Mob offerer = p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(5), e -> close(p, e))
                    .stream().min(Comparator.comparingDouble(p::distanceToSqr)).orElse(null);
            if (offerer != null) {
                PetOwnerActivity.prepare(p, offerer, 160);
                gesture(offerer, fish(offerer), 100); voice(offerer);
                message(p, "offer_hint", offerer.getName()); s.nextHint = p.tickCount + (JRConfig.isAbsurdityEnabled() ? 100 : 400);
            }
            return;
        }
        if (!close(p, npc) || p.tickCount < s.pausedUntil) return;
        if (p.tickCount >= s.nextInstinct && (GESTURES.containsKey(npc.getUUID())
                || npc.getMainHandItem().is(Items.COD) || npc.getMainHandItem().is(JRItems.Companion.getDRIED_FISH()))) {
            broadcast(p, 1); s.nextInstinct = p.tickCount + 200;
        }
        if (p.tickCount >= s.nextInteraction && p.distanceToSqr(npc) < 6.25 && p.getRandom().nextInt(12) == 0)
            stroke(p, false);
        if (p.getFoodData().getFoodLevel() <= 14 && p.tickCount >= s.nextFeed) feed(p);
        if (s.mode == PetState.Mode.FREE && p.tickCount >= s.nextOrder && !COMPANIONS.containsKey(npc.getUUID()))
            order(p, p.getRandom().nextBoolean() ? PetState.Mode.SIT : PetState.Mode.FOLLOW);
    }

    static boolean hasPrompt(ServerPlayer p) { return PROMPTS.containsKey(p.getUUID()); }

    public static boolean recall(ServerPlayer p, boolean directed) {
        PetState s = PetState.of(p);
        Mob npc = owner(p);
        if (!available(p) || npc == null || npc.level() != p.level()
                || (!directed && (s.mode != PetState.Mode.FOLLOW || p.tickCount < s.pausedUntil || p.tickCount < s.nextRecall))) return false;
        s.nextRecall = p.tickCount + 100; // unsuccessful searches are rate limited too
        ServerLevel level = (ServerLevel) p.level();
        BlockPos center = npc.blockPosition();
        for (int dy : new int[]{0, 1, -1, 2, -2}) for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            if (dx * dx + dz * dz < 4) continue;
            Vec3 destination = Vec3.atBottomCenterOf(center.offset(dx, dy, dz));
            if (!safeDestination(p, destination)) continue;
            particles(p, ParticleTypes.PORTAL);
            p.teleportTo(destination.x, destination.y, destination.z);
            p.setDeltaMovement(Vec3.ZERO); p.fallDistance = 0;
            particles(p, ParticleTypes.PORTAL); sound(p, SoundEvents.ENDERMAN_TELEPORT);
            message(p, "recalled");
            return true;
        }
        return false;
    }

    public static boolean safeDestination(ServerPlayer p, Vec3 destination) {
        ServerLevel level = (ServerLevel) p.level();
        AABB box = p.getBoundingBox().move(destination.subtract(p.position()));
        BlockPos min = BlockPos.containing(box.minX - 0.5, box.minY - 1, box.minZ - 0.5);
        BlockPos max = BlockPos.containing(box.maxX + 0.5, box.maxY, box.maxZ + 0.5);
        if (box.minY < level.getMinY() || box.maxY >= level.getMaxY() || !level.hasChunksAt(min, max)
                || !level.getWorldBorder().isWithinBounds(box) || !level.noCollision(p, box) || level.containsAnyLiquid(box)) return false;
        if (level.noBlockCollision(p, new AABB(box.minX + 0.05, box.minY - 0.1, box.minZ + 0.05,
                box.maxX - 0.05, box.minY, box.maxZ - 0.05))) return false;
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            var block = level.getBlockState(pos);
            if (!level.getFluidState(pos).isEmpty() || block.is(BlockTags.FIRE) || block.is(Blocks.CACTUS)
                    || block.is(Blocks.MAGMA_BLOCK) || block.is(Blocks.CAMPFIRE) || block.is(Blocks.SOUL_CAMPFIRE)
                    || block.is(Blocks.SWEET_BERRY_BUSH) || block.is(Blocks.POWDER_SNOW)
                    || block.is(Blocks.WITHER_ROSE) || block.is(Blocks.END_PORTAL) || block.is(Blocks.NETHER_PORTAL)) return false;
        }
        return true;
    }
}
