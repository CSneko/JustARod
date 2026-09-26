package org.cneko.justarod.item.slime;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * 分解催化物（岩浆膏 + 虚弱药水 + 金苹果合成）。
 *
 * <p>喂食逻辑写在 {@link org.cneko.justarod.entity.slime.TamedSlimeEntity#mobInteract} 里：
 * 26.x 的 {@code Item#interactLivingEntity} 只在命名牌/刷怪蛋这类「重要交互」上被调用
 * （见 {@code Mob#checkAndHandleImportantInteractions}），自定义物品对它右键根本不会触发，
 * 所以本类只负责物品本身的展示。
 */
public class DecomposeCatalystItem extends Item {

    public DecomposeCatalystItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> consumer, TooltipFlag flag) {
        consumer.accept(Component.translatable("item.justarod.decompose_catalyst.desc").withStyle(ChatFormatting.GRAY));
    }
}
