package org.cneko.justarod.client.renderer.empathy;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Vanilla skin UVs on a big-headed, short-bodied, permanently seated plush. */
public final class FumoModel {
    public final ModelPart root;

    public FumoModel(boolean slim) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();
        part(parts, "head", 0, 0, -4, -8, -4, 8, 8, 8,
                32, 0, 0.4f, 0, -6.8f, 0, 0);
        part(parts, "body", 16, 16, -4, 0, -2, 8, 12, 4,
                16, 32, 0.25f, 0, -7, 0, 0);
        int arm = slim ? 3 : 4;
        part(parts, "right_arm", 40, 16, -arm / 2f, 0, -2, arm, 12, 4,
                40, 32, 0.25f, -3.8f, -6.5f, 0, -0.13f);
        part(parts, "left_arm", 32, 48, -arm / 2f, 0, -2, arm, 12, 4,
                48, 48, 0.25f, 3.8f, -6.5f, 0, 0.13f);
        part(parts, "right_leg", 0, 16, -2, 0, -2, 4, 12, 4,
                0, 32, 0.25f, -1.9f, -1.8f, -0.6f, 0);
        part(parts, "left_leg", 16, 48, -2, 0, -2, 4, 12, 4,
                0, 48, 0.25f, 1.9f, -1.8f, -0.6f, 0);
        root = LayerDefinition.create(mesh, 64, 64).bakeRoot();
        scale("head", 1.25f, 1.15f, 1.15f);
        scale("body", 0.7f, 0.45f, 1.05f);
        scale("right_arm", 0.65f, 0.42f, 0.8f);
        scale("left_arm", 0.65f, 0.42f, 0.8f);
        scale("right_leg", 0.85f, 0.42f, 0.85f);
        scale("left_leg", 0.85f, 0.42f, 0.85f);
        root.getChild("right_leg").xRot = -(float) Math.PI / 2;
        root.getChild("left_leg").xRot = -(float) Math.PI / 2;
    }

    private static void part(PartDefinition parent, String name, int u, int v,
                             float x, float y, float z, int w, int h, int d,
                             int outerU, int outerV, float expansion,
                             float px, float py, float pz, float roll) {
        var part = parent.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v)
                .addBox(x, y, z, w, h, d), PartPose.offsetAndRotation(px, py, pz, 0, 0, roll));
        part.addOrReplaceChild("outer", CubeListBuilder.create().texOffs(outerU, outerV)
                .addBox(x, y, z, w, h, d, new CubeDeformation(expansion)), PartPose.ZERO);
    }

    private void scale(String name, float x, float y, float z) {
        var part = root.getChild(name);
        part.xScale = x;
        part.yScale = y;
        part.zScale = z;
    }
}
