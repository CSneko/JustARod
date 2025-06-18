package org.cneko.justarod.client.renderer.armor;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.cneko.justarod.item.armor.RodArmorItem;
import org.cneko.toneko.common.mod.misc.ToNekoEnchantments;
import org.cneko.toneko.common.mod.util.EnchantmentUtil;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

import static org.cneko.justarod.Justarod.MODID;
// 假yangju
public class RodArmorRenderer <T extends RodArmorItem<T>> extends GeoArmorRenderer<T> {
    public RodArmorRenderer(String id) {
        super(new DefaultedItemGeoModel(Identifier.of(MODID, "armor/"+id)));
    }

    public void preRender(MatrixStack poseStack, T item, BakedGeoModel model, @Nullable VertexConsumerProvider bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        if (EnchantmentUtil.hasEnchantment(ToNekoEnchantments.REVERSION.getValue(), this.currentStack)) {
            poseStack.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(180.0F));
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

    public void setBaseModel(BipedEntityModel<?> baseModel) {
        this.baseModel = baseModel;
    }

    public void setSlot(EquipmentSlot slot) {
        this.currentSlot = slot;
    }

    public void setAnimatable(T animatable) {
        this.animatable = animatable;
    }
}
