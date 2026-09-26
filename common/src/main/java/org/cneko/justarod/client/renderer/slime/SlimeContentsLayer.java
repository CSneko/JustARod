package org.cneko.justarod.client.renderer.slime;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.cneko.justarod.entity.slime.TamedSlimeEntity;

import java.util.List;

/**
 * 体内内容渲染层：把史莱姆体内的**实体与物品**画在壳里。
 *
 * <p>为什么内容必须由这一层来画，而不是靠原版渲染乘客：
 * 外壳和内核都是半透明的，且都在**实体主渲染阶段**提交，于是「主阶段画的内容」会被
 * 壳 + 核叠两层半透明色盖住（0.32 × 0.45 ≈ 只有 ~18% 透出来，肉眼就是什么都看不见，
 * 只剩一层壳）。图层是在模型之后提交的，画在这里才会真正显示出来。
 * 原版那次乘客渲染由 {@code SlimePassengerRenderMixin} 关掉，因此不会出现两个分身。
 *
 * <p>坐标约定（26.x 实体模型，`javap` + `dumpSlimeModel` 核对）：
 * 模型空间 **16 像素 = 1 格**、**+Y 朝下**，外壳是 8 像素立方体（模型空间 0.5 格），
 * 渲染器再乘 {@code blockSize * 2} → 1 模型单位 = 2 × blockSize 世界格。
 * 外壳在模型空间里位于 {@code y = 1.0..1.5}，所以体内空腔中心是 {@code (0, 1.25, 0)}。
 *
 * <p><b>缩放/摆放规则（第九轮 + 第十轮修正）</b>：默认**原尺寸**——内容按
 * {@link SlimeContentSizing} 量出自己「正常画出来多大」，然后
 * {@code scale = min(1 / (2 × 外壳边长), ENTITY_FIT / (2 × 内容数 × 最长边))}：
 *
 * <ul>
 *   <li>第一项是「原尺寸」：图层→世界的倍率正好是 {@code 2 × blockSize}（渲染器的缩放），
 *       乘上 {@code 1 / (2 × blockSize)} 刚好抵消，实体在体内就是它平时的大小；</li>
 *   <li>第二项只在**装不下**时才生效：多个内容平分外壳、每个最多占 {@link #ENTITY_FIT} 份，
 *       所以「铁傀儡塞进 T3 壳」会被缩，而「美西螈/鸡/Rod 塞进 T5 壳」保持原样、**不会被放大**
 *       （第十轮修的正是这个：早期版本按「撑到外壳的 60%」缩放，小实体反而被放大好几倍）。</li>
 * </ul>
 *
 * <p>内容的**中心**落在空腔中心（实体是「从脚往上长」的，所以要先下压半个身位），
 * 水平/竖直的散布半径再按内容自身尺寸夹住，内容不会从壳里捅出去。
 */
public class SlimeContentsLayer extends RenderLayer<SlimeRenderState, JRTamedSlimeModel> {

    /** 外壳边长（模型空间，8 像素 = 0.5 格） */
    private static final double SHELL = 0.5D;
    /** 空腔中心（模型空间，格） */
    private static final double CAVITY_Y = 1.25D;
    /** 空腔可用半径（模型空间，格）：内核只有 0.375 格见方，留出边距 */
    private static final double CAVITY_RADIUS = 0.09D;
    /** 物品图标缩放（模型空间） */
    private static final float ITEM_SCALE = 0.1F;
    /** 内容最多占外壳边长的比例（只在「装不下」时才缩；留一点余量免得贴着壳壁） */
    private static final double ENTITY_FIT = 0.9D;
    /** 体内实体数量上限：避免超大史莱姆渲染爆炸 */
    private static final int MAX_CONTAINED_ENTITIES = 12;
    /** 黄金角（弧度）：多个内容按它散布，避免重叠 */
    private static final double GOLDEN_ANGLE = 2.399963D;

    private final ItemModelResolver itemModelResolver;

    public SlimeContentsLayer(RenderLayerParent<SlimeRenderState, JRTamedSlimeModel> parent,
                              ItemModelResolver itemModelResolver) {
        super(parent);
        this.itemModelResolver = itemModelResolver;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                       SlimeRenderState state, float limbSwing, float limbSwingAmount) {
        TamedSlimeEntity slime = state.slime;
        if (slime == null) return;

        if (state.isInvisible) return;   // 隐身的史莱姆不该透出体内内容

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);

        // ===== 1. 体内实体（真实乘客，按原尺寸摆进空腔，装不下才缩）=====
        List<Entity> passengers = slime.getPassengers();
        Entity cameraEntity = minecraft.getCameraEntity();
        boolean firstPerson = minecraft.options.getCameraType().isFirstPerson();
        // 多个内容平分外壳：每个最多占 ENTITY_FIT / 内容数
        int contentCount = 0;
        for (Entity passenger : passengers) {
            if (contentCount >= MAX_CONTAINED_ENTITIES) break;
            if (firstPerson && passenger == cameraEntity) continue;
            contentCount++;
        }
        contentCount = Math.max(1, contentCount);
        // 外壳边长（格）：渲染器把它乘了 blockSize * 2，所以「原尺寸」= 1 / (2 * blockSize)
        double blockSize = Math.max(0.2D, state.blockSize);
        // 吞噬技能的放大倍数：渲染器在这次渲染里额外乘了它，所以这里必须除回去。
        // 除回去的净效果是「内容跟着壳一起放大」——壳张开 4 倍把生物吸进来时，
        // 体内的内容也同步变大，比例始终和自己平时一样。
        double devourScale = Math.max(1.0D, state.devourScale);

