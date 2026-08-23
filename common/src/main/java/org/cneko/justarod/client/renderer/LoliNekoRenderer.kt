package org.cneko.justarod.client.renderer

import net.minecraft.client.renderer.RenderType
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.EntityRendererProvider
import com.mojang.blaze3d.vertex.PoseStack
import org.cneko.justarod.entity.LoliNekoEntity
import org.cneko.toneko.common.mod.client.renderers.NekoRenderer
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.cache.`object`.GeoBone
import java.util.Optional

// 萝莉控？变态变态变态！
class LoliNekoRenderer(renderManager: EntityRendererProvider.Context) : NekoRenderer<LoliNekoEntity>(renderManager) {


    override fun actuallyRender(
        poseStack: PoseStack?,
        entity: LoliNekoEntity?,
        model: BakedGeoModel?,
        renderType: RenderType?,
        bufferSource: MultiBufferSource?,
        buffer: VertexConsumer?,
        isReRender: Boolean,
        partialTick: Float,
        packedLight: Int,
        packedOverlay: Int,
        colour: Int
    ) {
        super.actuallyRender(
            poseStack,
            entity,
            model,
            renderType,
            bufferSource,
            buffer,
            isReRender,
            partialTick,
            packedLight,
            packedOverlay,
            colour
        )


        val head: Optional<GeoBone?>? = model?.getBone("Head")

        if (head?.isPresent == true) {
            head.get().setScaleX(1.5f)
            head.get().setScaleY(1.5f)
            head.get().setScaleZ(1.5f)
        }
    }
}