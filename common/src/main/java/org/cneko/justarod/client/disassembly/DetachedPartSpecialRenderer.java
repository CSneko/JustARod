package org.cneko.justarod.client.disassembly;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.item.ItemStack;
import org.cneko.justarod.disassembly.*;
import org.joml.Vector3fc;
import java.util.EnumMap;
import java.util.function.Consumer;

public final class DetachedPartSpecialRenderer implements SpecialModelRenderer<DetachedPartSpecialRenderer.Argument> {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("justarod","detached_part");
    public record Argument(BodyPart part, PlayerSkinRenderCache.RenderInfo skin) { }
    private final PlayerSkinRenderCache skins;
    private final EnumMap<BodyPart,DetachedPartModel> wide = new EnumMap<>(BodyPart.class), slim = new EnumMap<>(BodyPart.class);
    public DetachedPartSpecialRenderer(PlayerSkinRenderCache skins) {
        this.skins=skins;
        for (BodyPart part : BodyPart.values()) { wide.put(part,new DetachedPartModel(part,false)); slim.put(part,new DetachedPartModel(part,true)); }
    }
    public record Unbaked() implements SpecialModelRenderer.Unbaked<Argument> {
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(new Unbaked());
        @Override public SpecialModelRenderer<Argument> bake(BakingContext context) { return new DetachedPartSpecialRenderer(context.playerSkinRenderCache()); }
        @Override public MapCodec<Unbaked> type() { return CODEC; }
    }
    @Override public Argument extractArgument(ItemStack stack) {
        var profile = stack.get(DataComponents.PROFILE);
        return new Argument(stack.getItem() instanceof DetachedPartItem item?item.part:BodyPart.HEAD,profile==null?null:skins.getOrDefault(profile));
    }
    @Override public void submit(Argument arg, PoseStack poses, SubmitNodeCollector collector,int light,int overlay,boolean foil,int outline) {
        if (arg == null) return;
        var skin = arg.skin()==null?DefaultPlayerSkin.getDefaultSkin():arg.skin().playerSkin();
        poses.pushPose(); poses.translate(0.5,arg.part().eye()?0.3125:arg.part()==BodyPart.HEAD?0.25:0.125,0.5); poses.scale(1,-1,-1);
        if (arg.part().eye()) poses.scale(3,3,3);
        if (arg.part() == BodyPart.HEAD) {
            var model = (skin.model()==PlayerModelType.SLIM?slim:wide).get(arg.part());
            var client = net.minecraft.client.Minecraft.getInstance();
            boolean blink = client.level != null && client.level.getGameTime() % 85 < 3;
            model.root.getChild("left_lid").visible = blink; model.root.getChild("right_lid").visible = blink;
        }
        collector.submitModelPart((skin.model()==PlayerModelType.SLIM?slim:wide).get(arg.part()).root,poses,
                RenderTypes.entityTranslucent(skin.body().texturePath()),light,overlay,null,false,foil,-1,null,outline);
        poses.popPose();
    }
    @Override public void getExtents(Consumer<Vector3fc> consumer) {
        for (var entry : wide.entrySet()) {
            BodyPart part = entry.getKey();
            PoseStack poses=new PoseStack(); poses.translate(0.5,part.eye()?0.3125:part==BodyPart.HEAD?0.25:0.125,0.5); poses.scale(1,-1,-1);
            if (part.eye()) poses.scale(3,3,3);
            entry.getValue().root.getExtentsForGui(poses,consumer);
        }
    }
}
