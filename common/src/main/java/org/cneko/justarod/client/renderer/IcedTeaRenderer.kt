package org.cneko.justarod.client.renderer

import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.resources.Identifier
import org.cneko.justarod.JRUtil.Companion.rodId
import org.cneko.justarod.entity.IcedTeaProjectileEntity
import com.geckolib.model.GeoModel
import com.geckolib.renderer.GeoEntityRenderer
import com.geckolib.renderer.base.GeoRenderState

// Man! What can I say?
// 26.x：GeoEntityRenderer 需要 2 个类型参数（实体 + 渲染状态），模型资源方法参数改为 GeoRenderState
class IcedTeaRenderer(renderManager: EntityRendererProvider.Context) :
    GeoEntityRenderer<IcedTeaProjectileEntity, JREntityGeoState>(renderManager, RodModel()) {
    class RodModel : GeoModel<IcedTeaProjectileEntity>() {
        override fun getModelResource(renderState: GeoRenderState): Identifier {
            return rodId("geo/entity/iced_tea.geo.json")
        }

        override fun getTextureResource(renderState: GeoRenderState): Identifier {
            return rodId("textures/entity/iced_tea.png")
        }

        override fun getAnimationResource(animatable: IcedTeaProjectileEntity): Identifier {
            return rodId("animations/entity/rod.animation.json")
        }
    }
}
