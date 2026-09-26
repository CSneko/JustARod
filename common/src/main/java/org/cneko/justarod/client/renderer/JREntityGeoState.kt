package org.cneko.justarod.client.renderer

import com.geckolib.constant.dataticket.DataTicket
import com.geckolib.renderer.base.GeoRenderState
import org.cneko.justarod.entity.RodEntity

/**
 * 26.x GeckoLib 5.5：渲染改为提交式管线，实体渲染器必须提供
 * "R extends EntityRenderState" 且 "R implements GeoRenderState" 的状态类型。
 */
open class JREntityGeoState : net.minecraft.client.renderer.entity.state.EntityRenderState(), GeoRenderState {
    private val data: MutableMap<DataTicket<*>, Any> = HashMap()

    override fun getDataMap(): MutableMap<DataTicket<*>, Any> {
        return this.data
    }

    companion object {
        /** capture 阶段写入实体引用，供模型/预渲染阶段读取 */
        val ROD_ENTITY: DataTicket<RodEntity> = DataTicket.create("justarod:rod_entity", RodEntity::class.java)
    }
}
