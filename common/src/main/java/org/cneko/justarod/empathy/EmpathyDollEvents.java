package org.cneko.justarod.empathy;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import org.cneko.justarod.config.JRConfig;
import org.cneko.justarod.effect.JREffects;
import org.cneko.justarod.entity.EmpathyDollEntity;
import org.cneko.justarod.item.EmpathyDollItem;
import org.cneko.justarod.packet.EmpathyReactionPayload;
import org.cneko.justarod.sound.JRSounds;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

/** Target-wide cooldown and transient reactions are authoritative and never written into a player's save. */
public final class EmpathyDollEvents {
    private record Reaction(ServerPlayer player, ServerLevel level, EmpathyReaction kind, long startedAt, long cooldownUntil) {
        EmpathyReactionPayload payload() { return new EmpathyReactionPayload(player.getId(), kind.ordinal(), startedAt); }
    }
    private static final Map<UUID, Reaction> ACTIVE = new HashMap<>();
    private static final DustParticleOptions PINK = new DustParticleOptions(0xFF9FCF, 0.8f);

    private EmpathyDollEvents() { }

    public static void init() {
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (player.isSpectator() || !JRConfig.isEmpathyDollEnabled()
                    || !(player.getItemInHand(hand).getItem() instanceof EmpathyDollItem)
                    || !(entity instanceof Player target)) return InteractionResult.PASS;
            if (!level.isClientSide()) {
                EmpathyDollItem.bind(player.getItemInHand(hand), target);
                player.sendOverlayMessage(Component.translatable("message.justarod.empathy_doll.bound", target.getName()));
            }
            return InteractionResult.SUCCESS;
        });
        ServerTickEvents.END_SERVER_TICK.register(EmpathyDollEvents::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ACTIVE.clear());
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> clear(handler.player));
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            if (entity instanceof ServerPlayer player) clear(player);
        });
        EntityTrackingEvents.START_TRACKING.register((entity, observer) -> {
            Reaction reaction = ACTIVE.get(entity.getUUID());
            if (reaction != null && reaction.player == entity && reaction.level == entity.level()
                    && entity.level().getGameTime() - reaction.startedAt < reaction.kind.duration) {
                send(observer, reaction.payload());
            }
        });
    }

    private static void send(ServerPlayer player, EmpathyReactionPayload payload) {
        if (ServerPlayNetworking.canSend(player, EmpathyReactionPayload.ID)) ServerPlayNetworking.send(player, payload);
    }

    private static void broadcast(ServerPlayer player, EmpathyReactionPayload payload) {
        var observers = new HashSet<>(PlayerLookup.tracking(player));
        observers.add(player);
        observers.forEach(observer -> send(observer, payload));
    }

    public static void clear(ServerPlayer player) {
        if (ACTIVE.remove(player.getUUID()) != null)
            broadcast(player, new EmpathyReactionPayload(player.getId(), -1, 0));
    }

    public static EmpathyReaction activeReaction(ServerPlayer player) {
        Reaction state = ACTIVE.get(player.getUUID());
        return state != null && state.player == player && state.level == player.level()
                && state.level.getGameTime() - state.startedAt < state.kind.duration ? state.kind : null;
    }

    public static boolean trigger(EmpathyDollEntity doll, Player operator, EmpathyReaction kind) {
        if (!(doll.level() instanceof ServerLevel level) || !JRConfig.isEmpathyDollEnabled()
                || !doll.isAlive() || !operator.isAlive() || operator.isSpectator() || operator.level() != level
                || !operator.isWithinEntityInteractionRange(doll, 0)) return false;
        UUID bound = EmpathyDollItem.boundPlayer(doll.getDollStack());
        if (bound == null) {
            operator.sendOverlayMessage(Component.translatable("message.justarod.empathy_doll.no_binding"));
            return false;
        }
        ServerPlayer target = level.getServer().getPlayerList().getPlayer(bound);
        if (target == null || !target.isAlive() || target.isSpectator() || target.level() != level) {
            operator.sendOverlayMessage(Component.translatable("message.justarod.empathy_doll.unavailable"));
            return false;
        }
        long now = level.getGameTime();
        Reaction previous = ACTIVE.get(bound);
        if (previous != null && previous.player == target && now < previous.cooldownUntil) {
            // A new squeeze interrupts the current one, resetting both doll and player below.
            // Still ignore duplicate attack callbacks within the same server tick.
            boolean repeatSqueeze = kind == EmpathyReaction.SQUEEZE && previous.kind == EmpathyReaction.SQUEEZE
                    && now > previous.startedAt;
            if (!repeatSqueeze) return false;
        }
        Reaction reaction = new Reaction(target, level, kind, now, now + JRConfig.getEmpathyDollCooldown());
        ACTIVE.put(bound, reaction);
        broadcast(target, reaction.payload());
        target.sendOverlayMessage(Component.translatable("message.justarod.empathy_doll.reaction." + kind.name().toLowerCase(java.util.Locale.ROOT)));
        if (kind == EmpathyReaction.PRICK) {
            target.hurtServer(level, target.damageSources().cactus(), 0.5f);
        }
        if (kind == EmpathyReaction.END_ROD) {
            // The effect owns the hearts, including when applied by rods, commands or other items.
            target.addEffect(new MobEffectInstance(JREffects.Companion.getORGASM_EFFECT(), kind.duration, 0));
        }
        if (kind == EmpathyReaction.TICKLE && target.onGround() && !target.isPassenger()
                && !target.isInWater() && !target.getAbilities().flying) {
            var motion = target.getDeltaMovement();
            target.setDeltaMovement(motion.x, Math.max(motion.y, 0.32), motion.z);
            target.hurtMarked = true;
            target.connection.send(new ClientboundSetEntityMotionPacket(target));
        }
        if (kind == EmpathyReaction.SQUEEZE) {
            doll.squeeze();
            level.playSound(null, doll.getX(), doll.getY(), doll.getZ(), JRSounds.DOLL_SQUEEZE, SoundSource.NEUTRAL, 0.65f, 1);
            if (target.distanceToSqr(doll) > 16 * 16)
                level.playSound(null, target.getX(), target.getY(), target.getZ(), JRSounds.DOLL_SQUEEZE, SoundSource.NEUTRAL, 0.65f, 1);
        }
        particles(reaction, 0);
        return true;
    }

    private static void tick(MinecraftServer server) {
        var iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            Reaction reaction = iterator.next().getValue();
            ServerPlayer player = reaction.player;
            long age = reaction.level.getGameTime() - reaction.startedAt;
            if (!JRConfig.isEmpathyDollEnabled() || !player.isAlive() || player.isRemoved()
                    || player.isSpectator() || player.level() != reaction.level
                    || server.getPlayerList().getPlayer(player.getUUID()) != player || age < 0) {
                broadcast(player, new EmpathyReactionPayload(player.getId(), -1, 0));
                iterator.remove();
            } else if (age >= Math.max(reaction.kind.duration, reaction.cooldownUntil - reaction.startedAt)) {
                iterator.remove();
            } else if (age > 0 && age < reaction.kind.duration) {
                particles(reaction, (int) age);
            }
        }
    }

    private static void particles(Reaction reaction, int age) {
        ServerPlayer p = reaction.player;
        ServerLevel level = reaction.level;
        switch (reaction.kind) {
            case STROKE -> {
                if (age % 12 == 0) level.sendParticles(PINK, p.getX(), p.getY() + 1.1, p.getZ(), 3, 0.3, 0.4, 0.3, 0);
            }
            case TICKLE -> {
                if (age % 8 == 0) level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1, p.getZ(), 2, 0.3, 0.4, 0.3, 0);
            }
            case COLD -> {
                if (age % 6 == 0) {
                    level.sendParticles(ParticleTypes.SNOWFLAKE, p.getX(), p.getY() + 1, p.getZ(), 5, 0.35, 0.6, 0.35, 0.01);
                    level.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.5, p.getZ(), 2, 0.3, 0.3, 0.3, 0.005);
                }
            }
            case PRICK -> {
                if (age == 0) level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, p.getX(), p.getY() + 1, p.getZ(), 3, 0.2, 0.3, 0.2, 0.02);
            }
            case SQUEEZE, END_ROD -> { }
        }
    }
}
