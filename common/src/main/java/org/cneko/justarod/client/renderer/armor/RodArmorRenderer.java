package org.cneko.justarod.client.renderer.armor;

import org.cneko.justarod.item.armor.RodArmorItem;
import org.cneko.toneko.common.mod.misc.ToNekoEnchantments;
import org.cneko.toneko.common.mod.util.EnchantmentUtil;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

import static org.cneko.justarod.Justarod.MODID;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
// 假yangju
public class RodArmorRenderer <T extends RodArmorItem<T>> extends GeoArmorRenderer<T> {
    public RodArmorRenderer(String id) {
        super(new DefaultedItemGeoModel(ResourceLocation.fromNamespaceAndPath(MODID, "armor/"+id)));
    }

    public void preRender(PoseStack poseStack, T item, BakedGeoModel model, @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        if (EnchantmentUtil.hasEnchantment(ToNekoEnchantments.REVERSION.location(), this.currentStack)) {
            poseStack.mulPose(Axis.XN.rotationDegrees(180.0F));
            poseStack.translate(0.0, -1.5, -0.0625);
        }

        super.preRender(poseStack, item, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }


    public void setItemStack(ItemStack stack) {
        this.currentStack = stack;
    }

    public void setEntity(Entity entity) {
        this.currentEntity = entity;
    }

    public void setBaseModel(HumanoidModel<?> baseModel) {
        this.baseModel = baseModel;
    }

    public void setSlot(EquipmentSlot slot) {
        this.currentSlot = slot;
    }

    public void setAnimatable(T animatable) {
        this.animatable = animatable;
    }
}
