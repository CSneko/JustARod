package org.cneko.justarod.body;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.cneko.justarod.block.JRBlocks;

/**
 * 真实雌性似蚓蛔线虫（Ascaris lumbricoides）解剖结构生成器（全自定义生物组织方块版）。
 *
 * <p>核心生物学结构（沿 X 轴从前端口唇 x=-46 延伸至尾端肛门 x=+46）：
 * <ol>
 *   <li>无体节圆柱体壁：外层角质层 + 严格沿纵轴排列的纵肌束（无环肌）+ 背/腹神经索 + 左右侧线（内嵌 H 形排泄管膜与前腹横桥）；</li>
 *   <li>假体腔（Pseudocoelom）：浅层假体腔液与半透薄膜步道，包裹内部“管中管”器官；</li>
 *   <li>完整直筒消化道：品字形三唇 → Y 形三辐射肌肉咽 → 围咽神经环 → 笔直扁平单层上皮肠道（含少量甜味食糜团）→ 尾端直肠与肛门；</li>
 *   <li>双管型雌性生殖系统：前 1/3 腹侧阴门与阴道 → Y 形分叉 → 左右平行巨大子宫（满载蛔虫卵囊块）→ 受精囊与输卵管 → 中后段假体腔内盘绕的丝状卵巢组织。</li>
 * </ol>
 */
public final class AscarisAnatomyBuilder {

    public static final int FLOOR_Y = BodyWorld.FLOOR_Y;
    public static final int CY = FLOOR_Y + 10;
    public static final int HALF_LEN = 46;
    public static final int GUT_FLOOR_Y = FLOOR_Y + 6;
    public static final int NERVE_RING_X = -31;
    public static final int EXCRETORY_BRIDGE_X = -26;

    // 4 个符合真实解剖位置的入口坐标偏移（相对 region center）
    public static final int MOUTH_DX = -38, MOUTH_DY = 7, MOUTH_DZ = 0;
    public static final int VAGINA_DX = -14, VAGINA_DY = 1, VAGINA_DZ = 0;
    public static final int ANUS_DX = 36, ANUS_DY = 7, ANUS_DZ = 0;
    public static final int ANYWHERE_DX = 0, ANYWHERE_DY = 1, ANYWHERE_DZ = 0;

    private static final int FLAGS = Block.UPDATE_CLIENTS;

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState WATER = Blocks.WATER.defaultBlockState();

    private AscarisAnatomyBuilder() {
    }

    private static BlockState cuticle() {
        return JRBlocks.ASCARIS_CUTICLE.defaultBlockState();
    }

    private static BlockState muscleX() {
        return JRBlocks.ASCARIS_MUSCLE.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
    }

    private static BlockState muscleZ() {
        return JRBlocks.ASCARIS_MUSCLE.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
    }

    private static BlockState mucosa() {
        return JRBlocks.ASCARIS_MUCOSA.defaultBlockState();
    }

    private static BlockState nerveX() {
        return JRBlocks.ASCARIS_NERVE_CORD.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
    }

    private static BlockState nerveY() {
        return JRBlocks.ASCARIS_NERVE_CORD.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
    }

    private static BlockState nerveZ() {
        return JRBlocks.ASCARIS_NERVE_CORD.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
    }

    private static BlockState excretory() {
        return JRBlocks.ASCARIS_EXCRETORY_MEMBRANE.defaultBlockState();
    }

    private static BlockState gutMembrane() {
        return JRBlocks.ASCARIS_INTESTINAL_MEMBRANE.defaultBlockState();
    }

    private static BlockState fluidMembrane() {
        return JRBlocks.ASCARIS_PSEUDOCOELOM_MEMBRANE.defaultBlockState();
    }

    private static BlockState uterineWall() {
        return JRBlocks.ASCARIS_UTERINE_WALL.defaultBlockState();
    }

    private static BlockState eggSac() {
        return JRBlocks.ASCARIS_EGG_SAC.defaultBlockState();
    }

    private static BlockState ovary() {
        return JRBlocks.ASCARIS_OVARY_TISSUE.defaultBlockState();
    }

    private static BlockState chyme() {
        return JRBlocks.ASCARIS_CHYME_BLOCK.defaultBlockState();
    }

