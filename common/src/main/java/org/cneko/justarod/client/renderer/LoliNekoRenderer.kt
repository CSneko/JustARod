package org.cneko.justarod.client.renderer

import net.minecraft.client.renderer.entity.EntityRendererProvider
import org.cneko.justarod.entity.LoliNekoEntity
import org.cneko.toneko.common.mod.client.renderers.NekoRenderer
import com.geckolib.renderer.base.BoneSnapshots
import com.geckolib.renderer.base.RenderPassInfo

// 萝莉控？变态变态变态！
// 26.x GeckoLib 5.5：旧 actuallyRender(...) 直接绘制挂钩已被提交式渲染管线取代。
// 头部放大 1.5 倍的效果改为在 applyAnimationControllers 中缩放 Head 骨骼快照。
class LoliNekoRenderer(renderManager: EntityRendererProvider.Context) : NekoRenderer<LoliNekoEntity>(renderManager) {
    override fun applyAnimationControllers(info: RenderPassInfo<org.cneko.toneko.common.mod.client.renderers.TonekoLivingEntityGeoState>, snapshots: BoneSnapshots) {
        super.applyAnimationControllers(info, snapshots)
        snapshots.ifPresent("Head") { head ->
            head.setScale(1.5f, 1.5f, 1.5f)
        }
    }
}
