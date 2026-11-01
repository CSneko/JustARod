package org.cneko.justarod.client.renderer

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.block.BlockModelRenderState
import net.minecraft.client.renderer.block.BlockModelResolver
import net.minecraft.client.renderer.block.model.BlockDisplayContext
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState
import net.minecraft.client.renderer.feature.ModelFeatureRenderer
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.LightCoordsUtil
import net.minecraft.world.level.Level
import net.minecraft.world.level.LightLayer
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.cneko.justarod.block.EndRodTrapBlockEntity

/** 末地烛陷阱的渲染状态：客户端要知道它伪装成了什么方块。 */
class EndRodTrapRenderState : BlockEntityRenderState() {
    var disguise: BlockState? = null
    val model: BlockModelRenderState = BlockModelRenderState()
}

/**
 * 伪装方块渲染：
 * 27.x 的渲染管线里没有 `renderSingleBlock` 了，这里用 [BlockModelResolver] 把伪装方块的
 * 模型解析进 [BlockModelRenderState]，再提交给 [SubmitNodeCollector]。
 * 没伪装时什么都不画——本体用朝上的发射器模型。
 */
class EndRodTrapRenderer(context: BlockEntityRendererProvider.Context) :
    BlockEntityRenderer<EndRodTrapBlockEntity, EndRodTrapRenderState> {

    private val blockModelResolver: BlockModelResolver = context.blockModelResolver()

    override fun createRenderState(): EndRodTrapRenderState = EndRodTrapRenderState()

    override fun extractRenderState(
        blockEntity: EndRodTrapBlockEntity,
        renderState: EndRodTrapRenderState,
        partialTick: Float,
        cameraPos: Vec3,
        crumblingOverlay: ModelFeatureRenderer.CrumblingOverlay?
    ) {
        BlockEntityRenderState.extractBase(blockEntity, renderState, crumblingOverlay)
        renderState.disguise = blockEntity.disguise
        // 陷阱本体是完整的实心方块，自身位置的亮度恒为 0（光进不来）。
        // 直接用它会让伪装方块全黑，所以取六个邻居里最亮的那一份。
        val level = blockEntity.level
        val disguise = blockEntity.disguise
        renderState.lightCoords = if (level == null || disguise == null) {
            LightCoordsUtil.FULL_BRIGHT
        } else {
            neighbourLight(level, blockEntity.blockPos, disguise.lightEmission)
        }
    }

    /** 六个邻居里最亮的天空光 / 方块光，再叠上伪装方块自身的发光。 */
    private fun neighbourLight(level: Level, pos: BlockPos, emission: Int): Int {
        var blockLight = emission
        var skyLight = 0
        for (direction in Direction.values()) {
            val neighbour = pos.relative(direction)
            blockLight = maxOf(blockLight, level.getBrightness(LightLayer.BLOCK, neighbour))
            skyLight = maxOf(skyLight, level.getBrightness(LightLayer.SKY, neighbour))
        }
        return LightCoordsUtil.pack(blockLight, skyLight)
    }

    override fun submit(
        renderState: EndRodTrapRenderState,
        poseStack: PoseStack,
        collector: SubmitNodeCollector,
        cameraRenderState: CameraRenderState
    ) {
        val disguise = renderState.disguise ?: return
        blockModelResolver.update(renderState.model, disguise, BlockDisplayContext.create())
        renderState.model.submit(
            poseStack,
            collector,
            renderState.lightCoords,
            OverlayTexture.NO_OVERLAY,
            0
        )
    }
}