    public static void build(ServerLevel level, long slot) {
        int cx = BodyRegionData.centerX(slot);
        int cz = BodyRegionData.centerZ(slot);

        runBlueprint(level, cx, cz, null);
    }

    /**
     * 器官温和自愈：仅修补原始解剖结构中被挖空的空气缺口，绝不触碰或覆盖玩家自行摆放的箱子、床、火把与建筑装饰！
     *
     * @param maxBlocksToHeal 单次最多修补的方块数（避免卡顿并保留渐进愈合感）
     * @return 实际愈合恢复的生物组织方块数量
     */
    public static int healDamagedAnatomy(ServerLevel level, long slot, int maxBlocksToHeal) {
        if (maxBlocksToHeal <= 0) return 0;
        int cx = BodyRegionData.centerX(slot);
        int cz = BodyRegionData.centerZ(slot);
        RepairBudget budget = new RepairBudget(maxBlocksToHeal);
        runBlueprint(level, cx, cz, budget);
        return budget.healed;
    }

    private static void runBlueprint(ServerLevel level, int cx, int cz, RepairBudget repair) {
        buildOuterBodyWallAndPseudocoelom(level, cx, cz, repair);
        buildExcretoryTransverseBridge(level, cx, cz, repair);
        buildFemaleReproductiveSystem(level, cx, cz, repair);
        buildDigestiveTractAndNerveRing(level, cx, cz, repair);
        buildGutToPseudocoelomStairs(level, cx, cz, repair);
        buildWarmSnuggleNookAndMicrovilli(level, cx, cz, repair);
        carveSafeLandingsAndSentinel(level, cx, cz, repair);
    }

    private static final class RepairBudget {
        final int max;
        int healed;

        RepairBudget(int max) {
            this.max = max;
        }
    }

    /** 根据沿虫体纵轴的 X 偏移返回当前横截面的体壁外半径（中段等粗圆筒，头尾自然渐细） */
    private static double bodyRadius(int x) {
        int ax = Math.abs(x);
        if (ax <= 36) return 16.0;
        double t = (ax - 36.0) / 10.0;
        return Math.max(5.5, 16.0 - 10.5 * t * t);
    }

    private static void set(ServerLevel level, int x, int y, int z, BlockState state, RepairBudget repair) {
        BlockPos pos = new BlockPos(x, y, z);
        if (repair == null) {
            level.setBlock(pos, state, FLAGS);
            return;
        }
        if (repair.healed >= repair.max) return;
        // 自愈模式只补回原结构中的实体生物组织方块（绝不挖空气破坏玩家箱子/装饰，也不刷免费卵囊/食糜）
        if (state.isAir() || state == WATER
                || state.is(JRBlocks.ASCARIS_EGG_SAC)
                || state.is(JRBlocks.ASCARIS_CHYME_BLOCK)) {
            return;
        }
        if (level.getBlockState(pos).isAir()) {
            level.setBlock(pos, state, Block.UPDATE_ALL);
            repair.healed++;
        }
    }

