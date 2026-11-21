package org.cneko.justarod.client.disassembly;

import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import org.cneko.justarod.disassembly.BodyPart;

/** Full-size vanilla skin UVs, including the outer clothing layer. */
public final class DetachedPartModel {
    public final ModelPart root;
    public DetachedPartModel(BodyPart part, boolean slim) {
        var mesh = new MeshDefinition();
        int u, v, ou, ov, w, h, d;
        if (part == BodyPart.HEAD) { u=0; v=0; ou=32; ov=0; w=8; h=8; d=8; }
        else if (part == BodyPart.LEFT_ARM) { u=32; v=48; ou=48; ov=48; w=slim?3:4; h=12; d=4; }
        else if (part == BodyPart.RIGHT_ARM) { u=40; v=16; ou=40; ov=32; w=slim?3:4; h=12; d=4; }
        else if (part == BodyPart.LEFT_LEG) { u=16; v=48; ou=0; ov=48; w=4; h=12; d=4; }
        else if (part == BodyPart.RIGHT_LEG) { u=0; v=16; ou=0; ov=32; w=4; h=12; d=4; }
        else { u=part == BodyPart.LEFT_EYE?11:7; v=10; ou=u; ov=v; w=2; h=2; d=2; }
        var cube = mesh.getRoot().addOrReplaceChild("part", CubeListBuilder.create().texOffs(u,v)
                .addBox(-w/2f,-h,-d/2f,w,h,d),PartPose.ZERO);
        if (!part.eye()) cube.addOrReplaceChild("outer",CubeListBuilder.create().texOffs(ou,ov)
                .addBox(-w/2f,-h,-d/2f,w,h,d,new CubeDeformation(part == BodyPart.HEAD?0.5f:0.25f)),PartPose.ZERO);
        if (part == BodyPart.HEAD) {
            mesh.getRoot().addOrReplaceChild("left_lid", CubeListBuilder.create().texOffs(12, 9).addBox(1,-4,-4.02f,2,1,0), PartPose.ZERO);
            mesh.getRoot().addOrReplaceChild("right_lid", CubeListBuilder.create().texOffs(9, 9).addBox(-3,-4,-4.02f,2,1,0), PartPose.ZERO);
        }
        root = LayerDefinition.create(mesh,64,64).bakeRoot();
    }
}