        int index = 0;
        for (Entity passenger : passengers) {
            if (index >= MAX_CONTAINED_ENTITIES) break;
            // 第一人称下不画自己：相机就在自己脑袋里，画出来只会看到一圈内壁
            // （第三人称照常画，主人在外面能看见体内的自己）
            if (firstPerson && passenger == cameraEntity) continue;

            EntityRenderState containedState = dispatcher.extractEntity(passenger, partialTick);
            // 体内不画浮空名字/计分板，也避免把 null 相机状态传进 submitNameTag
            containedState.nameTag = null;
            containedState.scoreText = null;

            EntityRenderer<?, ? super EntityRenderState> renderer = dispatcher.getRenderer(containedState);
            if (renderer == null) continue;

            // 量「这个实体正常画出来多大」：模型包围盒（含实体 scale 属性与渲染器自己的缩放），
            // 量不到就退回碰撞箱。按**最长的那条边**算，而不是只按高度——
            // 美西螈就是被高度坑的（碰撞箱 0.42 高、模型却有 1.69 格长）。
            SlimeContentSizing.RenderedSize size = SlimeContentSizing.measure(renderer, containedState, passenger);
            double maxDimension = size.maxDimension();

            // ① 原尺寸：图层 → 世界的倍率是 2 * blockSize * devourScale，
            //    乘 1/(...) 正好抵消，实体在体内就是它平时的大小（**绝不放大**）。
            double naturalScale = 1.0D / (2.0D * blockSize * devourScale);
            // ② 装不下才缩：多个内容均分外壳，每个最多占 ENTITY_FIT 份。
            double fitScale = ENTITY_FIT / (2.0D * contentCount * maxDimension * devourScale);
            float scale = (float) Math.min(naturalScale, fitScale);

            double contentWidth = size.maxHorizontal() * scale;    // 缩放后（模型空间）
            double contentHeight = size.height() * scale;
            // 实体是「从脚往上长」的：把它的中心压到空腔中心上
            double centerY = CAVITY_Y + size.bottomOffset() * scale + contentHeight * 0.5D;

            double[] offset = innerOffset(index++, contentWidth, centerY, contentHeight);

            poseStack.pushPose();
            poseStack.translate(offset[0], offset[1], offset[2]);
            // 图层空间是「+Y 朝下」的模型空间（外壳 Rz(180) 之后），而实体渲染器假设 +Y 朝上
            // 并且自己还会再翻一次 → 这里先补一个 Rz(180) 抵消，模型才是正的（否则整个人是倒的）
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            poseStack.scale(scale, scale, scale);
            submitContained(renderer, containedState, poseStack, collector);
            poseStack.popPose();
        }

        // ===== 2. 体内物品（服务端推来的图标列表；客户端自己的 storage 永远是空的）=====
        List<ItemStack> items = SlimeClientContents.itemsFor(slime.getId());
        int slot = 0;
        for (ItemStack stack : items) {
            if (stack.isEmpty()) continue;

            double[] offset = innerOffset(slot++, ITEM_SCALE, CAVITY_Y, ITEM_SCALE);
            ItemStackRenderState itemState = new ItemStackRenderState();
            this.itemModelResolver.updateForNonLiving(itemState, stack, ItemDisplayContext.FIXED, slime);

            poseStack.pushPose();
            poseStack.translate(offset[0], offset[1], offset[2]);
            // 物品模型是 +Y 朝上画的，这里用 Rx(180)（正交旋转，不会镜像）把它摆正
            poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
            itemState.submit(poseStack, collector, lightCoords, 0, 0);
            poseStack.popPose();
        }
    }

    /**
     * 空腔内偏移：黄金角螺旋，把内容均匀散布在内部空腔里（模型空间，+Y 朝下）。
     *
     * <p>散布半径按内容自身尺寸收紧：{@code 半径 ≤ 半壳 − 半个内容}，
     * 竖直方向同理夹住，于是**内容再大也不会顶出壳外**（螺旋只负责让多个内容不重叠）。
     *
     * @param contentWidth  内容缩放后的宽度（模型空间）
     * @param centerY       内容中心应有的 y（模型空间）；内容竖直居中后就是空腔中心附近
     * @param contentHeight 内容缩放后的高度（模型空间）
     */
    private static double[] innerOffset(int index, double contentWidth, double centerY, double contentHeight) {
        double angle = index * GOLDEN_ANGLE;
        double horizontalRoom = Math.max(0.0D, SHELL * 0.5D - contentWidth * 0.5D);
        double spiral = CAVITY_RADIUS * (0.5D + 0.5D * ((index % 3) / 2.0D));
        double radius = Math.min(spiral, horizontalRoom);
        double x = Math.cos(angle) * radius;
        double z = Math.sin(angle) * radius;

        double verticalRoom = Math.max(0.0D, SHELL * 0.5D - contentHeight * 0.5D);
        double jitter = 0.06D * (((index % 4) / 3.0D) - 0.5D);
        double y = centerY + Mth.clamp(jitter, -verticalRoom, verticalRoom);
        return new double[]{x, y, z};
    }

    /** 用被吞实体自己的渲染器把它画出来（26.x 渲染管线：RenderState + submit） */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void submitContained(EntityRenderer<?, ? super EntityRenderState> renderer, EntityRenderState state,
                                        PoseStack poseStack, SubmitNodeCollector collector) {
        ((EntityRenderer) renderer).submit(state, poseStack, collector, null);
    }
}
