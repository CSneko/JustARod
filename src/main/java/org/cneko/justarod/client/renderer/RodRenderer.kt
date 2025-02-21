package org.cneko.justarod.client.renderer

import net.minecraft.client.render.entity.EntityRendererFactory
import net.minecraft.util.Identifier
import org.cneko.justarod.entity.RodEntity
import software.bernie.geckolib.model.GeoModel
import software.bernie.geckolib.renderer.GeoEntityRenderer
import org.cneko.justarod.JRUtil.Companion.rodId
class RodRenderer(renderManager: EntityRendererFactory.Context?): GeoEntityRenderer<RodEntity>(renderManager,RodModel()) {


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