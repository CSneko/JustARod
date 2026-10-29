package org.cneko.justarod.client.renderer.empathy;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.cneko.justarod.empathy.EmpathyReaction;
import org.cneko.justarod.packet.EmpathyReactionPayload;

import java.util.Map;
import java.util.WeakHashMap;

/** Cosmetic transforms never change the player's rotation, collision box, scale attribute or controls. */
public final class EmpathyAnimation {
    public record Reaction(ClientLevel level, EmpathyReaction kind, long startedAt) {
        public float age(float partialTick) { return level.getGameTime() - startedAt + partialTick; }
        public float fade(float age) { return Math.min(1, Math.max(0, (kind.duration - age) / 8)); }
    }
    private static final Map<Entity, Reaction> REACTIONS = new WeakHashMap<>();
    private EmpathyAnimation() { }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(EmpathyReactionPayload.ID, (payload, context) -> context.client().execute(() -> accept(payload)));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> REACTIONS.clear());
        ClientTickEvents.END_CLIENT_TICK.register(client -> REACTIONS.entrySet().removeIf(entry -> {
            var entity = entry.getKey();
            var reaction = entry.getValue();
            return entity.level() != client.level || entity.isRemoved() || !entity.isAlive()
                    || reaction.level != client.level || reaction.age(0) >= reaction.kind.duration;
        }));
    }

    public static void accept(EmpathyReactionPayload payload) {
        var level = Minecraft.getInstance().level;
        if (level == null || !(level.getEntity(payload.playerId()) instanceof Player player)) return;
        if (payload.reaction() < 0 || payload.reaction() >= EmpathyReaction.values().length) REACTIONS.remove(player);
        else REACTIONS.put(player, new Reaction(level, EmpathyReaction.values()[payload.reaction()], payload.startedAt()));
    }

    public static Reaction reaction(Entity entity, float partialTick) {
        Reaction reaction = REACTIONS.get(entity);
        if (reaction == null || !entity.isAlive() || entity.isRemoved() || reaction.level != entity.level()) return null;
        float age = reaction.age(partialTick);
        return age >= 0 && age < reaction.kind.duration ? reaction : null;
    }

    public static Reaction reaction(AvatarRenderState state) {
        var client = Minecraft.getInstance();
        return client.level == null ? null : reaction(client.level.getEntity(state.id), partialTick());
    }

    public static float partialTick() { return Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false); }

    public static float shake(Reaction reaction, float age) {
        float fade = reaction.fade(age);
        return switch (reaction.kind) {
            case STROKE -> (float) Math.sin(age * 0.1) * 1.8f * fade;
            case TICKLE -> (float) Math.sin(age * 2.1) * 10 * fade;
            case COLD -> (float) Math.sin(age * 2.7) * 2.8f * fade;
            case PRICK -> (float) Math.sin(age * 0.8) * 12 * (1 - age / reaction.kind.duration);
            case SQUEEZE, END_ROD -> 0;
        };
    }

    public static void transform(PoseStack poses, Reaction reaction, float partialTick) {
        float age = reaction.age(partialTick);
        if (reaction.kind == EmpathyReaction.SQUEEZE) {
            float height = EmpathyReaction.squeezeHeight(age);
            float width = 1 / (float) Math.sqrt(height);
            poses.scale(width, height, width);
        } else {
            poses.mulPose(Axis.ZP.rotationDegrees(shake(reaction, age)));
            if (reaction.kind == EmpathyReaction.TICKLE)
                poses.translate(0, Math.abs(Math.sin(age * 0.75)) * 0.075 * reaction.fade(age), 0);
            if (reaction.kind == EmpathyReaction.PRICK)
                poses.translate(0, Math.sin(age / reaction.kind.duration * Math.PI) * 0.13, 0);
        }
    }

    public static void pose(HumanoidModel<?> model, Reaction reaction) {
        float age = reaction.age(partialTick());
        float fade = reaction.fade(age);
        switch (reaction.kind) {
            case STROKE -> {
                model.rightArm.xRot *= 1 - fade * 0.7f;
                model.leftArm.xRot *= 1 - fade * 0.7f;
                model.rightArm.zRot = 0.12f * fade;
                model.leftArm.zRot = -0.12f * fade;
            }
            case TICKLE -> {
                float wiggle = (float) Math.sin(age * 1.6) * 0.5f * fade;
                model.rightArm.xRot = -0.7f * fade + wiggle;
                model.leftArm.xRot = -0.7f * fade - wiggle;
                model.rightArm.zRot = (0.5f + wiggle) * fade;
                model.leftArm.zRot = (-0.5f + wiggle) * fade;
                model.rightLeg.xRot += wiggle * 0.6f;
                model.leftLeg.xRot -= wiggle * 0.6f;
            }
            case COLD -> {
                float shiver = (float) Math.sin(age * 2.7) * 0.08f * fade;
                model.rightArm.xRot = model.leftArm.xRot = -0.9f * fade + shiver;
                model.rightArm.yRot = -0.55f * fade;
                model.leftArm.yRot = 0.55f * fade;
                model.rightArm.zRot = 0.2f * fade;
                model.leftArm.zRot = -0.2f * fade;
            }
            case PRICK -> {
                model.rightArm.zRot = 0.7f * fade;
                model.leftArm.zRot = -0.7f * fade;
                model.head.xRot -= 0.2f * fade;
            }
            case END_ROD -> {
                float still = age < 12 ? 1 : fade * 0.5f;
                model.rightArm.xRot *= 1 - still;
                model.leftArm.xRot *= 1 - still;
                model.rightLeg.xRot *= 1 - still;
                model.leftLeg.xRot *= 1 - still;
                model.head.xRot *= 1 - still;
                model.head.yRot *= 1 - still;
            }
            case SQUEEZE -> { }
        }
    }

    public static void firstPerson(PoseStack poses, Reaction reaction, float partialTick) {
        float age = reaction.age(partialTick);
        poses.mulPose(Axis.ZP.rotationDegrees(shake(reaction, age) * 0.35f));
        if (reaction.kind == EmpathyReaction.SQUEEZE)
            poses.translate(0, -(1 - EmpathyReaction.squeezeHeight(age)) * 0.15, 0);
        if (reaction.kind == EmpathyReaction.COLD) poses.translate(0, 0.025 * Math.sin(age * 2.7), 0);
    }
}
