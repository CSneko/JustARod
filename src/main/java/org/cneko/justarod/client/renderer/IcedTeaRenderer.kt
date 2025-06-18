package org.cneko.justarod.client.renderer

import net.minecraft.client.render.entity.EntityRendererFactory
import net.minecraft.util.Identifier
import org.cneko.justarod.JRUtil.Companion.rodId
import org.cneko.justarod.entity.IcedTeaProjectileEntity
import software.bernie.geckolib.model.GeoModel
import software.bernie.geckolib.renderer.GeoEntityRenderer

// Man! What can I say?
class IcedTeaRenderer(renderManager: EntityRendererFactory.Context?): GeoEntityRenderer<IcedTeaProjectileEntity>(renderManager,RodModel()) {
    class RodModel : GeoModel<IcedTeaProjectileEntity>() {
        override fun getModelResource(animatable: IcedTeaProjectileEntity): Identifier {
            return rodId("geo/entity/iced_tea.geo.json")
        }

        override fun getTextureResource(animatable: IcedTeaProjectileEntity): Identifier {
            return rodId("textures/entity/iced_tea.png")
        }

        override fun getAnimationResource(animatable: IcedTeaProjectileEntity): Identifier {
            return rodId("animations/entity/rod.animation.json")
        }

    }
}