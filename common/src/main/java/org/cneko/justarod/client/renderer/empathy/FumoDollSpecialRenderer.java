package org.cneko.justarod.client.renderer.empathy;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.item.ItemStack;
import org.cneko.justarod.Justarod;
import org.joml.Vector3fc;

import java.util.function.Consumer;

/** The carried item uses the very same plush and skin as the placed entity. */
public final class FumoDollSpecialRenderer implements SpecialModelRenderer<PlayerSkinRenderCache.RenderInfo> {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Justarod.MODID, "fumo_doll");
    private final PlayerSkinRenderCache skins;
    private final FumoModel wide = new FumoModel(false);
    private final FumoModel slim = new FumoModel(true);

    public FumoDollSpecialRenderer(PlayerSkinRenderCache skins) { this.skins = skins; }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<PlayerSkinRenderCache.RenderInfo> {
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(new Unbaked());
        @Override public SpecialModelRenderer<PlayerSkinRenderCache.RenderInfo> bake(BakingContext context) {
            return new FumoDollSpecialRenderer(context.playerSkinRenderCache());
        }
        @Override public MapCodec<Unbaked> type() { return CODEC; }
    }

    @Override public PlayerSkinRenderCache.RenderInfo extractArgument(ItemStack stack) {
        var profile = stack.get(DataComponents.PROFILE);
        return profile == null ? null : skins.getOrDefault(profile);
    }

    private static void transform(PoseStack poses) {
        poses.translate(0.5, 0, 0.5);
        poses.scale(0.85f, -0.85f, -0.85f);
    }

    @Override
    public void submit(PlayerSkinRenderCache.RenderInfo info, PoseStack poses, SubmitNodeCollector collector,
                       int light, int overlay, boolean foil, int outline) {
        var skin = info == null ? DefaultPlayerSkin.getDefaultSkin() : info.playerSkin();
        poses.pushPose();
        transform(poses);
        collector.submitModelPart((skin.model() == PlayerModelType.SLIM ? slim : wide).root, poses,
                RenderTypes.entityTranslucent(skin.body().texturePath()), light, overlay, null, false, foil, -1, null, outline);
        poses.popPose();
    }

    @Override public void getExtents(Consumer<Vector3fc> consumer) {
        PoseStack poses = new PoseStack();
        transform(poses);
        wide.root.getExtentsForGui(poses, consumer);
    }
}