    /**
     * 1. 外体壁（角质层 + 纵肌束 + 背/腹神经索 + 左右侧线 H 形排泄纵管）与假体腔液床。
     */
    private static void buildOuterBodyWallAndPseudocoelom(ServerLevel level, int cx, int cz, RepairBudget repair) {
        BlockState cut = cuticle();
        BlockState musX = muscleX();
        BlockState muc = mucosa();
        BlockState nrvX = nerveX();
        BlockState exc = excretory();
        BlockState flMem = fluidMembrane();

        for (int x = -HALF_LEN; x <= HALF_LEN; x++) {
            double r = bodyRadius(x);
            int span = (int) Math.ceil(r) + 1;
            boolean endCap = Math.abs(x) == HALF_LEN;

            for (int dy = -span; dy <= span; dy++) {
                int y = CY + dy;
                for (int dz = -span; dz <= span; dz++) {
                    double dist = Math.hypot(dy, dz);
                    if (dist > r + 0.75) continue;

                    int wx = cx + x;
                    int wz = cz + dz;

                    if (endCap || dist >= r - 0.55) {
                        // 最外层光滑耐酸角质层（Cuticle）
                        set(level, wx, y, wz, cut, repair);
                        continue;
                    }

                    if (dist >= r - 1.95) {
                        // 内层：四条纵线 + 严格沿纵轴排列的纵肌层（无环肌、无体节）
                        if (Math.abs(dz) <= 1 && dy > 0) {
                            // 背神经线（Dorsal nerve cord）
                            set(level, wx, y, wz, dz == 0 ? nrvX : cut, repair);
                        } else if (Math.abs(dz) <= 1 && dy < 0) {
                            // 腹神经线（Ventral nerve cord）
                            set(level, wx, y, wz, dz == 0 ? nrvX : cut, repair);
                        } else if (Math.abs(dy) <= 1) {
                            // 左右侧线 + H 形排泄纵管（Lateral lines & Excretory canals）
                            set(level, wx, y, wz, dy == 0 ? exc : cut, repair);
                        } else {
                            // 纵肌束与柔韧肌膜纵棱（沿 X 方向完全连续，无假体节）
                            int sector = (int) Math.floor((Math.atan2(dy, dz) + Math.PI) / (Math.PI / 8.0));
                            BlockState muscle = (Math.floorMod(sector, 3) == 1) ? muc : musX;
                            set(level, wx, y, wz, muscle, repair);
                        }
                        continue;
                    }

                    // 内部假体腔（Pseudocoelom）
                    if (y < FLOOR_Y - 2) {
                        set(level, wx, y, wz, musX, repair);
                    } else if (y == FLOOR_Y - 2) {
                        // 底部防漏密封层
                        set(level, wx, y, wz, cut, repair);
                    } else if (y == FLOOR_Y - 1) {
                        // 高液压假体腔液层（完全封闭在角质层与底部密封层内，绝不外泄）
                        set(level, wx, y, wz, (Math.abs(x) <= 35 && Math.abs(dz) <= 12) ? WATER : muc, repair);
                    } else if (y == FLOOR_Y) {
                        // 假体腔步行底面：中央柔韧肌膜步道 + 两侧半透粉白假体腔液薄膜
                        if (Math.abs(dz) <= 2) {
                            set(level, wx, y, wz, (dz == 0 && Math.floorMod(x, 8) == 0) ? nrvX : muc, repair);
                        } else if (Math.abs(x) <= 35 && Math.abs(dz) <= 12) {
                            set(level, wx, y, wz, flMem, repair);
                        } else {
                            set(level, wx, y, wz, muc, repair);
                        }
                    } else {
                        set(level, wx, y, wz, AIR, repair);
                    }
                }
            }
        }
    }

    /**
     * 2. H 形排泄管前腹横桥（Excretory transverse bridge & ventral excretory pore）：
     * 在前段 x = -26 处沿底面横跨连接左右两条侧线排泄管，并在两侧壁上攀汇入侧线。
     */
    private static void buildExcretoryTransverseBridge(ServerLevel level, int cx, int cz, RepairBudget repair) {
        BlockState exc = excretory();
        BlockState cut = cuticle();
        int wx = cx + EXCRETORY_BRIDGE_X;
        for (int z = -14; z <= 14; z++) {
            set(level, wx, FLOOR_Y - 1, cz + z, WATER, repair);
            set(level, wx, FLOOR_Y, cz + z, exc, repair);
            set(level, wx - 1, FLOOR_Y, cz + z, cut, repair);
            set(level, wx + 1, FLOOR_Y, cz + z, cut, repair);
        }
        // 沿左右体壁向上汇入左右侧线排泄管（y = FLOOR_Y .. CY）
        for (int side : new int[] {-1, 1}) {
            for (int y = FLOOR_Y + 1; y <= CY; y++) {
                double dy = y - CY;
                double rInner = bodyRadius(EXCRETORY_BRIDGE_X) - 2.1;
                int wallZ = (int) Math.round(Math.sqrt(Math.max(1.0, rInner * rInner - dy * dy))) * side;
                set(level, wx, y, cz + wallZ, exc, repair);
            }
        }
    }

