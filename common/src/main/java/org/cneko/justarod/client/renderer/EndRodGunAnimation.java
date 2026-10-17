package org.cneko.justarod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import org.cneko.justarod.item.EndRodGunItem;
import org.cneko.justarod.packet.EndRodGunShotPayload;

import java.util.Map;
import java.util.WeakHashMap;

public final class EndRodGunAnimation {
    private record Shot(long tick, int hands, int interval) { }
    private static final Map<LivingEntity, Shot> SHOTS = new WeakHashMap<>();

    private EndRodGunAnimation() { }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(EndRodGunShotPayload.ID, (payload, context) -> {
            var level = context.client().level;
            if (level != null && level.getEntity(payload.playerId()) instanceof LivingEntity player) {
                SHOTS.put(player, new Shot(level.getGameTime(), payload.hands(), Math.max(1, payload.interval())));
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> SHOTS.clear());
    }

    public static InteractionHand handForArm(LivingEntity player, HumanoidArm arm) {
        return arm == player.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }

    public static boolean holdsGun(LivingEntity player, HumanoidArm arm) {
        return player.getItemInHand(handForArm(player, arm)).getItem() instanceof EndRodGunItem;
    }

    public static float recoil(LivingEntity player, HumanoidArm arm, float partialTick) {
        Shot shot = SHOTS.get(player);
        int bit = handForArm(player, arm) == InteractionHand.MAIN_HAND ? 1 : 2;
        if (shot == null || (shot.hands() & bit) == 0) return 0;
        float age = player.level().getGameTime() - shot.tick() + partialTick;
        if (age < 0 || age >= shot.interval()) return 0;
        // A short kick followed by a smooth recovery, completed before the next shot.
        if (age < 0.35f) return age / 0.35f;
        float recovery = 1 - (age - 0.35f) / (shot.interval() - 0.35f);
        return recovery * recovery;
    }

    public static float aimAmount(LivingEntity player, float partialTick) {
        return player.isUsingItem() && player.getUseItem().getItem() instanceof EndRodGunItem
                ? Mth.clamp(player.getTicksUsingItem(partialTick) / 3f, 0, 1) : 0;
    }

    public static void poseArm(net.minecraft.client.model.geom.ModelPart arm, LivingEntity player,
                               HumanoidArm side, HumanoidRenderState state, float partialTick) {
        float aim = aimAmount(player, partialTick);
        float pitch = Mth.clamp(state.xRot, -75, 75) * Mth.DEG_TO_RAD;
        float yaw = Mth.clamp(state.yRot, -70, 70) * Mth.DEG_TO_RAD;
        float breathing = Mth.sin(state.ageInTicks * 0.09f) * 0.012f;
        arm.xRot = Mth.lerp(aim, -1.15f + pitch * 0.5f, -Mth.HALF_PI + pitch)
                - recoil(player, side, partialTick) * 0.16f + breathing;
        arm.yRot = yaw + (side == HumanoidArm.RIGHT ? -0.06f : 0.06f);
        arm.zRot = side == HumanoidArm.RIGHT ? 0.035f : -0.035f;
    }

    public static boolean isGunView(LivingEntity player, net.minecraft.world.item.ItemStack stack, ItemDisplayContext display) {
        return stack.getItem() instanceof EndRodGunItem
                && (display == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || display == ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
    }

    public static void poseFirstPerson(PoseStack poses, LivingEntity player, ItemDisplayContext display, float partialTick) {
        HumanoidArm arm = display == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ? HumanoidArm.RIGHT : HumanoidArm.LEFT;
        int sign = arm == HumanoidArm.RIGHT ? 1 : -1;
        float kick = recoil(player, arm, partialTick);
        float aim = aimAmount(player, partialTick);
        float breathing = Mth.sin((player.tickCount + partialTick) * 0.09f) * 0.003f;
        poses.translate(-sign * (0.035f + aim * 0.06f), 0.025f + aim * 0.055f + breathing + kick * 0.018f,
                -aim * 0.035f + kick * 0.1f);
        poses.mulPose(Axis.XP.rotationDegrees(kick * 7f));
        poses.mulPose(Axis.ZP.rotationDegrees(sign * kick * -1.5f));
        if (EndRodGunItem.isSpinning(player)) {
            poses.translate(sign * 0.14f, -0.08f, 0);
            poses.mulPose(Axis.YP.rotationDegrees(-sign * 22f));
        }
    }
}
