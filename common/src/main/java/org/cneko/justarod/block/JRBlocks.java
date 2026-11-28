package org.cneko.justarod.block;

import static org.cneko.justarod.Justarod.MODID;

import org.cneko.justarod.JRIds;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.UntintedParticleLeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import org.cneko.justarod.block.ascaris.AscarisChymeBlock;
import org.cneko.justarod.block.ascaris.AscarisEggSacBlock;
import org.cneko.justarod.block.ascaris.AscarisExcretoryMembraneBlock;
import org.cneko.justarod.block.ascaris.AscarisMembraneBlock;
import org.cneko.justarod.block.ascaris.AscarisMicrovilliBlock;
import org.cneko.justarod.block.ascaris.AscarisMucosaStairsBlock;
import org.cneko.justarod.block.ascaris.AscarisMuscleBlock;
import org.cneko.justarod.block.ascaris.AscarisNerveCordBlock;
import org.cneko.justarod.block.ascaris.AscarisPseudocoelomMembraneBlock;
import org.cneko.justarod.block.ascaris.AscarisTissueBlock;
import org.cneko.justarod.block.ascaris.AscarisWarmCushionBlock;

public class JRBlocks {
    // 没吃过，但感觉不好吃
    // 所以猫猫一直在自娱自乐 炫压抑是吧~ -NT
    public static final Block GOLDEN_LEAVES = register(
            // 26.x：LeavesBlock 变为抽象类，使用其具体子类
            new UntintedParticleLeavesBlock(0.01f, net.minecraft.core.particles.ParticleTypes.CHERRY_LEAVES, JRIds.blockProps("golden_leaves").sound(SoundType.CHERRY_LEAVES).noCollision()),
            "golden_leaves",
            true
    );

    /** 末地烛陷阱：可装末地烛、可伪装成其它完整方块，踩上去会把烛塞进副手。 */
    public static final Block END_ROD_TRAP = register(
            new EndRodTrapBlock(),
            "end_rod_trap",
            true
    );

    // ===== 粉白蛔虫娘体内真实解剖自定义组织方块（可采集带走且具生物功能） =====

    /** 蛔虫角质层：外壁耐酸光滑的乳粉角质保护层 */
    public static final Block ASCARIS_CUTICLE = register(
            new AscarisTissueBlock(JRIds.blockProps("ascaris_cuticle")
                    .mapColor(MapColor.COLOR_PINK)
                    .strength(2.0f, 6.0f)
                    .sound(SoundType.CORAL_BLOCK)),
            "ascaris_cuticle",
            true
    );

    /** 蛔虫纵肌束：严格沿虫体纵轴排列的纵肌纤维（支持三轴朝向 + 轻度肌肉缓冲） */
    public static final Block ASCARIS_MUSCLE = register(
            new AscarisMuscleBlock(JRIds.blockProps("ascaris_muscle")
                    .mapColor(MapColor.COLOR_PINK)
                    .strength(1.2f, 3.0f)
                    .sound(SoundType.SHROOMLIGHT)),
            "ascaris_muscle",
            true
    );

    /** 蛔虫柔韧黏膜：口唇、咽壁、阴道壁与直肠内壁的柔软黏膜组织（口/肛门处可自然滑出，咽部可投喂甜食） */
    public static final Block ASCARIS_MUCOSA = register(
            new AscarisTissueBlock(JRIds.blockProps("ascaris_mucosa")
                    .mapColor(MapColor.COLOR_PINK)
                    .strength(0.9f, 2.0f)
                    .sound(SoundType.SHROOMLIGHT)),
            "ascaris_mucosa",
            true
    );

    /** 蛔虫黏膜阶梯：连通消化道与假体腔的柔韧黏膜梯道 */
    public static final Block ASCARIS_MUCOSA_STAIRS = register(
            new AscarisMucosaStairsBlock(ASCARIS_MUCOSA.defaultBlockState(), JRIds.blockProps("ascaris_mucosa_stairs")
                    .mapColor(MapColor.COLOR_PINK)
                    .strength(0.9f, 2.0f)
                    .sound(SoundType.SHROOMLIGHT)),
            "ascaris_mucosa_stairs",
            true
    );

    /** 蛔虫神经索：背/腹神经线与围咽神经环（微光 13 + 生物电红石脉冲 + 体内安抚减执念） */
    public static final Block ASCARIS_NERVE_CORD = register(
            new AscarisNerveCordBlock(JRIds.blockProps("ascaris_nerve_cord")
                    .mapColor(MapColor.GOLD)
                    .strength(1.0f, 2.5f)
                    .lightLevel(state -> state.getValue(AscarisNerveCordBlock.EXCITED) ? 15 : 13)
                    .sound(SoundType.FROGLIGHT)),
            "ascaris_nerve_cord",
            true
    );

    /** 蛔虫排泄管膜：左右侧线与 H 形排泄管横桥（半透冰青导管膜 + 右键净化排毒） */
    public static final Block ASCARIS_EXCRETORY_MEMBRANE = register(
            new AscarisExcretoryMembraneBlock(membraneProps("ascaris_excretory_membrane", MapColor.COLOR_LIGHT_BLUE, 12)),
            "ascaris_excretory_membrane",
            true
    );

