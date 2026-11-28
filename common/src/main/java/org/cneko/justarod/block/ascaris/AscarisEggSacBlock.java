package org.cneko.justarod.block.ascaris;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.cneko.justarod.body.AscarisBodyInteractions;
import org.cneko.justarod.body.BodyWorld;
import org.cneko.justarod.item.JRItems;

/**
 * 蛔虫卵囊块：
 * - 用工具挖掘可直接采集整块发光卵囊带回家装饰；
 * - 空手右键可直接剥离收获 1 粒「蛔虫卵胶囊」（并有 25% 概率附带 1 份「粉白虫蜕」），在体内双子宫中饱食后会自动再生！
 */
public class AscarisEggSacBlock extends AscarisTissueBlock {

    private static final DustParticleOptions PEARL_DUST = new DustParticleOptions(0xFFF0F6, 0.9f);

    public AscarisEggSacBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel sl) || !(player instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }
        sl.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        ItemStack capsule = new ItemStack(JRItems.Companion.getASCARIS_EGG_CAPSULE(), 1);
        if (!sp.getInventory().add(capsule)) {
            Block.popResource(sl, pos, capsule);
        }
        boolean gotMolt = sl.getRandom().nextFloat() < 0.25f;
        if (gotMolt) {
            ItemStack molt = new ItemStack(JRItems.Companion.getASCARIS_MOLT(), 1);
            if (!sp.getInventory().add(molt)) {
                Block.popResource(sl, pos, molt);
            }
        }
        sl.playSound(null, pos, SoundEvents.SLIME_SQUISH_SMALL, SoundSource.BLOCKS, 0.7f, 1.25f);
        sl.sendParticles(PEARL_DUST, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.03);
        sp.sendOverlayMessage(Component.literal(gotMolt
                ? "§d从蛔虫卵囊中采集到了 1 粒蛔虫卵胶囊和 1 份粉白虫蜕~"
                : "§d从蛔虫卵囊中小心取出了一粒温热的蛔虫卵胶囊~"));
        if (level.dimension().equals(BodyWorld.DIMENSION)) {
            AscarisBodyInteractions.onEggSacHarvestedInside(sp);
        }
        return InteractionResult.SUCCESS;
    }
}
