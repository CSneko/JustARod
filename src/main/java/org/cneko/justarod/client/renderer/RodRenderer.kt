package org.cneko.justarod.client.renderer

import net.minecraft.client.renderer.RenderType
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.EntityRendererProvider
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.resources.ResourceLocation
import org.cneko.justarod.entity.RodEntity
import software.bernie.geckolib.model.GeoModel
import software.bernie.geckolib.renderer.GeoEntityRenderer
import org.cneko.justarod.JRUtil.Companion.rodId
import org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc
import software.bernie.geckolib.cache.`object`.BakedGeoModel

class RodRenderer(renderManager: EntityRendererProvider.Context?): GeoEntityRenderer<RodEntity>(renderManager,RodModel()) {
    override fun preRender(
        poseStack: PoseStack?,
        animatable: RodEntity?,
        model: BakedGeoModel?,
        bufferSource: MultiBufferSource?,
        buffer: VertexConsumer?,
        isReRender: Boolean,
        partialTick: Float,
        packedLight: Int,
        packedOverlay: Int,
        colour: Int
    ) {
        // 应用遗传学大小：长度影响Y轴，宽度影响X/Z轴
        if (animatable != null) {
            val lengthScale = 1.0f + animatable.getLengthBonus()
            val widthScale = 1.0f + animatable.getWidthBonus()
            poseStack?.scale(widthScale, lengthScale, widthScale)
        }

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
    override fun getModelResource(animatable: RodEntity): ResourceLocation {
        return rodId("geo/entity/rod.geo.json")
    }

    override fun getTextureResource(animatable: RodEntity): ResourceLocation {
        return rodId("textures/entity/rod.png")
    }

    override fun getAnimationResource(animatable: RodEntity): ResourceLocation {
        return toNekoLoc("animations/neko/common.animation.json")
    }

}