    /** 蛔虫肠壁上皮膜：笔直扁平单层柱状上皮肠道的半透粉红膜壁 */
    public static final Block ASCARIS_INTESTINAL_MEMBRANE = register(
            new AscarisMembraneBlock(membraneProps("ascaris_intestinal_membrane", MapColor.COLOR_PINK, 6)),
            "ascaris_intestinal_membrane",
            true
    );

    /** 假体腔液透膜：覆盖在假体腔液层之上的粉白半透薄膜（水床缓冲免 80% 摔落伤 + 轻弹） */
    public static final Block ASCARIS_PSEUDOCOELOM_MEMBRANE = register(
            new AscarisPseudocoelomMembraneBlock(membraneProps("ascaris_pseudocoelom_membrane", MapColor.COLOR_PINK, 8)),
            "ascaris_pseudocoelom_membrane",
            true
    );

    /** 蛔虫子宫膜壁：左右双管巨大子宫与受精囊的乳白半透平滑肌膜 */
    public static final Block ASCARIS_UTERINE_WALL = register(
            new AscarisMembraneBlock(membraneProps("ascaris_uterine_wall", MapColor.SNOW, 5)),
            "ascaris_uterine_wall",
            true
    );

    /** 蛔虫卵囊块：子宫内壁堆叠的晶莹受精蛔虫卵囊团（挖掘带走装饰 / 空手右键收获蛔虫卵胶囊） */
    public static final Block ASCARIS_EGG_SAC = register(
            new AscarisEggSacBlock(JRIds.blockProps("ascaris_egg_sac")
                    .mapColor(MapColor.QUARTZ)
                    .strength(0.7f, 1.5f)
                    .lightLevel(state -> 11)
                    .sound(SoundType.FROGLIGHT)),
            "ascaris_egg_sac",
            true
    );

    /** 蛔虫丝状卵巢组织：中后段假体腔内盘绕的丝状卵巢管组织 */
    public static final Block ASCARIS_OVARY_TISSUE = register(
            new AscarisTissueBlock(JRIds.blockProps("ascaris_ovary_tissue")
                    .mapColor(MapColor.SNOW)
                    .strength(0.8f, 2.0f)
                    .lightLevel(state -> 8)
                    .sound(SoundType.SHROOMLIGHT)),
            "ascaris_ovary_tissue",
            true
    );

    /** 甜味食糜团：肠道内尚未完全吸收的温甜半流质食糜块（支持粉蜜/发光浆果/蜂蜜金苹 3 种风味，发光浆果与蜜金自带柔光） */
    public static final Block ASCARIS_CHYME_BLOCK = register(
            new AscarisChymeBlock(JRIds.blockProps("ascaris_chyme_block")
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(0.5f, 1.0f)
                    .noOcclusion()
                    .lightLevel(state -> switch (state.getValue(AscarisChymeBlock.FLAVOR)) {
                        case AscarisChymeBlock.FLAVOR_GLOW -> 12;
                        case AscarisChymeBlock.FLAVOR_HONEY_GOLD -> 10;
                        default -> 6;
                    })
                    .sound(SoundType.HONEY_BLOCK)),
            "ascaris_chyme_block",
            true
    );

    /** 柔粉肠绒毛：微摆发光的无碰撞生物绒毛/黏膜垂丝，支持地面丛生与顶壁垂挂 */
    public static final Block ASCARIS_MICROVILLI = register(
            new AscarisMicrovilliBlock(JRIds.blockProps("ascaris_microvilli")
                    .mapColor(MapColor.COLOR_PINK)
                    .strength(0.2f, 0.5f)
                    .noCollision()
                    .noOcclusion()
                    .lightLevel(state -> 5)
                    .sound(SoundType.CHERRY_LEAVES)),
            "ascaris_microvilli",
            true
    );

    /** 柔暖体腔软垫：温暖玫瑰蜜桃微光软垫，右键依偎温暖回血、清疲劳与宫寒，减免 85% 摔落伤 */
    public static final Block ASCARIS_WARM_CUSHION = register(
            new AscarisWarmCushionBlock(JRIds.blockProps("ascaris_warm_cushion")
                    .mapColor(MapColor.COLOR_PINK)
                    .strength(0.8f, 2.0f)
                    .noOcclusion()
                    .lightLevel(state -> 11)
                    .sound(SoundType.WOOL)),
            "ascaris_warm_cushion",
            true
    );

    private static BlockBehaviour.Properties membraneProps(String name, MapColor color, int light) {
        return JRIds.blockProps(name)
                .mapColor(color)
                .strength(0.6f, 1.5f)
                .noOcclusion()
                .isValidSpawn((state, level, pos, type) -> false)
                .isRedstoneConductor((state, level, pos) -> false)
                .isSuffocating((state, level, pos) -> false)
                .isViewBlocking((state, level, pos) -> false)
                .lightLevel(state -> light)
                .sound(SoundType.FROGLIGHT);
    }

    public static void init(){
    }

    public static Block register(Block block, String name, boolean shouldRegisterItem) {

        Identifier id = Identifier.fromNamespaceAndPath(MODID, name);

        if (shouldRegisterItem) {
            BlockItem blockItem = new BlockItem(block, JRIds.itemProps(name));
            Registry.register(BuiltInRegistries.ITEM, id, blockItem);
        }

        return Registry.register(BuiltInRegistries.BLOCK, id, block);
    }
}
