package org.cneko.justarod.client.renderer

import com.geckolib.constant.dataticket.DataTicket
import com.geckolib.renderer.base.GeoRenderState
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
import org.cneko.justarod.entity.RodEntity

/**
 * 26.x GeckoLib 5.5：渲染改为提交式管线，实体渲染器必须提供
 * "R extends EntityRenderState" 且 "R implements GeoRenderState" 的状态类型。
 */
open class JREntityGeoState : net.minecraft.client.renderer.entity.state.EntityRenderState(), GeoRenderState {
    private val data: MutableMap<DataTicket<*>, Any> = Reference2ObjectOpenHashMap()

    // EntityRenderState's GeckoLib mixin supplies concrete write/has methods backed by its own map.
    // Override both as well as getDataMap so controller data is written and read from the same map.
    override fun <D : Any> addGeckolibData(dataTicket: DataTicket<D>, data: D) {
        this.data[dataTicket] = data
    }

    override fun hasGeckolibData(dataTicket: DataTicket<*>): Boolean {
        return this.data.containsKey(dataTicket)
    }

    override fun getDataMap(): MutableMap<DataTicket<*>, Any> {
        return this.data
    }

    companion object {
        /** capture 阶段写入实体引用，供模型/预渲染阶段读取 */
        val ROD_ENTITY: DataTicket<RodEntity> = DataTicket.create("justarod:rod_entity", RodEntity::class.java)
    }
}
