package org.cneko.justarod.entity.slime;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * 驯服史莱姆的体积刻度。
 *
 * <p>刻度采用「连续整数指数」设计：level 为整数（0..5），
 * 碰撞箱边长（格） = 2^(level/2)，内部容量（组） = 4 * 2^(3*level/2)。
 *
 * <pre>
 * level | 理论边长 | 实际边长(格) | 内部空间(格) | 容量(组) | 容量(格) | 最大生命
 *   0   |  1.000   |    1.0       |      1       |     4    |     4    |   20
 *   1   |  1.414   |    1.5       |      3       |    11    |    11    |   30
 *   2   |  2.000   |    2.0       |      8       |    32    |    32    |   40
 *   3   |  2.828   |    3.0       |     27       |    90    |    90    |   50
 *   4   |  4.000   |    4.0       |     64       |   256    |   256    |   60
 *   5   |  5.657   |    5.5       |    166       |   724    |   724    |   70
 * </pre>
 *
 * <p>注意：容量数值与「内部空间格数」在本模组中被刻意取成同一套数字
 * （见 {@link #capacity} 与 {@link #internalSpace}），这样「1 格空间 = 1 组物品」，
 * 实体按碰撞箱体积占用同一池空间，规则简单且与设计稿一致。
 */
@Getter
@Accessors(fluent = true)
public enum SlimeTier {
    T0(0, 1.0F, 1, 4, 20, 0),
    T1(1, 1.45F, 3, 11, 30, 5),
    T2(2, 2.0F, 8, 32, 40, 12),
    T3(3, 2.85F, 27, 90, 50, 30),
    T4(4, 4.0F, 64, 256, 60, 80),
    T5(5, 5.5F, 166, 724, 70, 200);

    /** 最大等级（与设计确认：默认上限 level 5） */
    public static final int MAX_LEVEL = 5;

    /** 能否收纳实体所需的最小等级（边长 > 2 格） */
    public static final int MIN_SWALLOW_LEVEL = 3;

    private final int level;
    /** 碰撞箱边长，同时也是内部立方体边长（单位：格） */
    private final float blockSize;
    /** 内部空间（立方格数取整），实体与物品共用此空间池 */
    private final int internalSpace;
    /** 物品容量（组数） */
    private final int capacity;
    /** 最大生命值 = 20 + level * 10 */
    private final float maxHealth;
    /** 升到本等级所需的内容量（格）；T0 为 0（初始等级） */
    private final int levelCost;

    SlimeTier(int level, float blockSize, int internalSpace, int capacity, float maxHealth, int levelCost) {
        this.level = level;
        this.blockSize = blockSize;
        this.internalSpace = internalSpace;
        this.capacity = capacity;
        this.maxHealth = maxHealth;
        this.levelCost = levelCost;
    }

    /** 由等级取刻度，自动夹在 [0, MAX_LEVEL] */
    public static SlimeTier of(int level) {
        if (level <= 0) return T0;
        if (level >= MAX_LEVEL) return T5;
        return VALUES[level];
    }

    private static final SlimeTier[] VALUES = {T0, T1, T2, T3, T4, T5};

    /**
     * 实体按碰撞箱占用的内部空间（格）。
     * 用「半格向上取整」计量，这样鸡（0.4×0.7）也会占 1 格，而不是被算成 0。
     */
    public static int volumeOf(Entity entity) {
        AABB box = entity.getBoundingBox();
        int w = halfUp(box.getXsize());
        int h = halfUp(box.getYsize());
        int d = halfUp(box.getZsize());
        return Math.max(1, w * h * d);
    }

    private static int halfUp(double size) {
        return Math.max(1, (int) Math.ceil(size * 2.0D));
    }

    /** 尺寸（格）→ 体积（格），用于反推 */
    public static int volumeOf(double xSize, double ySize, double zSize) {
        return Math.max(1, halfUp(xSize) * halfUp(ySize) * halfUp(zSize));
    }

    /** 该等级是否已能收纳实体（边长 > 2 格） */
    public boolean canSwallowEntities() {
        return this.level >= MIN_SWALLOW_LEVEL;
    }

    /** 是否已满级 */
    public boolean isMax() {
        return this.level >= MAX_LEVEL;
    }

    /** 敌人被分解后转化为的 XP（满级后内容量的去处） */
    public int xpFor(int contentUnits) {
        return Math.max(0, contentUnits / 4);
    }
}
