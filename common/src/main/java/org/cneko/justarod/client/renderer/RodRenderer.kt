package org.cneko.justarod.client.renderer

import com.geckolib.model.GeoModel
import com.geckolib.renderer.GeoEntityRenderer
import com.geckolib.renderer.base.GeoRenderState
import com.geckolib.renderer.base.RenderPassInfo
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.resources.Identifier
import org.cneko.justarod.JRUtil.Companion.rodId
import org.cneko.justarod.entity.RodEntity

class RodRenderer(renderManager: EntityRendererProvider.Context) :
    GeoEntityRenderer<RodEntity, JREntityGeoState>(renderManager, RodModel()) {

    override fun createRenderState(animatable: RodEntity, unused: Void?): JREntityGeoState {
        return JREntityGeoState()
    }

    override fun captureDefaultRenderState(animatable: RodEntity, unused: Void?, state: JREntityGeoState, partialTick: Float) {
        super.captureDefaultRenderState(animatable, unused, state, partialTick)
        // 实体引用入渲染状态，供预渲染缩放读取
        state.addGeckolibData(JREntityGeoState.ROD_ENTITY, animatable)
    }

    // 26.x GeckoLib 5.5：旧 preRender(PoseStack,...) 改为 firePreRenderEvent(RenderPassInfo, collector)
    override fun firePreRenderEvent(info: RenderPassInfo<JREntityGeoState>, collector: SubmitNodeCollector): Boolean {
        val result = super.firePreRenderEvent(info, collector)
        val entity = info.renderState().getGeckolibData(JREntityGeoState.ROD_ENTITY)
        if (entity != null) {
            // 应用遗传学大小：长度影响Y轴，宽度影响X/Z轴
            val lengthScale = 1.0f + entity.getLengthBonus()
            val widthScale = 1.0f + entity.getWidthBonus()
            info.poseStack().scale(widthScale, lengthScale, widthScale)
            if (entity.isBaby) {
                info.poseStack().scale(0.5f, 0.5f, 0.5f)
            }
        }
        return result
    }
}

class RodModel : GeoModel<RodEntity>() {
    // 26.x：getModelResource/getTextureResource 的参数改为 GeoRenderState
    override fun getModelResource(renderState: GeoRenderState): Identifier {
        return rodId("geo/entity/rod.geo.json")
    }

    override fun getTextureResource(renderState: GeoRenderState): Identifier {
        return rodId("textures/entity/rod.png")
    }

    override fun getAnimationResource(animatable: RodEntity): Identifier {
        // 26.x：toNeko 的 IdentifierUtil 改名为 ResourceLocationUtil
        return org.cneko.toneko.common.mod.util.ResourceLocationUtil.toNekoLoc("animations/neko/common.animation.json")
    }
}
