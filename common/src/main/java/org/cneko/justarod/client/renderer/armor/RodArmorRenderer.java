package org.cneko.justarod.client.renderer.armor;

import com.geckolib.model.DefaultedItemGeoModel;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.specialty.DyeableGeoArmorRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.cneko.justarod.item.armor.RodArmorItem;
import org.cneko.toneko.common.mod.client.renderers.TonekoHumanoidGeoState;
import org.cneko.toneko.common.mod.misc.ToNekoEnchantments;
import org.cneko.toneko.common.mod.util.EnchantmentUtil;

import static org.cneko.justarod.Justarod.MODID;

/**
 * 假yangju
 * 26.x / GeckoLib 5.5 迁移说明：
 * 旧版基于 preRender/currentStack 的直接绘制管线已被提交式渲染状态管线取代
 * （参考 toNeko 的 NekoArmorRenderer）。
 * 物品堆与穿戴者通过 DataTicket 存入渲染状态，「反转」附魔的 180° 翻转
 * 改在 firePreRenderEvent 中应用。
 */
public class RodArmorRenderer<T extends RodArmorItem<T>> extends DyeableGeoArmorRenderer<T, TonekoHumanoidGeoState> {
    /** 当前渲染的物品堆（capture 阶段写入渲染状态） */
    public static final com.geckolib.constant.dataticket.DataTicket<ItemStack> ITEM_STACK =
            com.geckolib.constant.dataticket.DataTicket.create("justarod:armor_item_stack", ItemStack.class);
    /** 当前穿戴者实体（capture 阶段写入渲染状态） */
    public static final com.geckolib.constant.dataticket.DataTicket<Entity> WEARER_ENTITY =
            com.geckolib.constant.dataticket.DataTicket.create("justarod:armor_wearer", Entity.class);

    public RodArmorRenderer(String id) {
        super(new DefaultedItemGeoModel<>(Identifier.fromNamespaceAndPath(MODID, "armor/" + id)));
    }

    @Override
    public TonekoHumanoidGeoState createRenderState(T item,
            com.geckolib.renderer.GeoArmorRenderer.RenderData data) {
        return new TonekoHumanoidGeoState();
    }

    @Override
    public void captureDefaultRenderState(T item, com.geckolib.renderer.GeoArmorRenderer.RenderData data,
                                          TonekoHumanoidGeoState state, float partialTick) {
        super.captureDefaultRenderState(item, data, state, partialTick);
        state.addGeckolibData(ITEM_STACK, data.itemStack());
        state.addGeckolibData(WEARER_ENTITY, data.entity());
    }

    @Override
    protected boolean isBoneDyeable(com.geckolib.cache.model.GeoBone bone) {
        return false; // 该护甲不支持染色
    }

    @Override
    protected int getColorForBone(TonekoHumanoidGeoState state, com.geckolib.cache.model.GeoBone bone, int baseColor) {
        return baseColor;
    }

    @Override
    public boolean firePreRenderEvent(RenderPassInfo<TonekoHumanoidGeoState> info, SubmitNodeCollector collector) {
        boolean proceed = super.firePreRenderEvent(info, collector);
        if (proceed) {
            PoseStack poseStack = info.poseStack();
            ItemStack stack = info.renderState().getOrDefaultGeckolibData(ITEM_STACK, ItemStack.EMPTY);

            // 如果有反转附魔：旋转180度并位移
            if (!stack.isEmpty() && EnchantmentUtil.hasEnchantment(ToNekoEnchantments.REVERSION.identifier(), stack)) {
                poseStack.mulPose(Axis.XN.rotationDegrees(180.0F));
                poseStack.translate(0.0, -1.5, -0.0625);
            }
        }
        return proceed;
    }
}