    /**
     * 3. 双管型雌性生殖系统（前 1/3 阴门/阴道 → Y 形分叉 → 左右平行双子宫[含蛔虫卵囊块] → 受精囊/输卵管 → 中后段盘绕丝状卵巢）。
     */
    private static void buildFemaleReproductiveSystem(ServerLevel level, int cx, int cz, RepairBudget repair) {
        BlockState cut = cuticle();
        BlockState musX = muscleX();
        BlockState muc = mucosa();
        BlockState uWall = uterineWall();
        BlockState gMem = gutMembrane();
        BlockState egg = eggSac();
        BlockState ov = ovary();

        // 3a. 前 1/3 腹侧阴门与肌质阴道（x ∈ [-18, -11], z ∈ [-3, +3], y ∈ [FLOOR_Y, FLOOR_Y+4]）
        for (int x = -18; x <= -11; x++) {
            for (int z = -3; z <= 3; z++) {
                for (int y = FLOOR_Y; y <= FLOOR_Y + 4; y++) {
                    boolean shell = (x == -18) || (Math.abs(z) == 3) || (y == FLOOR_Y + 4);
                    if (y == FLOOR_Y) {
                        set(level, cx + x, y, cz + z, (Math.abs(z) <= 1) ? muc : musX, repair);
                    } else if (shell) {
                        boolean window = (y == FLOOR_Y + 2 && Math.abs(z) == 3 && x > -17);
                        set(level, cx + x, y, cz + z, window ? uWall : ((x % 2 == 0) ? musX : muc), repair);
                    } else {
                        set(level, cx + x, y, cz + z, AIR, repair);
                    }
                }
            }
        }

        // 3b. Y 形双管分叉过渡室（x ∈ [-10, -7]，从中央阴道平缓分叉到左右双子宫 z = ±9）
        for (int x = -10; x <= -7; x++) {
            int spread = 3 + (x + 10) * 2;
            for (int z = -spread - 2; z <= spread + 2; z++) {
                boolean insideLeft = z <= -spread + 3 && z >= -spread - 1;
                boolean insideRight = z >= spread - 3 && z <= spread + 1;
                boolean insideCenter = (x <= -9 && Math.abs(z) <= 2);
                boolean inLumen = insideLeft || insideRight || insideCenter;
                boolean onWall = !inLumen && (Math.abs(Math.abs(z) - spread) <= 3 || (x == -10 && Math.abs(z) <= 4));
                if (!inLumen && !onWall) continue;

                set(level, cx + x, FLOOR_Y, cz + z, inLumen ? cut : muc, repair);
                for (int y = FLOOR_Y + 1; y <= FLOOR_Y + 4; y++) {
                    if (y == FLOOR_Y + 4) {
                        set(level, cx + x, y, cz + z, uWall, repair);
                    } else if (inLumen) {
                        set(level, cx + x, y, cz + z, AIR, repair);
                    } else {
                        set(level, cx + x, y, cz + z, (y == FLOOR_Y + 2) ? uWall : cut, repair);
                    }
                }
            }
        }

        // 3c. 左右平行巨大子宫（Paired Uteri: x ∈ [-6, +13], z 中心 = ±9）+ 蛔虫卵囊块
        for (int side : new int[] {-1, 1}) {
            int centerZ = side * 9;
            for (int x = -6; x <= 13; x++) {
                for (int dz = -3; dz <= 3; dz++) {
                    int wz = cz + centerZ + dz;
                    set(level, cx + x, FLOOR_Y, wz, (Math.abs(dz) <= 1) ? cut : muc, repair);
                    for (int dy = 1; dy <= 5; dy++) {
                        int wy = FLOOR_Y + dy;
                        double dist = Math.hypot(dy - 2.3, dz);
                        if (dist > 3.4) continue;
                        if (dist >= 2.35) {
                            // 子宫内侧 x ∈ [-2, +2] 留一道通往中央假体腔的观察拱门，方便访客自由探索
                            boolean sideDoor = (Math.abs(x) <= 2 && dz == -side * 3 && dy <= 3);
                            if (sideDoor) {
                                set(level, cx + x, wy, wz, AIR, repair);
                            } else {
                                BlockState wall = (dy == 2 || dy == 3) ? uWall : ((Math.floorMod(x, 3) == 0) ? muc : cut);
                                set(level, cx + x, wy, wz, wall, repair);
                            }
                        } else {
                            set(level, cx + x, wy, wz, AIR, repair);
                        }
                    }
                }

                // 子宫外侧内壁堆叠的受精蛔虫卵囊团
                if (Math.floorMod(x, 4) == 0) {
                    int eggZ = cz + centerZ + side * 2;
                    set(level, cx + x, FLOOR_Y + 1, eggZ, egg, repair);
                    set(level, cx + x, FLOOR_Y + 2, eggZ, egg, repair);
                    if (x + 1 <= 12) {
                        set(level, cx + x + 1, FLOOR_Y + 1, eggZ, egg, repair);
                    }
                }
            }

            // 3d. 受精囊（Seminal Receptacle: x = 14..15）与渐细输卵管（Oviduct: x = 16..21）
            for (int x = 14; x <= 21; x++) {
                double rad = (x <= 15) ? 3.2 : Math.max(1.8, 3.0 - (x - 15) * 0.22);
                int rInt = (int) Math.ceil(rad);
                for (int dz = -rInt; dz <= rInt; dz++) {
                    for (int dy = 0; dy <= rInt * 2; dy++) {
                        double dist = Math.hypot(dy - rad, dz);
                        if (dist > rad + 0.3) continue;
                        int wy = FLOOR_Y + dy;
                        int wz = cz + centerZ + dz;
                        if (dy == 0) {
                            set(level, cx + x, wy, wz, cut, repair);
                        } else if (dist >= rad - 0.95) {
                            set(level, cx + x, wy, wz, (x <= 15 && dy == 2) ? egg : ((x % 2 == 0) ? uWall : ov), repair);
                        } else {
                            set(level, cx + x, wy, wz, AIR, repair);
                        }
                    }
                }
            }
        }

        // 3e. 中后段假体腔内盘绕如毛线团的丝状卵巢（Coiled Thread-like Ovaries: x ∈ [21, 39]）
        for (int strand = 0; strand < 2; strand++) {
            double phaseOffset = strand * Math.PI;
            int steps = 160;
            for (int i = 0; i <= steps; i++) {
                double t = i / (double) steps;
                double ox = 21.0 + t * 17.5;
                double rWall = bodyRadius((int) Math.round(ox)) - 3.2;
                double angle = phaseOffset + t * Math.PI * 5.5;
                double radial = Math.min(rWall, 7.8 + 2.4 * Math.sin(t * Math.PI * 4.0));
                int px = cx + (int) Math.round(ox);
                int py = CY + (int) Math.round(Math.sin(angle) * radial);
                int pz = cz + (int) Math.round(Math.cos(angle) * radial);

                if (py <= FLOOR_Y + 1 || py >= CY + (int) rWall) continue;
                if (py >= GUT_FLOOR_Y - 1 && py <= GUT_FLOOR_Y + 6 && Math.abs(pz - cz) <= 5) continue;

                BlockState core = (i % 13 == 0) ? egg : ov;
                BlockState coat = (strand == 0) ? uWall : gMem;
                set(level, px, py, pz, core, repair);
                if (py + 1 < CY + (int) rWall) set(level, px, py + 1, pz, coat, repair);
                set(level, px, py, pz + (strand == 0 ? 1 : -1), coat, repair);
            }
        }
    }

