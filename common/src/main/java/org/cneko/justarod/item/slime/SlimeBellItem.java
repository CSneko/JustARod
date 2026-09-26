package org.cneko.justarod.item.slime;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 史莱姆铃铛。
 *
 * <p>两个用途（与作者确认的设计）：
 * <ol>
 *   <li>对「体积最小的野生史莱姆」使用：若其累计信任值已满，消耗一个铃铛把它转化为驯服史莱姆</li>
 *   <li>对自己驯服的史莱姆使用：远程召回（可跨维度，30 秒冷却）</li>
 * </ol>
 *
 * <p>这两段逻辑分别写在 {@code SlimeInteractMixin}（野生史莱姆）与
 * {@link org.cneko.justarod.entity.slime.TamedSlimeEntity#mobInteract}（已驯服）里：
 * 26.x 的 {@code Item#interactLivingEntity} 只对命名牌/刷怪蛋生效，自定义物品右键不会触发。
 * 本类只保留信任值的读写工具与物品展示。
 */
public class SlimeBellItem extends Item {

    public SlimeBellItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> consumer, TooltipFlag flag) {
        consumer.accept(Component.translatable("item.justarod.slime_bell.desc").withStyle(ChatFormatting.GRAY));
        consumer.accept(Component.translatable("item.justarod.slime_bell.hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    /**
     * 野生史莱姆的信任值：由 {@code SlimeTrustMixin} 注入到原版史莱姆上并随存档保存
     * （26.x 移除了 Entity#getPersistentData，故走 mixin 字段）。
     */
    public static int getTrust(Entity slime) {
        if (slime instanceof Slime vanilla) {
            return ((org.cneko.justarod.mixin.SlimeTrustAccessor) vanilla).justarod$getTrust();
        }
        return 0;
    }

    public static void setTrust(Entity slime, int trust) {
        if (slime instanceof Slime vanilla) {
            ((org.cneko.justarod.mixin.SlimeTrustAccessor) vanilla).justarod$setTrust(trust);
        }
    }

    public static void addTrust(Entity slime, int amount) {
        setTrust(slime, getTrust(slime) + amount);
    }
}
