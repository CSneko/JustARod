package org.cneko.justarod.client.ascaris;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.cneko.justarod.Justarod;
import org.cneko.justarod.ascaris.AscarisEntity;
import org.cneko.justarod.ascaris.AscarisPoseState;

import java.util.Map;

/**
 * 粉白蛔虫娘 GeckoLib 5.5 渲染器（v49：80 骨 / 26 动画 / 3 套 UV 一致眼部贴图动态无缝切换）。
 */
public class AscarisRenderer extends GeoEntityRenderer<AscarisEntity, AscarisRenderer.State> {

    public static final class State extends EntityRenderState implements GeoRenderState {
        private final Map<DataTicket<?>, Object> data = new Reference2ObjectOpenHashMap<>();
        float visualScale = 1.0f;
        int eyeVariant = AscarisEntity.EYE_DEFAULT;
        boolean shiveringCritical = false;
        float shiverPhase = 0f;
        float bustBounceY = 0f;
        float bustSwayX = 0f;
        float bustStrideAlt = 0f;

        @Override
        public <D> void addGeckolibData(DataTicket<D> dataTicket, D value) {
            this.data.put(dataTicket, value);
        }

        @Override
        public boolean hasGeckolibData(DataTicket<?> dataTicket) {
            return this.data.containsKey(dataTicket);
        }

        @Override
        public Map<DataTicket<?>, Object> getDataMap() {
            return this.data;
        }
    }

    public AscarisRenderer(EntityRendererProvider.Context context) {
        super(context, new AscarisModel());
        this.shadowRadius = 0.42f;
    }

    @Override
    public State createRenderState(AscarisEntity animatable, Void unused) {
        return new State();
    }

    @Override
    public void captureDefaultRenderState(AscarisEntity animatable, Void unused, State state, float partialTick) {
        super.captureDefaultRenderState(animatable, unused, state, partialTick);
        state.visualScale = animatable.getEffectiveScale();
        state.eyeVariant = animatable.getEyeVariant();
        state.shiveringCritical = animatable.getPoseState() == AscarisPoseState.COLLAPSE
                && animatable.getExpelledStage() == AscarisEntity.EXPELLED_ALBENDAZOLE_CRITICAL;
        state.shiverPhase = (animatable.tickCount + partialTick) * 1.9f;
        state.bustBounceY = animatable.getBustBounceY(partialTick);
        state.bustSwayX = animatable.getBustSwayX(partialTick);
        state.bustStrideAlt = animatable.getBustStrideAlt(partialTick);
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<State> renderPassInfo, BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(renderPassInfo, snapshots);
        State state = renderPassInfo.renderState();
        float by = state.bustBounceY;
        float bx = state.bustSwayX;
        float alt = state.bustStrideAlt;

        if (Math.abs(by) > 0.001f || Math.abs(bx) > 0.001f) {
            snapshots.ifPresent("bust", snap -> {
                snap.setTranslateY(snap.getTranslateY() + by * 0.48f);
                snap.setTranslateX(snap.getTranslateX() + bx * 0.32f);
                snap.setRotX(snap.getRotX() + by * 0.24f);
                snap.setRotZ(snap.getRotZ() - bx * 0.14f);
                float sqY = Mth.clamp(1.0f - by * 0.055f, 0.92f, 1.08f);
                float sqZ = Mth.clamp(1.0f + by * 0.048f, 0.93f, 1.08f);
                snap.setScale(snap.getScaleX(), snap.getScaleY() * sqY, snap.getScaleZ() * sqZ);
            });
        }
        if (Math.abs(alt) > 0.001f) {
            snapshots.ifPresent("bust_left", snap -> {
                snap.setTranslateY(snap.getTranslateY() + alt * 0.16f);
                snap.setRotX(snap.getRotX() + alt * 0.08f);
                snap.setRotZ(snap.getRotZ() + alt * 0.05f);
            });
            snapshots.ifPresent("bust_right", snap -> {
                snap.setTranslateY(snap.getTranslateY() - alt * 0.16f);
                snap.setRotX(snap.getRotX() - alt * 0.08f);
                snap.setRotZ(snap.getRotZ() + alt * 0.05f);
            });
        }
    }

    @Override
    public boolean firePreRenderEvent(RenderPassInfo<State> info, SubmitNodeCollector collector) {
        boolean result = super.firePreRenderEvent(info, collector);
        State state = info.renderState();
        float s = state.visualScale;
        if (Math.abs(s - 1.0f) > 0.005f) {
            info.poseStack().scale(s, s, s);
        }
        if (state.shiveringCritical) {
            float jitter = Mth.sin(state.shiverPhase) * 0.015f;
            info.poseStack().translate(jitter, 0f, -jitter * 0.6f);
        }
        return result;
    }

    public static final class AscarisModel extends GeoModel<AscarisEntity> {
        private static final Identifier MODEL = Identifier.fromNamespaceAndPath(Justarod.MODID, "entity/ascaris");
        private static final Identifier ANIMATION = Identifier.fromNamespaceAndPath(Justarod.MODID, "entity/ascaris");
        private static final Identifier TEXTURE_DEFAULT = Identifier.fromNamespaceAndPath(Justarod.MODID, "textures/entity/ascaris.png");
        private static final Identifier TEXTURE_YANDERE = Identifier.fromNamespaceAndPath(Justarod.MODID, "textures/entity/ascaris_yandere.png");
        private static final Identifier TEXTURE_YANDERE_HEART = Identifier.fromNamespaceAndPath(Justarod.MODID, "textures/entity/ascaris_yandere_heart.png");

        @Override
        public Identifier getModelResource(GeoRenderState renderState) {
            return MODEL;
        }

        @Override
        public Identifier getTextureResource(GeoRenderState renderState) {
            int variant = (renderState instanceof State s) ? s.eyeVariant : AscarisEntity.EYE_DEFAULT;
            return switch (variant) {
                case AscarisEntity.EYE_YANDERE -> TEXTURE_YANDERE;
                case AscarisEntity.EYE_YANDERE_HEART -> TEXTURE_YANDERE_HEART;
                default -> TEXTURE_DEFAULT;
            };
        }

        @Override
        public Identifier getAnimationResource(AscarisEntity animatable) {
            return ANIMATION;
        }
    }
}