    /**
     * 4. 完整“管中管”直筒消化道 + 围咽神经环（纯生物神经组织，无铃铛）：
     * - 品字形三唇（x = -45..-40）
     * - Y 形三辐射肌肉咽（x = -40..-25）
     * - 围咽神经环（x = -31，背腹神经干汇交）
     * - 笔直扁平单层上皮肠道（x = -24..+32，无胃膨大、无盘曲）
     * - 尾端直肠与肛门括约肌（x = +33..+43）
     */
    private static void buildDigestiveTractAndNerveRing(ServerLevel level, int cx, int cz, RepairBudget repair) {
        BlockState cut = cuticle();
        BlockState musX = muscleX();
        BlockState musZ = muscleZ();
        BlockState muc = mucosa();
        BlockState nrvX = nerveX();
        BlockState nrvY = nerveY();
        BlockState nrvZ = nerveZ();
        BlockState gMem = gutMembrane();
        BlockState uWall = uterineWall();
        BlockState ch = chyme();

        // 4a. 品字形三唇（Triradiate 3 Lips: 1 背唇 + 2 腹侧唇, x = -45..-40）
        for (int x = -45; x <= -40; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = GUT_FLOOR_Y + 4; y <= GUT_FLOOR_Y + 6; y++) {
                    if (Math.abs(z) == 2 && y == GUT_FLOOR_Y + 6) continue;
                    set(level, cx + x, y, cz + z, (x == -41 && z == 0) ? nrvZ : muc, repair);
                }
            }
            for (int side : new int[] {-1, 1}) {
                for (int dz = 1; dz <= 4; dz++) {
                    for (int y = GUT_FLOOR_Y - 1; y <= GUT_FLOOR_Y + 3; y++) {
                        if (dz == 4 && y == GUT_FLOOR_Y - 1) continue;
                        set(level, cx + x, y, cz + side * dz, (dz == 1 && y == GUT_FLOOR_Y + 1) ? musX : muc, repair);
                    }
                }
            }
            for (int z = -1; z <= 1; z++) {
                set(level, cx + x, GUT_FLOOR_Y, cz + z, muc, repair);
                for (int y = GUT_FLOOR_Y + 1; y <= GUT_FLOOR_Y + 3; y++) {
                    set(level, cx + x, y, cz + z, AIR, repair);
                }
            }
        }

        // 4b. Y 形三辐射肌肉咽（Triradiate Muscular Pharynx: x = -40..-25）
        for (int x = -40; x <= -25; x++) {
            boolean atNerveRing = (x == NERVE_RING_X);
            int topCeil = atNerveRing ? GUT_FLOOR_Y + 5 : GUT_FLOOR_Y + 4;
            for (int z = -4; z <= 4; z++) {
                for (int y = GUT_FLOOR_Y - 1; y <= topCeil + 1; y++) {
                    boolean inLumen = (y == GUT_FLOOR_Y + 1 && Math.abs(z) <= 1)
                            || (y == GUT_FLOOR_Y + 2 && Math.abs(z) <= 2)
                            || (y == GUT_FLOOR_Y + 3 && Math.abs(z) <= 2)
                            || (atNerveRing && y == GUT_FLOOR_Y + 4 && Math.abs(z) <= 1);
                    if (inLumen) {
                        set(level, cx + x, y, cz + z, AIR, repair);
                    } else if (y == GUT_FLOOR_Y && Math.abs(z) <= 2) {
                        set(level, cx + x, y, cz + z, (z == 0 && Math.floorMod(x, 4) == 0) ? nrvX : musX, repair);
                    } else if (Math.abs(z) <= 3 && y >= GUT_FLOOR_Y && y <= topCeil) {
                        set(level, cx + x, y, cz + z, ((x + y) % 2 == 0) ? musX : muc, repair);
                    }
                }
            }
        }

        // 4c. 围咽神经环（Circumenteric Nerve Ring）+ 背腹神经连干（纯生物神经索）
        int nwx = cx + NERVE_RING_X;
        int ringCy = GUT_FLOOR_Y + 2;
        for (int dy = -6; dy <= 6; dy++) {
            for (int dz = -6; dz <= 6; dz++) {
                double dist = Math.hypot(dy, dz);
                if (dist >= 4.2 && dist <= 5.6) {
                    set(level, nwx, ringCy + dy, cz + dz, Math.abs(dy) >= Math.abs(dz) ? nrvZ : nrvY, repair);
                }
            }
        }
        // 向上接入背神经线、向下接入腹神经线
        for (int y = ringCy + 6; y <= CY + 13; y++) {
            set(level, nwx, y, cz, nrvY, repair);
        }
        for (int y = FLOOR_Y + 1; y <= ringCy - 6; y++) {
            set(level, nwx, y, cz, nrvY, repair);
        }
        // 咽腔顶端神经环拱脊（柔和神经微光，无任何铃铛）
        set(level, nwx, GUT_FLOOR_Y + 5, cz, nrvZ, repair);

        // 4d. 笔直扁平单层上皮肠道（Straight Flattened Intestine: x = -24..+32，宽 7 高 4）
        for (int x = -24; x <= 32; x++) {
            for (int z = -4; z <= 4; z++) {
                boolean sideWall = Math.abs(z) == 4;
                BlockState gutFloor;
                if (sideWall) {
                    gutFloor = cut;
                } else if (z == 0 && Math.floorMod(x, 6) == 0) {
                    gutFloor = nrvX;
                } else {
                    gutFloor = (Math.abs(z) <= 1) ? muc : musX;
                }
                set(level, cx + x, GUT_FLOOR_Y, cz + z, gutFloor, repair);

                // 肠道腔内（y = GUT_FLOOR_Y + 1 .. +4）与半透单层柱状上皮膜壁
                for (int dy = 1; dy <= 4; dy++) {
                    int wy = GUT_FLOOR_Y + dy;
                    if (sideWall) {
                        boolean rib = Math.floorMod(x, 8) == 0;
                        set(level, cx + x, wy, cz + z, rib ? ((dy == 2) ? nrvY : cut) : gMem, repair);
                    } else {
                        set(level, cx + x, wy, cz + z, AIR, repair);
                    }
                }
                // 半透背侧肠顶膜
                boolean roofRib = Math.floorMod(x, 8) == 0;
                set(level, cx + x, GUT_FLOOR_Y + 5, cz + z, roofRib ? cut : (Math.abs(z) <= 1 ? uWall : gMem), repair);
            }
        }

        // 肠道中段尚未完全吸收的甜味食糜团（自定义生物食糜块，靠边摆放不挡路）
        set(level, cx - 6, GUT_FLOOR_Y + 1, cz - 2, ch, repair);
        set(level, cx - 6, GUT_FLOOR_Y + 1, cz + 2, ch, repair);
        set(level, cx + 10, GUT_FLOOR_Y + 1, cz + 2, ch, repair);
        set(level, cx + 10, GUT_FLOOR_Y + 1, cz - 2, ch, repair);

        // 4e. 尾端直肠与肛门括约肌（Rectum & Anus: x = +33..+43）
        for (int x = 33; x <= 43; x++) {
            int halfW = (x <= 35) ? 3 : ((x <= 39) ? 2 : 1);
            boolean sphincter = (x >= 42);
            for (int z = -halfW - 1; z <= halfW + 1; z++) {
                for (int y = GUT_FLOOR_Y; y <= GUT_FLOOR_Y + 4; y++) {
                    boolean border = sphincter || Math.abs(z) == halfW + 1 || y == GUT_FLOOR_Y + 4;
                    if (y == GUT_FLOOR_Y && !sphincter && Math.abs(z) <= halfW) {
                        set(level, cx + x, y, cz + z, (z == 0 && x % 4 == 0) ? nrvX : muc, repair);
                    } else if (border) {
                        set(level, cx + x, y, cz + z, sphincter ? musZ : ((x % 2 == 0) ? musX : muc), repair);
                    } else {
                        set(level, cx + x, y, cz + z, AIR, repair);
                    }
                }
            }
        }
    }

    /**
     * 5. 消化道与假体腔之间的柔韧黏膜阶梯（自定义阶梯方块）。
     */
    private static void buildGutToPseudocoelomStairs(ServerLevel level, int cx, int cz, RepairBudget repair) {
        BlockState muc = mucosa();
        BlockState stairWest = JRBlocks.ASCARIS_MUCOSA_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST);
        for (int y = GUT_FLOOR_Y + 1; y <= GUT_FLOOR_Y + 3; y++) {
            set(level, cx - 23, y, cz + 4, AIR, repair);
            set(level, cx - 22, y, cz + 4, AIR, repair);
        }
        set(level, cx - 23, GUT_FLOOR_Y, cz + 5, muc, repair);
        for (int i = 0; i < 6; i++) {
            int sx = cx - 22 + i;
            int sy = GUT_FLOOR_Y - i;
            set(level, sx, sy, cz + 5, stairWest, repair);
            for (int clearY = sy + 1; clearY <= sy + 3; clearY++) {
                set(level, sx, clearY, cz + 5, AIR, repair);
            }
        }

        BlockState stairEast = JRBlocks.ASCARIS_MUCOSA_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST);
        for (int y = GUT_FLOOR_Y + 1; y <= GUT_FLOOR_Y + 3; y++) {
            set(level, cx + 29, y, cz - 4, AIR, repair);
            set(level, cx + 28, y, cz - 4, AIR, repair);
        }
        set(level, cx + 29, GUT_FLOOR_Y, cz - 5, muc, repair);
        for (int i = 0; i < 6; i++) {
            int sx = cx + 28 - i;
            int sy = GUT_FLOOR_Y - i;
            set(level, sx, sy, cz - 5, stairEast, repair);
            for (int clearY = sy + 1; clearY <= sy + 3; clearY++) {
                set(level, sx, clearY, cz - 5, AIR, repair);
            }
        }
    }

    /**
     * 5.5 温馨体温暖窝 + 柔粉微绒毛生态细节：
     * - 在中央假体腔步道两侧布置温暖微光的「柔暖体腔软垫」小窝，方便玩家随时依偎休憩；
     * - 在笔直肠道顶/底边沿与暖窝、子宫拱廊点缀微摆柔粉肠绒毛，消除生硬棱角、增添毛茸茸的活体质感。
     */
    private static void buildWarmSnuggleNookAndMicrovilli(ServerLevel level, int cx, int cz, RepairBudget repair) {
        BlockState cushion = JRBlocks.ASCARIS_WARM_CUSHION.defaultBlockState();
        BlockState villiFloor = JRBlocks.ASCARIS_MICROVILLI.defaultBlockState();
        BlockState villiHang = JRBlocks.ASCARIS_MICROVILLI.defaultBlockState()
                .setValue(org.cneko.justarod.block.ascaris.AscarisMicrovilliBlock.HANGING, true);

        // 1. 中央假体腔「体温暖窝」（位于 cx + 2..+4, z = ±2..±3，不挡中央通道 z∈[-1,+1]，视线一眼可见！）
        for (int side : new int[] {-1, 1}) {
            set(level, cx + 2, FLOOR_Y + 1, cz + side * 2, cushion, repair);
            set(level, cx + 3, FLOOR_Y + 1, cz + side * 2, cushion, repair);
            // 软垫旁点缀柔粉绒毛簇与下方垂挂绒毛
            set(level, cx + 1, FLOOR_Y + 1, cz + side * 3, villiFloor, repair);
            set(level, cx + 4, FLOOR_Y + 1, cz + side * 3, villiFloor, repair);
            set(level, cx + 2, GUT_FLOOR_Y - 1, cz + side * 2, villiHang, repair);
            set(level, cx + 3, GUT_FLOOR_Y - 1, cz + side * 2, villiHang, repair);
        }

        // 2. 笔直肠道两侧与肠顶的柔粉肠绒毛（真实蛔虫肠道内壁的微绒毛刷状缘！）
        for (int x = -20; x <= 26; x += 5) {
            for (int side : new int[] {-1, 1}) {
                // 肠底边沿 z = ±3（避开食糜团位置 z = ±2）
                set(level, cx + x, GUT_FLOOR_Y + 1, cz + side * 3, villiFloor, repair);
                // 肠顶垂挂 z = ±3（交错排布，自然垂落不挡中央视线）
                set(level, cx + x + 2, GUT_FLOOR_Y + 4, cz + side * 3, villiHang, repair);
            }
        }

        // 3. 假体腔两侧与阴道口点缀柔粉绒毛，增加近景与中景层次
        for (int x : new int[] {-16, -8, 8, 16}) {
            for (int side : new int[] {-1, 1}) {
                BlockPos p = new BlockPos(cx + x, FLOOR_Y + 1, cz + side * 4);
                if (level.getBlockState(p).isAir()) {
                    set(level, p.getX(), p.getY(), p.getZ(), villiFloor, repair);
                }
            }
        }
    }

    /** 6. 校验并固定全部 4 个入口安全落点与中心基岩哨兵 */
    private static void carveSafeLandingsAndSentinel(ServerLevel level, int cx, int cz, RepairBudget repair) {
        BlockState marker = nerveX();
        int[][] landings = {
                {MOUTH_DX, FLOOR_Y + MOUTH_DY - 1, MOUTH_DZ},
                {VAGINA_DX, FLOOR_Y + VAGINA_DY - 1, VAGINA_DZ},
                {ANUS_DX, FLOOR_Y + ANUS_DY - 1, ANUS_DZ},
                {ANYWHERE_DX, FLOOR_Y + ANYWHERE_DY - 1, ANYWHERE_DZ}
        };
        for (int[] l : landings) {
            int lx = cx + l[0];
            int ly = l[1];
            int lz = cz + l[2];
            set(level, lx, ly, lz, marker, repair);
            set(level, lx, ly + 1, lz, AIR, repair);
            set(level, lx, ly + 2, lz, AIR, repair);
        }
        // 区域初始化基岩哨兵（BodyWorld 统一校验点）
        set(level, cx, FLOOR_Y - 1, cz, Blocks.BEDROCK.defaultBlockState(), repair);
    }
}
