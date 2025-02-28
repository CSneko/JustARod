package org.cneko.justarod.client.renderer

import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.entity.EntityRendererFactory
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.Identifier
import org.cneko.justarod.entity.RodEntity
import software.bernie.geckolib.model.GeoModel
import software.bernie.geckolib.renderer.GeoEntityRenderer
import org.cneko.justarod.JRUtil.Companion.rodId
import software.bernie.geckolib.cache.`object`.BakedGeoModel

class RodRenderer(renderManager: EntityRendererFactory.Context?): GeoEntityRenderer<RodEntity>(renderManager,RodModel()) {
    override fun preRender(
        poseStack: MatrixStack?,
        animatable: RodEntity?,
        model: BakedGeoModel?,
        bufferSource: VertexConsumerProvider?,
        buffer: VertexConsumer?,
        isReRender: Boolean,
        partialTick: Float,
        packedLight: Int,
        packedOverlay: Int,
        colour: Int
    ) {

        if (animatable?.isBaby == true){
            poseStack?.scale(0.5f,0.5f,0.5f)
        }
        super.preRender(
            poseStack,
            animatable,
            model,
            bufferSource,
            buffer,
            isReRender,
            partialTick,
            packedLight,
            packedOverlay,
            colour
        )

    }

}
class RodModel : GeoModel<RodEntity>() {
    override fun getModelResource(animatable: RodEntity): Identifier {
        return rodId("geo/entity/rod.geo.json")
    }

    override fun getTextureResource(animatable: RodEntity): Identifier {
        return rodId("textures/entity/rod.png")
    }

    override fun getAnimationResource(animatable: RodEntity): Identifier {
        return rodId("animations/entity/rod.animation.json")
    }

}