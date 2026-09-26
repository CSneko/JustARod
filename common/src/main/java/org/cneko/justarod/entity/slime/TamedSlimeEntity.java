package org.cneko.justarod.entity.slime;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.cneko.justarod.JRUtil;
import org.cneko.justarod.api.VisualSized;
import org.cneko.justarod.entity.JREntities;
import org.cneko.justarod.item.JRItems;
import org.cneko.justarod.packet.SlimeDevourStatePayload;
import org.cneko.justarod.sound.JRSounds;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * 驯服史莱姆。
 *
 * <p>设计要点（与作者确认的 24 项决策一致）：
 * <ul>
 *   <li>体积刻度见 {@link SlimeTier}，自带 size 字段只用于原版动画/粒子/挤压效果</li>
 *   <li>体内空间 = 物品格 + 实体格的共享空间池，见 {@link SlimeStorage}</li>
 *   <li>只攻击主人的攻击目标；主动吞入靠近的怪物与野生动物（不吞玩家、不吞队友宠物）</li>
 *   <li>主人空手右键可主动钻入体内并操控史莱姆移动；潜行键退出</li>
 *   <li>被吞实体无法移动与攻击，可通过挣扎（按键）或时间自动漂移逃出</li>
 *   <li>喂食分解催化物后逐个分解体内内容：先回血，满血后转成长进度，多级连升，满级转 XP</li>
 * </ul>
 */
public class TamedSlimeEntity extends Slime implements VisualSized {

    /** 同步给客户端用于渲染与 UI 的标记 */
    private static final EntityDataAccessor<Boolean> DATA_TAMED =
            SynchedEntityData.defineId(TamedSlimeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_TIER =
            SynchedEntityData.defineId(TamedSlimeEntity.class, EntityDataSerializers.INT);
    /**
     * 主人名字（同步给客户端）。
     *
     * <p><b>为什么需要它</b>：真正的归属是 {@code ownerUuid}，而它只存在字段与 NBT 里、
     * <b>从不参与实体同步</b>——26.x 的 {@code EntityDataSerializers} 连 UUID 序列化器都没有
     * （只有 {@code Optional<LivingEntity>} 之类）。于是客户端的 {@code isOwner} 恒为 false，
     * 所有「客户端先自查一下要不要发包」的地方都会静默失效：
     * 第十二轮加的 B（体内开背包）与 G（吞噬）按键就是这样「按了毫无反应」的
     * （同一类坑见 §9.14 的操控权问题，那里也是 ownerUuid 在客户端为空）。
     *
     * <p>这里同步的是**名字**而不是「是不是你」：实体同步数据是所有追踪者共享的一份，
     * 服务端写进去的「是不是你」对每个客户端含义都不同，根本没法这么用。
     * 发一个 everyone 都能看、客户端自己拿去比对的名字才是对的。
     *
     * <p>名字只用于**客户端**的 UI 与发包前的自查（安全无关紧要，且认错人只会多发一个包）；
     * 服务端仍然只用 {@link #isOwner(Player)} 按 UUID 裁决，那才是权限边界。
     */
    private static final EntityDataAccessor<String> DATA_OWNER_NAME =
            SynchedEntityData.defineId(TamedSlimeEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_DECOMPOSE_STATE =
            SynchedEntityData.defineId(TamedSlimeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_DECOMPOSE_PROGRESS =
            SynchedEntityData.defineId(TamedSlimeEntity.class, EntityDataSerializers.FLOAT);
    /**
     * 放大动画的起始时刻（{@code level.getGameTime()}）。
     * 客户端用「自己的 gameTime − 它」还原进度，所以不需要每 tick 发包。
     */
    private static final EntityDataAccessor<Long> DATA_DEVOUR_ANIM_START =
            SynchedEntityData.defineId(TamedSlimeEntity.class, EntityDataSerializers.LONG);
    /**
     * 放大动画是否正在进行。
     *
     * <p>单独用一个布尔量而不是「时间戳 == 哨兵值」：动画起点是**世界游戏时间**，
     * 那是个任意大的正数，任何整型哨兵值理论上都可能撞上真实时间，
     * 让「没在放动画」被误判成「在放动画」。分开一个标志位就没有这个隐患。
     */
    private static final EntityDataAccessor<Boolean> DATA_DEVOUR_ANIM_ACTIVE =
            SynchedEntityData.defineId(TamedSlimeEntity.class, EntityDataSerializers.BOOLEAN);
    /** 放大动画总时长（tick），客户端渲染要知道分母 */
    private static final EntityDataAccessor<Integer> DATA_DEVOUR_ANIM_TICKS =
            SynchedEntityData.defineId(TamedSlimeEntity.class, EntityDataSerializers.INT);

    /** 分解器状态 */
    public static final int DECOMPOSE_IDLE = 0;
    public static final int DECOMPOSE_RUNNING = 1;
    public static final int DECOMPOSE_SUSPENDED = 2;

    private static final Identifier HEALTH_MODIFIER_ID =
            Identifier.fromNamespaceAndPath("justarod", "slime_tier_health");

    // ==================== 驯服与指令 ====================

    /** 驯服信任阈值：累计喂食多少个粘液球 */
    public static final int TRUST_TO_TAME = 10;

    private boolean tameData = false;
    @Nullable
    private UUID ownerUuid = null;
    private String ownerName = "";
    private int trust = 0;

    /** 指令：跟随 / 待命 / 吞人 */
    private int command = COMMAND_FOLLOW;
    public static final int COMMAND_FOLLOW = 0;
    public static final int COMMAND_STAY = 1;
    public static final int COMMAND_SWALLOW = 2;

    /** 铃铛召回冷却（tick） */
    private int recallCooldown = 0;

    // ==================== 体积 / 属性 ====================

    private int tierLevel = 0;

    // ==================== 体内空间 ====================

    private final SlimeStorage storage = new SlimeStorage();

    /** 每个被吞实体对应的乘客序号；用于按 1:1 顺序更新挣扎进度 */
    private final List<UUID> passengerOrder = new ArrayList<>();

    // ==================== 分解器 ====================

    /** 剩余待执行的分解任务数（一份催化物 = 一个任务 = 分解一份内容） */
    private int decomposeTasks = 0;
    private int decomposeTimer = 0;
    private int decomposeTotal = 0;
    private int decomposeState = DECOMPOSE_IDLE;
    /** 成长进度（格）：累计到下一级所需内容量即升级 */
    private int growthProgress = 0;
    /** 吞入冷却，避免同 tick 内连续吞入刷屏 */
    private int swallowCooldown = 0;

    // ==================== 手动「吞噬」技能 ====================

    /** 技能冷却：30 秒，每头史莱姆各自记账（存档保存） */
    public static final int DEVOUR_COOLDOWN_TICKS = 20 * 30;
    /** 放大阶段：0 → {@link #DEVOUR_SCALE}，用动画总时长的一半 */
    public static final int DEVOUR_ANIM_TICKS = 18;
    /** 动画峰值时把史莱姆放大到几倍（纯视觉，碰撞箱与空间都不变） */
    public static final float DEVOUR_SCALE = 4.0F;
    /** 技能判定范围：碰撞箱外扩几格（固定值，见 {@link #devourRange()}） */
    public static final double DEVOUR_RANGE = 6.0D;
    /** 视线夹角的一半（度）：只有玩家前方 120° 锥体内的目标会被吞 */
    public static final float DEVOUR_CONE_DEGREES = 60.0F;
    /** 状态同步节流：骑乘中每几 tick 推一次「冷却 / 目标数」（动画期间每 tick 推，让放大动画跟手） */
    private static final int DEVOUR_SYNC_INTERVAL = 5;

    /** 冷却剩余 tick */
    private int devourCooldown = 0;
    /** 帧同步计时 */
    private int devourSyncTimer = 0;
    /** 上一 tick 是不是「主人正骑着」；变化的那一帧必须立刻推状态（提示要马上出现/消失） */
    private boolean devourRideSynced = false;
    /** 放大动画结束后要吞掉的目标（动画峰值时才真正执行） */
    private final List<LivingEntity> devourQueue = new ArrayList<>();

    /**
     * 潜行逃出用的「松手闩」。
     *
     * <p>主人钻出后再钻回来很方便，如果只看「潜行键是否按下」，那么**按住不放**的玩家
     * 会立刻又被弹出去（甚至来回抖动）。所以要求潜行键**先松开过一次**才允许下一次逃出。
     */
    private boolean escapeLatched = false;

    public TamedSlimeEntity(EntityType<? extends TamedSlimeEntity> type, Level level) {
        super(type, level);
        this.setSize(1, true);
        this.storage.ensureSlots(SlimeTier.T0.capacity());
        this.setPersistenceRequired();
    }

    /** 由野生史莱姆转化而来（保留血量与信任值） */
    public static TamedSlimeEntity fromWild(Slime wild, Player tamer) {
        TamedSlimeEntity tamed = new TamedSlimeEntity(JREntities.TAMED_SLIME, wild.level());
        tamed.setPos(wild.getX(), wild.getY(), wild.getZ());
        tamed.setYRot(wild.getYRot());
        tamed.setXRot(wild.getXRot());
        tamed.setHealth(Math.min(tamed.getMaxHealth(), wild.getHealth()));
        tamed.setOwner(tamer);
        tamed.setTameData(true);
        tamed.setTrust(wild instanceof TamedSlimeEntity t ? t.getTrust() : 0);
        return tamed;
    }

    public static AttributeSupplier.Builder createSlimeAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, SlimeTier.T0.maxHealth())
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    // ==================== 数据同步 ====================

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_TAMED, false);
        builder.define(DATA_TIER, 0);
        builder.define(DATA_OWNER_NAME, "");
        builder.define(DATA_DECOMPOSE_STATE, DECOMPOSE_IDLE);
        builder.define(DATA_DECOMPOSE_PROGRESS, 0.0F);
        builder.define(DATA_DEVOUR_ANIM_START, 0L);
        builder.define(DATA_DEVOUR_ANIM_ACTIVE, false);
        builder.define(DATA_DEVOUR_ANIM_TICKS, 0);
    }

    /**
     * 26.x：等级必须**从同步数据读回来**。
     *
     * <p>{@link #setTierLevel} 只往 {@code DATA_TIER} 里写，之前没有任何地方读它，
     * 于是客户端（远程联机，甚至单人存档的客户端侧实体）的 {@code tierLevel} 恒为 0：
     * <ul>
     *   <li>{@code tier() / getTierLevel()} 全错 → 渲染器按 T0 缩放（升级后看起来没变大）；</li>
     *   <li>{@code SlimeMenu / SlimeStorageContainer} 按 T0 算容量 = 4 →
     *       90 个槽位里只有前 4 格可见可点，翻页也永远只有 1 页。</li>
     * </ul>
     *
     * <p>这里只回填字段，不动尺寸/属性：实体尺寸由原版 {@code Slime#ID_SIZE} 自己同步
     * （{@code Slime#setSize} 内部就是 {@code entityData.set(ID_SIZE, ...)}），
     * 客户端由原版 {@code Slime#onSyncedDataUpdated} 负责 refreshDimensions。
     */
    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_TIER.equals(key)) {
            this.tierLevel = entityData.get(DATA_TIER);
        }
        if (DATA_OWNER_NAME.equals(key)) {
            this.ownerName = entityData.get(DATA_OWNER_NAME);
        }
    }

    public boolean isTameData() {
        return entityData.get(DATA_TAMED);
    }

    protected void setTameData(boolean value) {
        this.tameData = value;
        entityData.set(DATA_TAMED, value);
    }

    public SlimeTier tier() {
        return SlimeTier.of(tierLevel);
    }

    /**
     * 体内内容渲染按这个尺寸缩（见 {@code SlimeContentSizing}）。
     *
     * <p>本实体的碰撞箱固定 0.5 格，**外观**却由等级决定（T0 一格、T5 五点五格）：
     * 主模型只剩一张脸，真正的体积在外壳图层（{@code JRTamedSlimeShellModel}）里。
     * 所以「另一个驯服史莱姆被吞进来」时不能拿碰撞箱当渲染尺寸——那会把它放大十几倍。
     */
    @Override
    public float visualSize() {
        return this.tier().blockSize();
    }

    public int getTierLevel() {
        return tierLevel;
    }

    public SlimeStorage storage() {
        return storage;
    }

    public int getTrust() {
        return trust;
    }

    public void setTrust(int trust) {
        this.trust = Math.max(0, trust);
    }

    public void addTrust(int amount) {
        this.trust = Math.max(0, this.trust + amount);
    }

    public int getDecomposeState() {
        return entityData.get(DATA_DECOMPOSE_STATE);
    }

    public float getDecomposeProgress() {
        return entityData.get(DATA_DECOMPOSE_PROGRESS);
    }

    public int getGrowthProgress() {
        return growthProgress;
    }

    public int getCommand() {
        return command;
    }

    public void cycleCommand() {
        this.command = (this.command + 1) % 3;
    }

    public void setCommand(int command) {
        this.command = command;
    }

    public int getRecallCooldown() {
        return recallCooldown;
    }

    @Override
    public boolean isTiny() {
        return false;
    }

    @Nullable
    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwner(Player player) {
        this.ownerUuid = player.getUUID();
        this.ownerName = player.getGameProfile().name();
        // 名字要同步出去：客户端就是靠它（而不是永不参与同步的 ownerUuid）
        // 判断「这头史莱姆是不是我的」，否则按键与提示在客户端全部失效（见 DATA_OWNER_NAME）。
        this.entityData.set(DATA_OWNER_NAME, this.ownerName);
        setTameData(true);
    }

    /** 灵魂绑定判定：主人 / 自己 */
    public boolean isOwnedBy(LivingEntity entity) {
        return entity == this || (ownerUuid != null && ownerUuid.equals(entity.getUUID()));
    }

    /**
     * 这只史莱姆是不是 {@code player} 的。
     *
     * <p>服务端按 UUID 权威判定；客户端没有 {@code ownerUuid}，退化成比较
     * {@link #getOwnerName() 主人名字}与玩家档案名。名字只用于 UI 与发包前自查，
     * 权限边界始终在服务端（那里的 UUID 比对才是真的）。
     */
    public boolean isOwner(Player player) {
        if (ownerUuid != null) {
            return ownerUuid.equals(player.getUUID());
        }
        return !ownerName.isEmpty() && ownerName.equals(player.getGameProfile().name());
    }

    /**
     * 「不主动攻击」判定：自己 + 同一主人的其它驯服史莱姆。
     *
     * <p><b>主人不在其中</b>（这是刻意改的）：需求是「分解实体不再看是否是主人，主人也可以被分解」，
     * 而主人能被吞入体内是能被分解的前提。之前这里把主人也算作友军，
     * {@code findSwallowTarget} 就会永远跳过主人——于是主人一辈子进不了自己史莱姆的分解队列。
     *
     * <p>安全性由别处保证：自动吞入模式仍然**不吞玩家**（{@code findSwallowTarget} 里单独判），
     * 所以放开这里不会导致「路过的主人被自动吞掉」；要吞主人只能用下面
     * {@link #devourBatch()} 那条手动技能路径，或者主人自己主动钻进去。
     *
     * <p>刻意不做「同名即友军」的模糊匹配（命名牌可以随便改，会误伤真敌人）。
     */
    public static boolean isAllyOf(TamedSlimeEntity slime, Entity entity) {
        if (entity == slime) return true;
        if (slime.ownerUuid == null) return false;
        if (entity instanceof TamedSlimeEntity other && other.ownerUuid != null) {
            return slime.ownerUuid.equals(other.ownerUuid);
        }
        return false;
    }

    // ==================== 体积与属性 ====================

    public void setTierLevel(int level) {
        int clamped = Math.max(0, Math.min(SlimeTier.MAX_LEVEL, level));
        this.tierLevel = clamped;
        entityData.set(DATA_TIER, clamped);
        this.setSize(clamped + 1, true);
        this.refreshDimensions();
        this.applyHealthModifier();
        // 物品槽位数跟随容量变化
        this.storage.ensureSlots(SlimeTier.of(clamped).capacity());
        this.clearFire();
    }

    private void applyHealthModifier() {
        var instance = this.getAttribute(Attributes.MAX_HEALTH);
        if (instance == null) return;
        instance.removeModifier(HEALTH_MODIFIER_ID);
        instance.addOrUpdateTransientModifier(new AttributeModifier(
                HEALTH_MODIFIER_ID,
                Math.max(0.0D, tier().maxHealth() - SlimeTier.T0.maxHealth()),
                AttributeModifier.Operation.ADD_VALUE));
        if (this.getHealth() > this.getMaxHealth()) {
            this.setHealth(this.getMaxHealth());
        } else if (this.getHealth() <= 0.0F) {
            this.setHealth(1.0F);
        }
    }

    @Override
    protected float getAttackDamage() {
        var instance = this.getAttribute(Attributes.ATTACK_DAMAGE);
        return instance == null ? 2.0F : (float) instance.getValue();
    }

    // ==================== 行为（AI） ====================

    @Override
    protected void registerGoals() {
        // 注意：26.x 的 Slime 直接继承 Mob（不是 PathfinderMob），
        // MoveControl 只能设置「想去的坐标」而不会自动走路，
        // 因此跟随/近战都必须用自定义 Goal（见文件末尾）。
        this.goalSelector.addGoal(1, new SlimeFloatGoal(this));
        this.goalSelector.addGoal(2, new SlimeMeleeGoal(this, 0.55D));
        this.goalSelector.addGoal(3, new SlimeSwallowGoal(this));
        this.goalSelector.addGoal(5, new SlimeFollowOwnerGoal(this, 0.6D, 8.0F, 2.5F));
        this.goalSelector.addGoal(6, new SlimeKeepJumpingGoal(this));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));

        // 只打主人正在打的目标（用户决策），不主动索敌
        this.targetSelector.addGoal(2, new OwnerTargetGoal(this));
    }

    /** 只把主人的攻击目标当作自己的目标；主人挨打时只记仇不主动出击 */
    public static class OwnerTargetGoal extends TargetGoal {
        private final TamedSlimeEntity slime;
        private LivingEntity target;

        public OwnerTargetGoal(TamedSlimeEntity slime) {
            super(slime, false);
            this.slime = slime;
            this.setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (slime.ownerUuid == null) return false;
            Player owner = slime.level().getPlayerInAnyDimension(slime.ownerUuid);
            if (owner == null) return false;
            LivingEntity target = owner.getLastHurtMob();
            if (target == null || !target.isAlive()) return false;
            if (slime.isAllyOf(slime, target)) return false;
            this.target = target;
            return true;
        }

        @Override
        public void start() {
            slime.setTarget(this.target);
            super.start();
        }
    }

    /** 靠近并吞入附近可吞的目标（怪物 + 野生动物，避开玩家与队友宠物） */
    public static class SlimeSwallowGoal extends net.minecraft.world.entity.ai.goal.Goal {
        private final TamedSlimeEntity slime;
        private int cooldown = 0;

        public SlimeSwallowGoal(TamedSlimeEntity slime) {
            this.slime = slime;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            // 主人在体内操控时，AI 不许抢移动
            if (slime.getControllingPassenger() != null) return false;
            if (!slime.tier().canSwallowEntities()) return false;
            if (slime.swallowCooldown > 0) return false;
            LivingEntity target = slime.findSwallowTarget(6.0D);
            if (target == null) return false;
            this.target = target;
            return true;
        }

        private LivingEntity target;

        @Override
        public boolean canContinueToUse() {
            if (target == null || !target.isAlive() || target.isPassenger()) return false;
            return slime.distanceToSqr(target) > 1.2D && this.cooldown-- > 0;
        }

        @Override
        public void start() {
            this.cooldown = 200;
        }

        @Override
        public void stop() {
            this.target = null;
            slime.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (target == null) return;
            slime.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (slime.distanceToSqr(target) <= 1.8D) {
                if (slime.trySwallow(target)) {
                    this.target = null;
                    slime.getNavigation().stop();
                }
            } else {
                slime.hopToward(target.getX(), target.getY(), target.getZ(), 1.0D, true);
            }
        }
    }

    /** 浮水：照抄原版 Slime.SlimeFloatGoal 的实现（Slime 没有可复用的通用 FloatGoal） */
    public static class SlimeFloatGoal extends net.minecraft.world.entity.ai.goal.Goal {
        private final TamedSlimeEntity slime;

        public SlimeFloatGoal(TamedSlimeEntity slime) {
            this.slime = slime;
            this.setFlags(EnumSet.of(Flag.JUMP, Flag.MOVE));
            slime.getNavigation().setCanFloat(true);
        }

        @Override
        public boolean canUse() {
            return (slime.isInWater() || slime.isInLava())
                    && slime.getFluidHeight(net.minecraft.tags.FluidTags.WATER) > slime.getFluidJumpThreshold()
                    || slime.isInLava();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (slime.getRandom().nextFloat() < 0.8F) {
                slime.getJumpControl().jump();
            }
        }
    }

    /** 像原版史莱姆一样持续朝 MoveControl 的目标点跳跃前进 */
    public static class SlimeKeepJumpingGoal extends net.minecraft.world.entity.ai.goal.Goal {
        private final TamedSlimeEntity slime;

        public SlimeKeepJumpingGoal(TamedSlimeEntity slime) {
            this.slime = slime;
            this.setFlags(EnumSet.of(Flag.JUMP, Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            // 主人正坐在体内操控时不要抢移动（原版 SlimeKeepOnJumpingGoal 同样是 !isPassenger()）
            if (slime.getControllingPassenger() != null) return false;
            return !slime.getNavigation().isDone() || slime.getMoveControl().hasWanted();
        }

        @Override
        public boolean canContinueToUse() {
            // 必须也有这一条：否则「骑着的那一刻」这个 goal 不会停，
            // 它 tick 里的 setSpeed/setZza 会继续把史莱姆往前推（平移的另一个来源）。
            if (slime.getControllingPassenger() != null) return false;
            return !slime.getNavigation().isDone() || slime.getMoveControl().hasWanted();
        }

        @Override
        public void stop() {
            slime.setSpeed(0.0F);
            slime.setZza(0.0F);
        }

        @Override
        public void tick() {
            if (slime.getMoveControl().hasWanted()) {
                double speed = slime.getMoveControl().getSpeedModifier();
                slime.setSpeed((float) speed);
                slime.setZza(1.0F);
            } else {
                slime.setSpeed(0.0F);
                slime.setZza(0.0F);
            }
        }
    }

    /** 跟随主人（Slime 用 MoveControl + 跳跃移动，所以自己实现） */
    public static class SlimeFollowOwnerGoal extends net.minecraft.world.entity.ai.goal.Goal {
        private final TamedSlimeEntity slime;
        private final double speedModifier;
        private final float startDistance;
        private final float stopDistance;
        private Player owner;

        public SlimeFollowOwnerGoal(TamedSlimeEntity slime, double speedModifier, float startDistance, float stopDistance) {
            this.slime = slime;
            this.speedModifier = speedModifier;
            this.startDistance = startDistance;
            this.stopDistance = stopDistance;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (slime.getCommand() != COMMAND_FOLLOW) return false;
            if (slime.getControllingPassenger() != null) return false;
            if (slime.getOwnerUuid() == null) return false;
            this.owner = slime.level().getPlayerInAnyDimension(slime.getOwnerUuid());
            if (owner == null || owner.isSpectator()) return false;
            if (slime.isPassenger() || owner.isPassenger()) return false;
            return slime.distanceToSqr(owner) > (double) (startDistance * startDistance);
        }

        @Override
        public boolean canContinueToUse() {
            if (slime.getCommand() != COMMAND_FOLLOW) return false;
            if (slime.getControllingPassenger() != null) return false;
            if (owner == null || !owner.isAlive()) return false;
            return slime.distanceToSqr(owner) > (double) (stopDistance * stopDistance);
        }

        @Override
        public void start() {
        }

        @Override
        public void stop() {
            this.owner = null;
            slime.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (owner == null) return;
            slime.getLookControl().setLookAt(owner, 10.0F, (float) slime.getMaxHeadXRot());
            if (slime.isLeashed() || slime.isPassenger()) return;

            // 太远则直接传送（原版宠物逻辑）。门槛随体积变大：大史莱姆腿长，允许拉更开一点。
            double teleportDistance = TELEPORT_BASE_DISTANCE + slime.tier().blockSize() * TELEPORT_PER_BLOCK_SIZE;
            if (slime.distanceToSqr(owner) >= teleportDistance * teleportDistance) {
                slime.teleportTo(owner.getX(), owner.getY(), owner.getZ());
                return;
            }
            // 必须每 tick 驱动一次（史莱姆是跳跃移动，跳一下就停）
            slime.hopToward(owner.getX(), owner.getY(), owner.getZ(), speedModifier, false);
        }
    }

    /** 近战：走到目标身边并造成伤害 */
    public static class SlimeMeleeGoal extends net.minecraft.world.entity.ai.goal.Goal {
        private final TamedSlimeEntity slime;
        private final double speedModifier;
        private int attackCooldown = 0;

        public SlimeMeleeGoal(TamedSlimeEntity slime, double speedModifier) {
            this.slime = slime;
            this.speedModifier = speedModifier;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (slime.getControllingPassenger() != null) return false;
            LivingEntity target = slime.getTarget();
            return target != null && target.isAlive() && !slime.isAlliedTo(target);
        }

        @Override
        public boolean canContinueToUse() {
            if (slime.getControllingPassenger() != null) return false;
            LivingEntity target = slime.getTarget();
            return target != null && target.isAlive() && !slime.isAlliedTo(target);
        }

        @Override
        public void stop() {
            slime.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity target = slime.getTarget();
            if (target == null) return;
            slime.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (attackCooldown > 0) attackCooldown--;

            double reach = (slime.getBbWidth() * 0.5D) + 1.0D;
            if (slime.distanceToSqr(target) <= reach * reach) {
                if (attackCooldown <= 0) {
                    attackCooldown = 20;
                    slime.doHurtTarget((net.minecraft.server.level.ServerLevel) slime.level(), target);
                }
            } else {
                // 每 tick 都要驱动跳跃，不是每 10 tick 一次
                slime.hopToward(target.getX(), target.getY(), target.getZ(), speedModifier, true);
            }
        }
    }

    @Nullable
    private LivingEntity findSwallowTarget(double radius) {
        if (!this.tier().canSwallowEntities()) return null;
        if (this.swallowCooldown > 0) return null;      // 与 SlimeSwallowGoal.canUse 同一道闸
        int space = this.tier().internalSpace();
        AABB area = this.getBoundingBox().inflate(radius);
        List<LivingEntity> candidates = this.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e.isAlive() && e != this && !e.isPassenger());
        for (LivingEntity candidate : candidates) {
            if (candidate instanceof Player) continue;              // 玩家绝不自动吞
            if (isAllyOf(this, candidate)) continue;                 // 其它驯服史莱姆（主人已不算友军，见 isAllyOf）
            if (candidate instanceof TamedSlimeEntity other && other.isTameData()) continue;
            if (candidate instanceof TamableAnimal animal && animal.isTame()) continue;
            int volume = SlimeTier.volumeOf(candidate);
            if (storage.canSwallow(volume, space)) return candidate;
        }
        return null;
    }

    /**
     * 技能判定用的「朝向」。
     *
     * <p>主人骑着时用**主人的视线**，而不是史莱姆自己的朝向：被操控的史莱姆只在
     * 收到移动输入时才跟着转向（{@code travelRiddenHopping} 里按输入方向设 yaw），
     * 站着不动时它朝哪边是上一次行走留下的。玩家按 G 的直觉是「我准星指着的那一片」，
     * 用视线才算得对。没人在操控（例如只是一头跟着你的史莱姆）就退回它自己的朝向。
     */
    private Vec3 devourFacing() {
        if (this.getControllingPassenger() instanceof Player rider) {
            return rider.getViewVector(1.0F);
        }
        return this.getViewVector(1.0F);
    }

    /**
     * 技能判定范围：碰撞箱外扩几格。
     *
     * <p>当前是**固定 6 格**（按需求指定）。注意总触及半径还要加上外壳半径，
     * 所以实际够得着的距离是 {@code 6 + 边长/2}：
     *
     * <pre>
     * level | 边长(格) | 总触及半径(格)
     *   3   |   2.85   |     7.43
     *   5   |   5.50   |     8.75
     * </pre>
     *
     * <p>之前试过按体积动态（`1.0 + 0.5 × 边长` → 外扩 2.4~3.75 格），
     * 体感太短所以改回固定值并加大。如果要再调，改这两个常量即可；
     * 想恢复动态就把它写成 {@code DEVOUR_RANGE_BASE + PER_BLOCK * blockSize}。
     */
    public double devourRange() {
        return DEVOUR_RANGE;
    }

    /** 玩家前方 120° 锥体内、按现有规则「可以成为吞噬目标」的活体（不看空间够不够） */
    private List<LivingEntity> devourTargetsInCone() {
        List<LivingEntity> result = new ArrayList<>();
        if (!this.tier().canSwallowEntities()) return result;
        // 刚吞过东西的那 10 tick 内不再统计目标：否则「自动吞入」紧跟着「手动技能」，
        // 同一瞬间连吞两批，玩家会看到一次技能吞出两倍的数量。
        if (this.swallowCooldown > 0) return result;

        Entity controller = this.getControllingPassenger();
        Vec3 look = this.devourFacing();
        double cosLimit = Math.cos(Math.toRadians(DEVOUR_CONE_DEGREES));
        AABB area = this.getBoundingBox().inflate(this.devourRange());

        List<LivingEntity> candidates = this.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e.isAlive() && e != this && e != controller);
        for (LivingEntity candidate : candidates) {
            // 必须是「没被载着」的：已经骑在别的东西上、或已在体内登记的，都跳过
            if (candidate.isPassenger() || storage.containsEntity(candidate.getUUID())) continue;
            if (isAllyOf(this, candidate)) continue;                 // 同一主人的其它驯服史莱姆
            if (candidate instanceof TamedSlimeEntity other && other.isTameData()) continue;
            if (candidate instanceof TamableAnimal animal && animal.isTame()) continue;
            // 视线锥体：用「史莱姆眼睛 → 目标碰撞箱中心」的方向与朝向夹角判断
            Vec3 toTarget = candidate.position().add(0.0D, candidate.getBbHeight() * 0.5D, 0.0D)
                    .subtract(this.getEyePosition());
            if (toTarget.lengthSqr() > 1.0E-4D) {
                if (look.dot(toTarget.normalize()) < cosLimit) continue;
            }
            result.add(candidate);
        }

        // 近的先吞，手感符合直觉（也是「最多 N 个」时该优先谁的定义）
        result.sort(java.util.Comparator.comparingDouble(this::distanceToSqr));
        return result;
    }

    /**
     * 本次技能最多能吞几个：{@code min{剩余空间, 史莱姆等级}}。
     *
     * <p>抽成纯函数是为了能离线验证（{@code .tmp/jrtest/DevourRuleTest.java}）：
     * 吞入流程要真实 {@code Level} 与实体，但这条规则本身不需要。
     *
     * <p>注意这里**只**管数量上限，不管「等级够不够大能收纳实体」——
     * 后者是 {@link SlimeTier#canSwallowEntities()}（等级 ≥ 3），
     * 在 {@link #devourTargetsInCone()} 与 {@link #triggerDevour} 里各拦一道。
     * 所以等级 1/2 会算出 1/2，但技能依然吞不到东西（需求确认「严格照做」：等级 0~2 吞不到）。
     *
     * @param freeSpace 体内剩余空间（格）
     * @param tierLevel 史莱姆等级
     */
    public static int devourCapacity(int freeSpace, int tierLevel) {
        return Math.max(0, Math.min(freeSpace, tierLevel));
    }

    /**
     * 真正会被这次技能吞掉的目标：{@code min{剩余空间, 等级}} 是**数量**上限，
     * 同时每个目标仍按现有体积规则占格（玩家 16 格、鸡 2 格、牛 8 格……）。
     *
     * <p>两个上限必须同时满足，所以这里是「逐个试吞 + 扣减剩余空间」，
     * 而不是简单地取前 N 个——否则会出现「一次吞 3 只、每只单独看都装得下、
     * 合起来装不下」的账目错误。
     *
     * @return 会被吞掉的目标（已按距离从近到远排序）
     */
    public List<LivingEntity> devourBatch() {
        List<LivingEntity> result = new ArrayList<>();
        int free = storage.freeSpace(this.tier().internalSpace());
        int cap = devourCapacity(free, this.getTierLevel());
        if (cap <= 0) return result;
        int remaining = free;
        for (LivingEntity candidate : devourTargetsInCone()) {
            if (result.size() >= cap) break;
            int volume = SlimeTier.volumeOf(candidate);
            if (volume > remaining) continue;
            remaining -= volume;
            result.add(candidate);
        }
        return result;
    }

    // ==================== 吞入 / 放出 ====================

    /** 跟随时「直接传送」的基础距离（格）：level 0 用这个值 */
    public static final double TELEPORT_BASE_DISTANCE = 12.0D;
    /** 每 1 格体积额外允许的传送距离：体积越大，能拉开的距离越远 */
    public static final double TELEPORT_PER_BLOCK_SIZE = 4.0D;

    /**
     * 尝试把一个实体吞入体内（带默认音效与粒子）。
     *
     * <p>手动「吞噬」技能不走这里——它要的是自定义黏糊吞咽声，见 {@link #performSwallow}。
     */
    public boolean trySwallow(LivingEntity target) {
        if (!this.performSwallow(target)) return false;
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.SLIME_ATTACK,
                    SoundSource.NEUTRAL, 0.8F, 0.6F);
            serverLevel.sendParticles(ParticleTypes.ITEM_SLIME,
                    this.getX(), this.getY() + this.tier().blockSize() * 0.5D, this.getZ(),
                    12, 0.3D, 0.3D, 0.3D, 0.02D);
        }
        return true;
    }

    /**
     * 把实体吞进体内的纯逻辑：校验 → 挂乘客 → 记账。不含音效/粒子，
     * 这样调用方可以自己决定什么时候发声（技能是在放大动画的峰值那一刻发声的）。
     */
    private boolean performSwallow(LivingEntity target) {
        if (this.level().isClientSide()) return false;
        if (!this.tier().canSwallowEntities()) {
            return false;
        }
        if (target.isPassenger() || target == this) return false;
        if (storage.containsEntity(target.getUUID())) return false;
        int volume = SlimeTier.volumeOf(target);
        int space = this.tier().internalSpace();
        if (!storage.canSwallow(volume, space)) {
            return false;
        }
        if (!target.startRiding(this, true, true)) return false;

        storage.addEntity(target.getUUID(), volume);
        passengerOrder.add(target.getUUID());
        this.swallowCooldown = 10;
        return true;
    }

    /** 释放一个被吞实体 */
    public boolean releaseEntity(UUID uuid) {
        storage.removeEntity(uuid);
        passengerOrder.remove(uuid);
        Entity entity = findContainedEntity(uuid);
        if (entity != null) {
            entity.stopRiding();
            if (entity instanceof LivingEntity living) {
                living.setHealth(Math.max(1.0F, living.getHealth()));
            }
            // 放到史莱姆脚下，避免卡在体内
            Vec3 pos = this.position().add(0.0D, this.getBbHeight() * 0.5D, 0.0D);
            entity.teleportTo(pos.x, pos.y, pos.z);
        }
        return true;
    }

    /** 释放全部被吞实体（死亡 / 强制驱散） */
    public void releaseAllEntities() {
        for (UUID uuid : new ArrayList<>(storage.containedEntities())) {
            Entity entity = findContainedEntity(uuid);
            if (entity != null) {
                entity.stopRiding();
            }
        }
        storage.clearEntities();
        passengerOrder.clear();
    }

    /**
     * 主人按潜行键从体内钻出（**只对主人有效**）。
     *
     * <p>为什么需要它，而且只能这么做：
     * <ul>
     *   <li>主人**站在外面**时，`mobInteract` 里的 `isShiftKeyDown` 分支负责开背包；</li>
     *   <li>主人**在体内**时是乘客，右键交互整条路都不会走到 `mobInteract`
     *       （同 §9.5 / §10.1 的问题），所以「潜行钻出」必须在这里实现；</li>
     *   <li>只对主人有效：被**技能**吞进来的其他玩家要靠挣扎（R）/ 自动漂移逃出，
     *       按潜行键不该有作用，否则被吞就形同虚设。</li>
     * </ul>
     *
     * <p>潜行状态从 {@code ServerPlayer#getLastClientInput().shift()} 读——
     * 和操控用的 {@code forward/backward} 是同一份客户端输入。
     * 用 {@link #escapeLatched} 要求「先松手过一次」，避免按住潜行键反复进出。
     */
    private void tickOwnerSneakEscape() {
        if (!(this.level() instanceof ServerLevel)) return;
        if (!(this.getControllingPassenger() instanceof net.minecraft.server.level.ServerPlayer owner)) {
            this.escapeLatched = false;
            return;
        }

        boolean sneaking = owner.getLastClientInput().shift();
        if (!sneaking) {
            this.escapeLatched = false;
            return;
        }
        if (this.escapeLatched) return;
        this.escapeLatched = true;

        // 用 releaseEntity 而不是裸 stopRiding：它会一并注销账本、归还空间、
        // 并把主人放到史莱姆脚下（否则会停在体内那个乘客挂点上）。
        this.releaseEntity(owner.getUUID());
        owner.sendOverlayMessage(Component.translatable("justarod.slime.exit")
                .withStyle(ChatFormatting.GREEN));
    }

    /**
     * 是否是「已经不该留在体内」的乘客：死了 / 正在死 / 已被移除。
     *
     * <p>死亡不会自动下车（见 {@link #settleDecomposedEntity} 的说明），
     * 而死亡到复活之间这个乘客仍然挂在 {@code getPassengers()} 里。
     * 这段时间必须把它当成「不在体内」，否则会出现两个方向相反的毛病：
     * <ul>
     *   <li>{@link #enforceContainedEntities()} 会不停地把它重新按上车（复活后仍在体内）；</li>
     *   <li>它还会一直占着体内空间。</li>
     * </ul>
     */
    private static boolean isDeadPassenger(Entity entity) {
        if (entity.isRemoved()) return true;
        return entity instanceof LivingEntity living && living.isDeadOrDying();
    }

    @Nullable
    public Entity findContainedEntity(UUID uuid) {
        if (this.level() instanceof ServerLevel serverLevel) {
            return serverLevel.getEntity(uuid);
        }
        for (Entity passenger : this.getPassengers()) {
            if (passenger.getUUID().equals(uuid)) return passenger;
        }
        return null;
    }

    /** 主人主动钻进来 */
    public boolean swallowOwner(Player player) {
        if (!this.isOwner(player)) return false;
        if (player.isPassenger()) {
            player.stopRiding();
        }
        return performSwallow(player);
    }

    /**
     * 让「主人是否在体内」这件事与实际挂载关系保持一致（服务端每 tick）。
     *
     * <p>主人进来时服务端没有把他登记进 {@code storage}，于是打开背包时实体页显示
     * 「体内没有实体」——明明人就坐在里面（本轮反馈的问题）。这里改成**状态对账**：
     * 不再要求每个入口都记得登记，而是每 tick 比一次「实际挂载」与「账本」。
     *
     * <p>主人登记后是**免疫挣扎**的（见 {@code SlimeStorage#struggleImmune}）：
     * 他进来是为了操控史莱姆，不该像别的猎物那样被自动漂移甩出去。
     * 被**技能**吞进来的别的玩家不免疫，照常能挣扎逃出。
     */
    private void reconcileContainedOwner() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        if (this.ownerUuid == null) return;

        Player owner = serverLevel.getPlayerInAnyDimension(this.ownerUuid);
        // 死了/正在死的玩家不算「在体内」：死亡不会自动下车，而这里如果照旧算他在车上，
        // 账本会一直留着他、复活后也还在体内。
        boolean riding = owner != null && owner.getVehicle() == this && !isDeadPassenger(owner);
        boolean registered = owner != null && storage.containsEntity(owner.getUUID());

        if (riding && !registered) {
            int volume = SlimeTier.volumeOf(owner);
            storage.addEntity(owner.getUUID(), volume, true);
            passengerOrder.add(owner.getUUID());
            storage.bumpVersion();
        } else if (!riding && registered) {
            // 已经出来了（潜行钻出、被放出、或死/换维度）：把空间还回去
            storage.removeEntity(owner.getUUID());
            passengerOrder.remove(owner.getUUID());
            storage.bumpVersion();
        } else if (riding) {
            // 已在账本里：确保免疫标记对（老存档读回来是 false）
            if (storage.syncContained(owner.getUUID(), SlimeTier.volumeOf(owner), true)) {
                storage.bumpVersion();
            }
        }
    }

    // ==================== 手动「吞噬」技能 ====================

    /**
     * 玩家骑着史莱姆按吞噬键：把附近目标吞进来。
     *
     * <p>流程（需求确认的节奏）：<b>先把史莱姆放大到 4 倍 → 吞噬 → 再变回原来的大小</b>，
     * 所以这个方法只负责「起手」：
     * <ol>
     *   <li>校验冷却 / 等级 / 有没有目标 / 空间够不够，任意一条不过就只发提示、不进冷却；</li>
     *   <li>把目标列表暂存到 {@link #devourQueue}，开始放大动画；</li>
     *   <li>真正的吞入与音效在动画峰值执行，见 {@link #tickDevour()}。</li>
     * </ol>
     *
     * <p>这个顺序还有个副作用是好的：玩家按下去到生物被吞之间有约 0.45 秒，
     * 正好对得上「张开身子吸进去」的观感。
     *
     * @return 是否真的发动了技能（false 表示被拒，此时不进冷却）
     */
    public boolean triggerDevour(Player player) {
        if (this.level().isClientSide()) return false;
        if (!this.isOwner(player)) return false;
        if (this.isDevouring()) return false;

        if (devourCooldown > 0) {
            player.sendOverlayMessage(Component.translatable("justarod.slime.devour.cooldown",
                    devourCooldown / 20).withStyle(ChatFormatting.RED));
            return false;
        }
        if (!this.tier().canSwallowEntities()) {
            player.sendOverlayMessage(Component.translatable("justarod.slime.devour.low_level")
                    .withStyle(ChatFormatting.YELLOW));
            return false;
        }

        List<LivingEntity> batch = this.devourBatch();
        if (batch.isEmpty()) {
            // 分清三种「吞不到」，提示才有用：
            //   ① 等级上限为 0（level<3）→ 已经在上面的 canSwallowEntities 里拦掉了；
            //   ② 前方锥体里根本没有目标；
            //   ③ 有目标但体内剩余空间放不下任何一个。
            boolean anyInCone = !this.devourTargetsInCone().isEmpty();
            String key = anyInCone ? "justarod.slime.devour.no_space" : "justarod.slime.devour.no_target";
            player.sendOverlayMessage(Component.translatable(key).withStyle(ChatFormatting.YELLOW));
            return false;
        }

        this.devourQueue.clear();
        this.devourQueue.addAll(batch);
        // 用 level.getGameTime() 而不是 tickCount：gameTime 在客户端与服务端是同一个值
        // （tickCount 是实体自己的年龄，客户端实体中途加载时会偏），
        // 这样客户端能直接用「自己的 gameTime − animStart」还原动画进度。
        this.entityData.set(DATA_DEVOUR_ANIM_START, this.devourClock());
        this.entityData.set(DATA_DEVOUR_ANIM_TICKS, DEVOUR_ANIM_TICKS);
        this.entityData.set(DATA_DEVOUR_ANIM_ACTIVE, true);
        this.devourCooldown = DEVOUR_COOLDOWN_TICKS;
        this.devourSyncTimer = 0;
        return true;
    }

    /** 动画用的时钟：世界游戏时间（两端一致） */
    private long devourClock() {
        return this.level().getGameTime();
    }

    /** 放大动画是否正在进行 */
    public boolean isDevouring() {
        return this.entityData.get(DATA_DEVOUR_ANIM_ACTIVE);
    }

    /** 放大动画总时长（tick） */
    public int getDevourAnimTicks() {
        return this.entityData.get(DATA_DEVOUR_ANIM_TICKS);
    }

    /**
     * 当前应该把外观放大到几倍。1.0 = 原始大小，峰值 {@link #DEVOUR_SCALE}。
     *
     * <p>纯视觉：碰撞箱、内部空间、容量、骑乘挂点全都不动（需求确认「仅视觉效果」）。
     * 曲线分两段——前半程 1 → 4 用 ease-out（先快后慢，像被撑开），
     * 后半程 4 → 1 用 smoothstep（收口时先快后慢）。
     *
     * <p>时间基准是 {@code level.getGameTime()}：客户端与服务端同一个值，
     * 所以两端算出来的缩放完全一致，也不需要额外的同步包。
     *
     * @param partialTick 渲染插值用；不插值时传 0
     */
    public float devourScale(float partialTick) {
        if (!this.isDevouring()) return 1.0F;
        int total = this.getDevourAnimTicks();
        if (total <= 0) return 1.0F;
        float progress = (float) ((this.devourClock() + partialTick
                - this.entityData.get(DATA_DEVOUR_ANIM_START)) / (double) total);
        if (progress <= 0.0F) return 1.0F;
        if (progress >= 1.0F) return 1.0F;
        float half = 0.5F;
        if (progress < half) {
            // 0 → 1 的 ease-out
            float p = progress / half;
            float eased = 1.0F - (1.0F - p) * (1.0F - p) * (1.0F - p);
            return 1.0F + (DEVOUR_SCALE - 1.0F) * eased;
        }
        // 1 → 0 的 smoothstep
        float p = (progress - half) / half;
        float eased = p * p * (3.0F - 2.0F * p);
        return DEVOUR_SCALE - (DEVOUR_SCALE - 1.0F) * eased;
    }

    /**
     * 放大动画的状态机（服务端每 tick）。
     *
     * <p>时间线：{@code [start, start + total/2)} 放大，中点<b>执行吞噬 + 放音效</b>，
     * 之后收缩回原大小。执行的那一帧之后清掉动画标志，客户端自然回到 1 倍。
     */
    private void tickDevour() {
        if (!this.isDevouring()) return;
        int total = Math.max(1, this.getDevourAnimTicks());
        long elapsed = this.devourClock() - this.entityData.get(DATA_DEVOUR_ANIM_START);
        if (elapsed >= total / 2) {
            this.performDevourGulp();
            this.entityData.set(DATA_DEVOUR_ANIM_ACTIVE, false);
        }
    }

    /** 动画峰值：真正把目标吞进来，并播放黏糊吞咽声 */
    private void performDevourGulp() {
        if (this.level().isClientSide()) return;
        int swallowed = 0;
        for (LivingEntity target : this.devourQueue) {
            if (!target.isAlive()) continue;
            if (!this.performSwallow(target)) continue;
            swallowed++;
            if (target instanceof Player victim) {
                victim.sendOverlayMessage(Component.translatable("justarod.slime.devour.victim",
                        this.getDisplayName()).withStyle(ChatFormatting.DARK_GREEN));
            }
        }
        this.devourQueue.clear();
        if (swallowed <= 0) return;

        if (this.level() instanceof ServerLevel serverLevel) {
            // 黏糊糊的吞咽声（自定义音效，不是吃东西的声音）
            serverLevel.playSound(null, this.blockPosition(), JRSounds.SLIME_SWALLOW,
                    SoundSource.NEUTRAL, 1.2F, 1.0F);
            serverLevel.sendParticles(ParticleTypes.ITEM_SLIME,
                    this.getX(), this.getY() + this.tier().blockSize() * 0.5D, this.getZ(),
                    18, 0.4D, 0.4D, 0.4D, 0.03D);
        }
        Player owner = this.ownerUuid == null ? null
                : this.level().getPlayerInAnyDimension(this.ownerUuid);
        if (owner != null) {
            owner.sendOverlayMessage(Component.translatable("justarod.slime.devour.swallowed", swallowed)
                    .withStyle(ChatFormatting.GREEN));
        }
    }

    /**
     * 把技能状态推给正在骑着的主人（服务端）。
     *
     * <p>只在「主人骑着」时推，且状态没变就不推；动画期间每 tick 推一次，
     * 因为动画进度靠 {@code animStartTick} + 客户端自己的 tickCount 还原，
     * 起始值一旦晚到一帧，整个放大过程就会整段延迟。
     */
    private void tickDevourSync() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        Player owner = this.ownerUuid == null ? null : serverLevel.getPlayerInAnyDimension(this.ownerUuid);
        boolean riding = owner != null && this.getControllingPassenger() == owner;
        if (!riding) {
            // 下马时补发一帧「未骑乘」状态，客户端好把提示收起来
            if (devourRideSynced && owner instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                sendDevourState(serverPlayer, false);
            }
            devourRideSynced = false;
            return;
        }
        if (!(owner instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) return;

        this.devourSyncTimer++;
        boolean stateChanged = riding != this.devourRideSynced;
        if (this.isDevouring()) {
            // 动画期间每 tick 推，保证放大起始帧不丢
            sendDevourState(serverPlayer, true);
        } else if (stateChanged || this.devourSyncTimer >= DEVOUR_SYNC_INTERVAL) {
            this.devourSyncTimer = 0;
            sendDevourState(serverPlayer, true);
        }
        this.devourRideSynced = riding;
    }

    private void sendDevourState(net.minecraft.server.level.ServerPlayer player, boolean riding) {
        int targetCount = 0;
        if (riding && !this.isDevouring() && devourCooldown <= 0) {
            targetCount = this.devourBatch().size();
        }
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,
                new SlimeDevourStatePayload(this.getId(), this.devourCooldown, targetCount));
    }

    // ==================== 移动 ====================

    /**
     * 让史莱姆朝某个坐标跳过去。
     *
     * <p>史莱姆的移动控制器不认坐标（见 {@link org.cneko.justarod.mixin.SlimeMoveControlAccessor}），
     * 必须「设朝向 + 每 tick 驱动跳跃」两步一起做，否则它只会朝旧朝向原地跳。
     *
     * @param speedMultiplier 速度倍率（乘在 MOVEMENT_SPEED 上，1.0 = 原版史莱姆的跳跃速度）
     * @param aggressive      是否激进（缩短起跳间隔，追击时用）
     */
    public void hopToward(double x, double y, double z, double speedMultiplier, boolean aggressive) {
        double dx = x - this.getX();
        double dz = z - this.getZ();
        if (dx * dx + dz * dz < 1.0E-4D) {
            return;
        }
        float yaw = (float) (net.minecraft.util.Mth.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
        if (this.getMoveControl() instanceof org.cneko.justarod.mixin.SlimeMoveControlAccessor control) {
            control.justarod$setDirection(yaw, aggressive);
            control.justarod$setWantedMovement(speedMultiplier);
        } else {
            // 理论上到不了这里（Slime 的移动控制器一定是 SlimeMoveControl），保底走基类实现
            this.getMoveControl().setWantedPosition(x, y, z, speedMultiplier);
        }
    }

    /**
     * 26.x：被玩家操控的实体默认是「客户端权威」——服务端不模拟它的移动，
     * 等客户端发位置包。而客户端的 {@code getControllingPassenger()} 依赖 ownerUuid
     * （那份数据没有同步给客户端），于是两边都不动：**主人在体内按 WASD 毫无反应**。
     *
     * <p>史莱姆改成**服务端权威**：服务端用 {@code ServerPlayer#getLastClientInput()}
     * 驱动跳跃移动，客户端照常接收位置同步。
     */
    @Override
    public boolean isClientAuthoritative() {
        return false;
    }

    // ==================== 乘客挂载点：全部放在体内中心 ====================

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, net.minecraft.world.entity.EntityDimensions dimensions, float partialTick) {
        // 外壳现在是 blockSize 格见方（见 JRTamedSlimeRenderer#scale，那里修掉了「外观只有一半大」的
        // 2 倍缩放 bug），所以体内的可用空间就是 blockSize。
        // 乘客要**整个待在体内**：竖直按乘客自身高度居中，再往下压 5% 给模型上多出来的
        // 耳朵/帽子留余量；比外壳还高的乘客只能站在壳底（此时压不过 0，直接夹住）。
        double size = this.tier().blockSize();
        double passengerHeight = Math.max(0.1D, passenger.getBbHeight());
        double y = Math.max(0.0D, (size - passengerHeight) * 0.5D - size * 0.05D);

        // 水平方向同样收在壳内：乘客自身宽度不能顶到壳壁
        double passengerWidth = Math.max(0.1D, passenger.getBbWidth());
        double maxRadius = Math.max(0.0D, size * 0.5D - passengerWidth * 0.5D);
        double index = Math.max(0, this.getPassengers().indexOf(passenger));
        double angle = index * 2.399963D;  // 黄金角散布，避免乘客重叠
        double radius = Math.min(Math.min(0.35D, 0.12D * (index + 1)) * size, maxRadius);
        return new Vec3(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        // 主人在体内时取得操控权
        for (Entity passenger : this.getPassengers()) {
            if (passenger instanceof Player player && this.isOwner(player)) {
                return player;
            }
        }
        return null;
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 movementInput) {
        return movementInput;
    }

    /** 主人的按键 → 世界方向（相对主人的视线：W 前、A 左…）；没按方向键返回零向量 */
    private Vec3 riddenDirection(net.minecraft.server.level.ServerPlayer player) {
        Input input = player.getLastClientInput();
        float forward = (input.forward() ? 1.0F : 0.0F) - (input.backward() ? 1.0F : 0.0F);
        float strafe = (input.left() ? 1.0F : 0.0F) - (input.right() ? 1.0F : 0.0F);
        if (forward == 0.0F && strafe == 0.0F) return Vec3.ZERO;
        Vec3 look = Vec3.directionFromRotation(0.0F, player.getYRot());
        Vec3 left = Vec3.directionFromRotation(0.0F, player.getYRot() - 90.0F);
        return look.scale(forward).add(left.scale(strafe)).normalize();
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        // 实际速度在 travelRiddenHopping 里按「跳跃中/落地等待」分别设置，
        // 这里只作为父类接口的兜底值
        return RIDE_HOP_SPEED;
    }

    @Override
    protected void tickRidden(Player player, Vec3 travelVector) {
        super.tickRidden(player, travelVector);
        // 朝向由「输入方向」决定（在下面的 travel 里设置），这里只同步俯仰
        this.yRotO = this.getYRot();
        this.setXRot(player.getXRot() * 0.5F);
        this.setRot(this.getYRot(), this.getXRot());
        this.yHeadRot = this.getYRot();
        this.yBodyRot = this.getYRot();
    }

    @Override
    public void travel(Vec3 travelVector) {
        Player controller = this.getControllingPassenger() instanceof Player p ? p : null;
        if (!(controller instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            // 身上带着乘客、但没人操控它时，必须把「前进量」彻底清掉。
            //
            // 漂移就是这样来的：原版 `Mob` 是「乘客即司机」的模型
            // （`Mob#travel` 会自己 `getRiddenInput(...)` 然后 `travelRidden(...)`），
            // 所以一个**非主人**的乘客会让 `super.travel` 拿 `zza` 去推史莱姆前进；
            // 而 `zza`/`speed` 会被 AI 目标（跳跃/跟随）写成残留值，于是它就一直往前平移。
            // 这条路径下史莱姆应当完全不动，移动只由 `travelRiddenHopping` 负责。
            if (!this.getPassengers().isEmpty()) {
                this.riddenMovementReset();
            }
            super.travel(travelVector);
            return;
        }
        this.travelRiddenHopping(serverPlayer);
    }

    /**
     * 清掉所有「会让它自己往前走」的残留状态。
     *
     * <p>`zza`（前进量）、`speed`（速度倍率）、跳跃间隔、以及 MoveControl/Navigation 的
     * 目标点，任何一项留下残值都会让史莱姆在没人操控时继续滑行。
     */
    private void riddenMovementReset() {
        this.rideHopCooldown = 0;
        this.setSpeed(0.0F);
        this.setZza(0.0F);
        if (!this.level().isClientSide()) {
            this.getNavigation().stop();
            this.getMoveControl().setWantedPosition(this.getX(), this.getY(), this.getZ(), 0.0D);
        }
    }

    /**
     * 有人上车时清一次移动状态。
     *
     * <p>尤其是**玩家钻进体内的那一刻**：在那之前 AI（跳跃/跟随）可能刚把
     * `zza`/`speed` 写成非零，切换成玩家操控后如果没清掉，
     * 史莱姆会带着那个残留量一直往前平移。
     */
    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        this.riddenMovementReset();
    }

    /**
     * 主人操控时的移动：**和野生史莱姆一样是跳的**，不是平移。
     *
     * <p>做法照抄 {@code Slime.SlimeMoveControl}：落地后按间隔起跳，空中保持前进速度，
     * 于是走起来就是一蹦一蹦的；按键方向直接决定朝向（按 A 它就朝左跳，会自己转身）。
     * 空格 = 立刻再跳一次（不用等间隔）。
     */
    private void travelRiddenHopping(net.minecraft.server.level.ServerPlayer player) {
        Vec3 direction = this.riddenDirection(player);
        if (direction.lengthSqr() < 1.0E-6D) {
            // 没给方向：站住（重力/摩擦交给父类），并把跳跃间隔清零，下次按方向键立刻起跳
            this.rideHopCooldown = 0;
            this.setSpeed(0.0F);
            this.setZza(0.0F);
            super.travel(Vec3.ZERO);
            return;
        }

        // 朝输入方向
        float yaw = (float) (net.minecraft.util.Mth.atan2(-direction.x, direction.z) * 180.0D / Math.PI);
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;

        if (this.onGround()) {
            if (player.getLastClientInput().jump()) {
                this.rideHopCooldown = 0;
            }
            if (this.rideHopCooldown > 0) {
                this.rideHopCooldown--;
                this.setSpeed(0.0F);
                super.travel(Vec3.ZERO);
                return;
            }
            this.rideHopCooldown = RIDE_HOP_INTERVAL_TICKS;
            this.getJumpControl().jump();
        }

        // 空中（以及刚起跳的这一刻）朝当前朝向前进；父类负责重力与摩擦
        this.setSpeed(RIDE_HOP_SPEED);
        this.setZza(1.0F);
        super.travel(new Vec3(0.0D, 0.0D, 1.0D));
    }

    /** 被操控时每次起跳之间的最小间隔（tick）：比野生史莱姆（10~20）更跟手 */
    public static final int RIDE_HOP_INTERVAL_TICKS = 5;
    /** 被操控时的跳跃前进速度倍率 */
    public static final float RIDE_HOP_SPEED = 0.32F;
    /** 距离下次起跳还差几 tick */
    private int rideHopCooldown = 0;

    /** 被吞的实体与主人一律从史莱姆脚下出来 */
    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        return new Vec3(this.getX(), this.getY(), this.getZ());
    }

    // ==================== 每 tick ====================

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.tickClient();
            return;
        }
        if (recallCooldown > 0) recallCooldown--;
        if (swallowCooldown > 0) swallowCooldown--;
        if (devourCooldown > 0) devourCooldown--;

        this.tickDevour();
        this.enforceContainedEntities();
        this.tickOwnerSneakEscape();
        this.reconcileContainedOwner();
        this.tickDecompose();
        this.tickGrowthSettle();
        this.tickSlimeBallHeal();
        this.tickContentsSync();
        this.tickDevourSync();
        this.closeOutdatedMenus();

        // 主人不在体内时，待命指令下保持静止
        if (this.getControllingPassenger() == null) {
            // 身上带着乘客却没人操控它：把 AI 遗留的前进量清掉，否则它会自己一直往前平移
            // （见 travel() 里的说明）。放在 AI 之前，保证同一 tick 内 AI 再写也会被这一步覆盖。
            this.enforceIdleWhenUncontrolled();
            if (command == COMMAND_STAY) {
                this.getNavigation().stop();
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.6D, 1.0D, 0.6D));
            }
        }
    }

    /**
     * 身上有乘客、但没有「主人操控」时，不让它自己走动。
     *
     * <p>这是「骑上去就一直往前平移」的兜底闸：`zza`/`speed` 是原版给
     * 「乘客即司机」模型用的公共状态（`Mob#travel` 会拿它推进载具），
     * AI 目标也会写它；只要有任何一处留下残值，史莱姆就会平移。
     * 这里每 tick 清一次，配合 {@link #travel} 里的判断，把这条路彻底堵死。
     */
    private void enforceIdleWhenUncontrolled() {
        if (this.getPassengers().isEmpty()) return;
        if (this.getControllingPassenger() != null) return;
        this.riddenMovementReset();
    }

    /**
     * 升级会让体内容量变大，而菜单槽位是建好就固定的（26.x 无法增删），
     * 两端数量不一致会直接导致容器内容包越界掉线，所以这里主动关掉旧界面。
     */
    private void closeOutdatedMenus() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        if (this.tierLevel == 0 && this.storage.items().size() <= SlimeTier.T0.capacity()) return;
        for (var serverPlayer : serverLevel.players()) {
            if (serverPlayer.containerMenu instanceof SlimeMenu menu
                    && menu.slime() == this && menu.slotCountOutOfSync()) {
                serverPlayer.closeContainer();
                serverPlayer.sendOverlayMessage(Component.translatable("justarod.slime.gui.reopen"));
            }
        }
    }

    private void tickClient() {
        // 客户端只负责渲染插值；体内实体的位置由服务端同步
        if (entityData.get(DATA_DECOMPOSE_STATE) == DECOMPOSE_RUNNING) {
            float p = entityData.get(DATA_DECOMPOSE_PROGRESS);
            if (p > 0.0F && this.random.nextInt(6) == 0) {
                this.level().addParticle(ParticleTypes.ITEM_SLIME,
                        this.getRandomX(0.6D), this.getY() + this.tier().blockSize() * 0.5D, this.getRandomZ(0.6D),
                        0.0D, 0.02D, 0.0D);
            }
        }
    }

    /** 体内实体的约束：不能移动、不能攻击，随时间自动漂移逃出 */
    private void enforceContainedEntities() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        List<UUID> ids = storage.containedEntities();
        for (int i = 0; i < ids.size(); i++) {
            UUID uuid = ids.get(i);
            Entity entity = findContainedEntity(uuid);
            if (entity == null) {
                // 实体已消失（死亡/卸载）：归还空间
                storage.removeEntity(uuid);
                passengerOrder.remove(uuid);
                i--;
                continue;
            }
            // 已死 / 正在死 / 已被移除的乘客不算「在体内」：断开挂载并归还空间。
            // 不做这一步的话，下面那句 startRiding 会把它重新按上车，
            // 玩家复活后仍然卡在史莱姆体内（本轮反馈的问题）。
            if (isDeadPassenger(entity)) {
                entity.stopRiding();
                storage.removeEntity(uuid);
                passengerOrder.remove(uuid);
                i--;
                continue;
            }
            if (!entity.isPassenger() || entity.getVehicle() != this) {
                if (!entity.startRiding(this, true, true)) {
                    releaseEntity(uuid);
                    i--;
                    continue;
                }
            }
            entity.setDeltaMovement(Vec3.ZERO);
            entity.resetFallDistance();
            // 锁住视线与朝向，避免被吞者看到外面（也顺便防止转向带来的移动）
            entity.setYRot(this.getYRot());
            entity.yRotO = this.getYRot();
            entity.setXRot(this.getXRot());
            entity.xRotO = this.getXRot();
            if (entity instanceof LivingEntity living) {
                living.hurtMarked = false;
                // 体内不受窒息/火焰伤害
                living.clearFire();
            }
            // 挣扎：随时间自动漂移（主人免疫——他是来操控史莱姆的，不能被自动甩出去）
            if (storage.isStruggleImmune(uuid)) {
                continue;
            }
            float progress = storage.struggleProgress().get(i);
            progress += autoStrugglePerTick(entity);
            storage.struggleProgress().set(i, progress);
            if (progress >= struggleThreshold()) {
                releaseEntity(uuid);
                i--;
            }
        }
    }

    // ==================== 体内内容同步（渲染用） ====================

    /** 上次推给客户端的物品版本号 */
    private int syncedContentsVersion = -1;
    /** 内容同步节流计时 */
    private int contentsSyncTimer = 0;

    /**
     * 每 5 tick 检查一次体内内容版本，变了就把「物品图标列表」推给附近玩家。
     *
     * <p>不推的话客户端画不出体内物品——物品列表不在实体同步数据里（最长 724 格，塞不下）。
     * 只发物品 id（见 {@link org.cneko.justarod.packet.SlimeContentsPayload}），一包几十字节。
     */
    private void tickContentsSync() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        if (++contentsSyncTimer < CONTENTS_SYNC_INTERVAL) return;
        contentsSyncTimer = 0;
        int version = this.storage.version();
        if (version == syncedContentsVersion) return;
        syncedContentsVersion = version;

        java.util.List<Integer> ids = new java.util.ArrayList<>();
        for (ItemStack stack : this.storage.items()) {
            if (stack.isEmpty()) continue;
            ids.add(net.minecraft.core.registries.BuiltInRegistries.ITEM.getId(stack.getItem()));
            if (ids.size() >= org.cneko.justarod.packet.SlimeContentsPayload.MAX_ITEMS) break;
        }
        var payload = new org.cneko.justarod.packet.SlimeContentsPayload(this.getId(), ids);
        for (var player : serverLevel.players()) {
            if (player.distanceToSqr(this) < CONTENTS_SYNC_RANGE * CONTENTS_SYNC_RANGE) {
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, payload);
            }
        }
    }

    /** 内容同步间隔（tick）：分解时物品每 tick 都在少，4 次/秒够看了 */
    public static final int CONTENTS_SYNC_INTERVAL = 5;
    /** 内容同步半径（格） */
    public static final double CONTENTS_SYNC_RANGE = 48.0D;

    // ==================== 粘液球回血 ====================

    /** 每 5 秒检查一次：血量没满且体内有粘液球就吃一个回 1 点血 */
    public static final int SLIME_BALL_HEAL_INTERVAL = 100;
    /** 一个粘液球回多少血 */
    public static final float SLIME_BALL_HEAL_AMOUNT = 1.0F;
    private int slimeBallHealTimer = 0;

    private void tickSlimeBallHeal() {
        if (++slimeBallHealTimer < SLIME_BALL_HEAL_INTERVAL) return;
        slimeBallHealTimer = 0;
        if (this.getHealth() >= this.getMaxHealth()) return;

        int slot = this.findSlimeBallSlot();
        if (slot < 0) return;
        if (!this.storage.consumeOneItem(slot)) return;

        this.heal(SLIME_BALL_HEAL_AMOUNT);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HEART,
                    this.getX(), this.getY() + this.tier().blockSize() * 0.6D, this.getZ(),
                    2, 0.25D, 0.25D, 0.25D, 0.01D);
        }
    }

    /** 找一格粘液球；没有返回 -1 */
    private int findSlimeBallSlot() {
        var items = this.storage.items();
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty() && stack.is(net.minecraft.world.item.Items.SLIME_BALL)) return i;
        }
        return -1;
    }

    /** 每个实体每 tick 自动积累的挣扎进度：越大的史莱姆越难逃出 */
    private float autoStrugglePerTick(Entity entity) {
        float base = entity instanceof Player ? 0.35F : 0.5F;
        // 阈值随等级线性上升，这里让"漂移速度"相应下降
        return base / (float) Math.max(1, this.tierLevel + 1);
    }

    /** 挣扎阈值：100 + 等级 * 50 */
    public float struggleThreshold() {
        return 100.0F + this.tierLevel * 50.0F;
    }

    /** 玩家主动挣扎（按键）一次 */
    public void applyStruggle(Entity entity, float amount) {
        int index = storage.containedEntities().indexOf(entity.getUUID());
        if (index < 0) return;
        // 主人连点挣扎键也没用：他本来就该待在体内操控，要出去用潜行键
        if (storage.isStruggleImmune(entity.getUUID())) return;
        float progress = storage.struggleProgress().get(index) + amount;
        storage.struggleProgress().set(index, progress);
        if (progress >= struggleThreshold()) {
            releaseEntity(entity.getUUID());
        }
    }

    // ==================== 分解系统 ====================

    // 时序（与作者确认）：
    //  * 一份催化物 = 一批分解，最多处理 max(容量/4, 1) 组内容（一组 = 一格：最多 64 个物品 / 一个实体）
    //  * **每组固定 80 tick（4 秒）**，但组内物品是**一个一个**被消耗掉的（不是整叠瞬间消失），
    //    所以玩家能看着格子一点点空下去；消耗节奏按 80 tick 均分（一叠 64 个 ≈ 每 tick 一个）
    //  * 每消耗一个物品回 1 点血
    //  * 成长进度按「组」结算：不足一组的物品数量攒起来，每 80 tick（4 秒）结算一次，
    //    满 64 个算 1 格进度，余数留到下一个周期
    //  * 一批没跑完之前不能再喂新的催化物
    public static final int DECOMPOSE_STEP_TICKS = 80;           // 每组 4 秒
    public static final int GROWTH_SETTLE_TICKS = 80;            // 成长 4 秒结算一次
    public static final int GROWTH_ITEMS_PER_UNIT = 64;          // 64 个物品 = 1 格成长进度
    public static final float HEAL_PER_ITEM = 1.0F;              // 每分解一个物品回 1 点血
    public static final float HEAL_PER_ENTITY = 2.0F;

    /** 一份催化物最多能分解多少组内容：max(最大容量 / 4, 1) */
    public int decomposeBatchSize() {
        return Math.max(1, this.tier().capacity() / 4);
    }

    /** 是否有一批分解正在进行（进行期间拒绝新的催化物） */
    public boolean isDecomposing() {
        return decomposeTasks > 0;
    }

    /**
     * 给分解器投入一份催化物：开启一批分解。
     *
     * @return false 表示已有批次在跑（调用方不该消耗催化物）
     */
    public boolean addDecomposeTask() {
        if (decomposeTasks > 0) {
            return false;
        }
        decomposeTasks = this.decomposeBatchSize();
        return true;
    }

    /** 当前批次还剩多少组没分解完 */
    public int getDecomposeTasks() {
        return decomposeTasks;
    }

    /** 当前这一组正在被吃掉的那一格（-1 = 本组是实体，或者没有物品组） */
    private int activeItemSlot = -1;
    /** 这一组开始时该格里的物品数 */
    private int activeItemCount = 0;
    /** 这一组已经被吃掉的物品数 */
    private int activeItemConsumed = 0;
    /** 攒着还没结算成成长进度的物品数（每 4 秒结算一次） */
    private int pendingGrowthItems = 0;
    /** 成长结算计时 */
    private int growthSettleTimer = 0;
    /** 回血溢出的小数部分（攒够 1 点再转成成长进度，避免每次都被截断丢掉） */
    private float overflowHealBuffer = 0.0F;

    private void tickDecompose() {
        if (decomposeTasks <= 0) {
            if (decomposeState != DECOMPOSE_IDLE) {
                decomposeState = DECOMPOSE_IDLE;
                entityData.set(DATA_DECOMPOSE_STATE, DECOMPOSE_IDLE);
                entityData.set(DATA_DECOMPOSE_PROGRESS, 0.0F);
            }
            return;
        }

        // 计时开始：取下一组内容并决定本步时长
        if (decomposeTimer <= 0 && !stepActive) {
            if (!this.beginDecomposeStep()) {
                return;
            }
        }

        decomposeTimer--;
        if (activeItemSlot >= 0) {
            // 组内物品逐个消耗（本 tick 该吃掉几个就吃几个）
            this.consumeItemTrickle(false);
        }

        float progress = decomposeTotal <= 0 ? 1.0F : 1.0F - (float) decomposeTimer / (float) decomposeTotal;
        entityData.set(DATA_DECOMPOSE_PROGRESS, Math.max(0.0F, Math.min(1.0F, progress)));

        if (decomposeTimer <= 0) {
            if (activeItemSlot >= 0) {
                this.consumeItemTrickle(true);   // 收尾：把这一格剩下的吃完
            }
            this.finishDecomposeStep();
            decomposeTasks--;
        }
    }

    /**
     * 组内物品的消耗节奏：整组 {@link #DECOMPOSE_STEP_TICKS} tick，物品按时间均匀地被吃掉——
     * 「第 k 个物品在本组走到 k/count 时被吃掉」，所以最后一个物品正好落在组末，
     * 而一叠 64 个几乎是每 tick 掉一个，玩家能看着格子一点点空下去。
     *
     * @param elapsed 本组已经过去的 tick（1..totalTicks）
     * @param count   本组一共几个物品
     * @return 到这一刻为止应当已经吃掉的物品数
     */
    static int consumedItemsAt(long elapsed, int count, int totalTicks) {
        if (count <= 0) return 0;
        if (totalTicks <= 0 || elapsed >= totalTicks) return count;
        if (elapsed <= 0) return 0;
        return (int) (elapsed * (long) count / totalTicks);
    }

    private void consumeItemTrickle(boolean finalize) {
        if (activeItemSlot < 0 || activeItemCount <= 0) return;
        long elapsed = Math.max(0, decomposeTotal - Math.max(0, decomposeTimer));
        int target = finalize ? activeItemCount
                : consumedItemsAt(elapsed, activeItemCount, decomposeTotal);
        while (activeItemConsumed < target) {
            if (!storage.consumeOneItem(activeItemSlot)) break;
            activeItemConsumed++;
            // 一个物品 = 1 点血；溢出部分攒起来转成长进度
            overflowHealBuffer += healWithOverflow(HEAL_PER_ITEM);
            int overflowUnits = (int) overflowHealBuffer;
            if (overflowUnits > 0) {
                overflowHealBuffer -= overflowUnits;
                addGrowthProgress(overflowUnits);
            }
            // 每吃掉一个物品冒一点粒子，看得见「在消化」
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        this.getX(), this.getY() + this.tier().blockSize() * 0.5D, this.getZ(),
                        1, 0.25D, 0.25D, 0.25D, 0.01D);
            }
        }
    }

    /** 成长进度结算：物品数量攒够 64 的整数倍才转成进度，余数留到下一个 4 秒周期 */
    private void tickGrowthSettle() {
        if (pendingGrowthItems < GROWTH_ITEMS_PER_UNIT) {
            growthSettleTimer = 0;
            return;
        }
        if (++growthSettleTimer < GROWTH_SETTLE_TICKS) {
            return;
        }
        growthSettleTimer = 0;
        int units = pendingGrowthItems / GROWTH_ITEMS_PER_UNIT;
        pendingGrowthItems -= units * GROWTH_ITEMS_PER_UNIT;
        addGrowthProgress(units);
    }

    /**
     * 开启一个分解步骤（一组内容）。
     *
     * @return false 表示没东西可分解 / 血量不足被挂起
     */
    private boolean beginDecomposeStep() {
        int slot = storage.firstNonEmptySlot();
        if (slot >= 0) {
            activeItemSlot = slot;
            activeItemCount = storage.items().get(slot).getCount();
            activeItemConsumed = 0;
            pendingEntity = null;
            pendingEntityVolume = 1;
            decomposeTotal = DECOMPOSE_STEP_TICKS;
            // 关键：计时器必须在这里种上。以前漏了这一句 → decomposeTimer 一直是 0，
            // 每一步「开始后立刻结束」，整批 22 组在一秒内跑完（看上去像 4 tick）。
            decomposeTimer = decomposeTotal;
            stepActive = true;
            storage.setSuspendedReason(null);
            this.markDecomposeRunning();
            return true;
        }

        activeItemSlot = -1;
        Entity entity = this.firstDecomposableEntity();
        if (entity == null) {
            // 体内已被分解干净
            decomposeTasks = 0;
            storage.setSuspendedReason(null);
            return false;
        }

        if (entity instanceof LivingEntity living) {
            float cost = entityDecomposeCost(living);
            if (this.getHealth() - cost < 1.0F) {
                // 血量不足：任务挂起，等回血（不消耗内容）
                decomposeState = DECOMPOSE_SUSPENDED;
                entityData.set(DATA_DECOMPOSE_STATE, DECOMPOSE_SUSPENDED);
                storage.setSuspendedReason("justarod.slime.hp_insufficient");
                return false;
            }
            this.setHealth(this.getHealth() - cost);
        }
        // 一个实体 = 一组 = 4 秒
        decomposeTotal = DECOMPOSE_STEP_TICKS;

        // 从登记表移除并记住，结算时再丢弃实体
        int volume = SlimeTier.volumeOf(entity);
        storage.removeEntity(entity.getUUID());
        passengerOrder.remove(entity.getUUID());
        pendingEntity = entity;
        pendingEntityVolume = Math.max(1, volume);
        decomposeTimer = decomposeTotal;
        stepActive = true;
        storage.setSuspendedReason(null);
        this.markDecomposeRunning();
        return true;
    }

    private void markDecomposeRunning() {
        decomposeState = DECOMPOSE_RUNNING;
        entityData.set(DATA_DECOMPOSE_STATE, DECOMPOSE_RUNNING);
    }

    private void finishDecomposeStep() {
        stepActive = false;
        boolean wasEntity = pendingEntity != null;

        if (wasEntity) {
            // 实体被彻底分解：回血 + 按体积记成长（物品组的回血/成长在 consumeItemTrickle 里逐条结算）
            float overflow = healWithOverflow(HEAL_PER_ENTITY);
            addGrowthProgress(pendingEntityVolume + (int) overflow);
            pendingEntity.stopRiding();
            this.settleDecomposedEntity(pendingEntity);
            pendingEntity = null;
        }

        activeItemSlot = -1;
        activeItemCount = 0;
        activeItemConsumed = 0;

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    this.getX(), this.getY() + this.getBbHeight() * 0.5D, this.getZ(),
                    6, 0.3D, 0.3D, 0.3D, 0.01D);
        }
    }

    /**
     * 被分解的实体按「正常死亡」结算：掉落物、经验照常给。
     *
     * <p>规则有三条，都是踩过坑才定下来的：
     *
     * <ol>
     *   <li><b>先下车再致死。</b> 原版 {@code LivingEntity#die} / {@code ServerPlayer#die}
     *       <b>都不会</b>把乘客放下来（`javap` 核对过：die 里没有 stopRiding/eject），
     *       所以不断开挂载关系的话，玩家死了还挂在史莱姆身上，复活后仍在体内。
     *       （另外 {@link #enforceContainedEntities()} 对死者也有一道闸，见
     *       {@link #isDeadPassenger}。）</li>
     *   <li><b>伤害来源尽量挂主人</b>，因为掉落表里不少条目只在「玩家击杀」时生效。
     *       但受害者**就是主人本人**时不能这么做：{@code ServerPlayer#hurtServer} 会用
     *       {@code canHarmPlayer(来源玩家)} 过滤玩家来源，「自己打自己」恒为 false
     *       → 这一击打不掉血 → 掉进兜底分支，而对活着的 {@code ServerPlayer} 调
     *       {@code discard()} 会把玩家实体整个拆掉（连带史莱姆一起消失）。
     *       所以那种情况改用 {@code genericKill()}：它不是玩家来源、不过 PvP 过滤，
     *       玩家会走正常的死亡 / 掉落 / 重生流程。</li>
     *   <li><b>玩家绝不 discard</b>：打不死的（无敌、创造模式）只放出来交给原版处理。</li>
     * </ol>
     */
    private void settleDecomposedEntity(Entity entity) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            entity.discard();
            return;
        }
        if (entity == this) return;

        // 先下车、再致死。顺序很关键：
        // 原版 `LivingEntity#die` / `ServerPlayer#die` **都不会**把乘客放下来
        // （`javap` 核对过：die 里没有 stopRiding/eject 调用），所以「死了还在体内」
        // 是原版行为，得我们自己断开挂载关系。放在致死之前做，
        // 客户端才能在死亡画面前先收到「已下车」，复活后就干净了。
        entity.stopRiding();

        Player owner = this.ownerUuid == null ? null : serverLevel.getPlayerInAnyDimension(this.ownerUuid);
        var source = owner != null && owner != entity
                ? serverLevel.damageSources().playerAttack(owner)
                : serverLevel.damageSources().genericKill();
        if (entity instanceof LivingEntity living) {
            living.hurtServer(serverLevel, source, Float.MAX_VALUE);
        }
        // 打不死的实体（无敌、创造模式玩家等）兜底清理。玩家绝不走 discard()：
        // 那会把玩家实体整个拆掉而不是「死亡并重生」，所以只把它放出来。
        if (entity.isAlive() && !(entity instanceof Player)) {
            entity.discard();
        }
    }

    /** 当前步骤正在分解的实体（物品步骤为 null） */
    @Nullable
    private Entity pendingEntity = null;
    /** 当前步骤的内容体积（实体步骤用实体体积，物品步骤为 1） */
    private int pendingEntityVolume = 0;
    /** 是否有正在计时的分解步骤 */
    private boolean stepActive = false;

    /** 分解一个实体需要消耗的自身血量：每格碰撞箱 = 5% × 实体最大血量 */
    public static float entityDecomposeCost(LivingEntity entity) {
        int volume = SlimeTier.volumeOf(entity);
        float cost = entity.getMaxHealth() * 0.05F * volume;
        // 半颗心为最小粒度
        return Math.max(0.5F, Math.round(cost * 2.0F) / 2.0F);
    }

    /**
     * 体内**可被分解**的第一个实体。
     *
     * <p>就是登记表的第 0 个，**没有**任何「主人豁免」：需求要的就是
     * 「分解实体不再看是不是主人，主人也可以被分解」。主人进到体内的唯一方式
     * 就是当乘客，而 {@link #getControllingPassenger()} 只要主人在乘客里就返回他，
     * 所以「跳过操控者」等于**永久跳过主人** —— 那正是「主人还是分解不了」的原因。
     *
     * <p>主人会被分解这件事本身是安全的，前提是**结算方式必须能真的杀死玩家**：
     * 早期版本用「主人打主人」（`playerAttack(owner)`）结算，而
     * `ServerPlayer#hurtServer` 会用 `canHarmPlayer(来源玩家)` 过滤，自己打自己永远为 false
     * → 不掉血 → 落进 `discard()` 兜底把玩家实体拆掉，史莱姆跟着一起完蛋。
     * 现在结算见 {@link #settleDecomposedEntity}：受害者与伤害来源是同一个人时改用
     * `genericKill()`，玩家会走正常的死亡/掉落/重生流程。
     */
    @Nullable
    private Entity firstDecomposableEntity() {
        return pickFirstDecomposable(storage.containedEntities(), null, this::findContainedEntity);
    }

    /**
     * 从体内登记表里挑出第一个**可被分解**的实体。
     *
     * <p>抽成静态纯函数是为了能离线验证（{@code .tmp/jrtest/DevourRuleTest.java}）：
     * 挑错人的代价很大（见 {@link #settleDecomposedEntity} 的说明）。
     * 泛型只是为了测试能用占位对象代替实体，实际调用点始终是 {@code Entity}。
     *
     * @param contained    体内登记表（按登记顺序）
     * @param skipId       要跳过的 UUID（当前调用点传 null，即不跳过任何人）
     * @param lookup       UUID → 实体；查不到（已卸载/已消失）的登记项直接跳过
     * @return 第一个可分解实体；没有则 null
     */
    @Nullable
    public static <T> T pickFirstDecomposable(List<UUID> contained, @Nullable UUID skipId,
                                             java.util.function.Function<UUID, T> lookup) {
        for (UUID uuid : contained) {
            if (skipId != null && skipId.equals(uuid)) continue;
            T entity = lookup.apply(uuid);
            if (entity != null) return entity;
        }
        return null;
    }

    /** 回血；返回溢出的治疗量（用于转成成长进度） */
    private float healWithOverflow(float amount) {
        float before = this.getHealth();
        float max = this.getMaxHealth();
        if (before >= max) return amount;
        this.heal(amount);
        return Math.max(0.0F, amount - (max - before));
    }

    /** 成长进度：满一级就升级，可连续升级；满级后转 XP */
    public void addGrowthProgress(int units) {
        if (units <= 0) return;
        if (this.tier().isMax()) {
            this.grantXp(SlimeTier.T5.xpFor(units));
            return;
        }
        growthProgress += units;
        int gained = 0;
        while (!this.tier().isMax() && growthProgress >= nextLevelCost()) {
            growthProgress -= nextLevelCost();
            setTierLevel(tierLevel + 1);
            gained++;
        }
        if (gained > 0) {
            this.setHealth(this.getMaxHealth());
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HEART,
                        this.getX(), this.getY() + this.getBbHeight(), this.getZ(),
                        8, 0.4D, 0.4D, 0.4D, 0.02D);
                serverLevel.playSound(null, this.blockPosition(), SoundEvents.SLIME_SQUISH,
                        SoundSource.NEUTRAL, 1.0F, 0.5F + 0.1F * tierLevel);
            }
        }
        if (this.tier().isMax() && growthProgress > 0) {
            this.grantXp(SlimeTier.T5.xpFor(growthProgress));
            growthProgress = 0;
        }
    }

    /** 升到下一级所需内容量（格） */
    public int nextLevelCost() {
        int next = Math.min(SlimeTier.MAX_LEVEL, tierLevel + 1);
        return SlimeTier.of(next).levelCost();
    }

    private void grantXp(int amount) {
        if (amount <= 0) return;
        if (this.level() instanceof ServerLevel serverLevel) {
            ExperienceOrb.award(serverLevel, this.position(), amount);
        }
    }

    // ==================== 交互 ====================

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // (1) 分解催化物：开启一个分解任务（26.x 的 Item#interactLivingEntity 只在命名牌/刷怪蛋上被调用，
        //     所以物品对实体的右键逻辑必须写在 mobInteract 里）
        if (stack.is(JRItems.Companion.getDECOMPOSE_CATALYST())) {
            return this.useCatalyst(player, stack);
        }

        // (2) 手持史莱姆铃铛：召回 / 显示信息
        if (stack.is(JRItems.Companion.getSLIME_BELL())) {
            if (this.isOwner(player)) {
                if (recallCooldown > 0) {
                    player.sendOverlayMessage(Component.translatable("justarod.slime.recall.cooldown",
                            recallCooldown / 20).withStyle(ChatFormatting.RED));
                } else {
                    this.recallTo(player);
                }
            } else {
                player.sendOverlayMessage(Component.translatable("justarod.slime.not_owner")
                        .withStyle(ChatFormatting.RED));
            }
            return InteractionResult.CONSUME;
        }

        // (3) 潜行 + 右键：打开体内容器 GUI（只有主人能开）
        if (player.isShiftKeyDown()) {
            if (!this.isOwner(player)) {
                player.sendOverlayMessage(Component.translatable("justarod.slime.not_owner")
                        .withStyle(ChatFormatting.RED));
                return InteractionResult.CONSUME;
            }
            this.openContainer(player);
            return InteractionResult.CONSUME;
        }

        // (4) 手持可堆叠物品普通右键：投入一整组（一组占一格）；只有主人能喂
        if (!stack.isEmpty() && stack.getMaxStackSize() > 1) {
            if (!this.isOwner(player)) {
                player.sendOverlayMessage(Component.translatable("justarod.slime.not_owner")
                        .withStyle(ChatFormatting.RED));
                return InteractionResult.CONSUME;
            }
            int total = this.tier().internalSpace();
            int accepted = this.storage.addItem(stack, total);
            if (accepted > 0) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(accepted);
                }
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.playSound(null, this.blockPosition(), SoundEvents.SLIME_SQUISH_SMALL,
                            SoundSource.NEUTRAL, 0.7F, 1.2F);
                }
                player.sendOverlayMessage(Component.translatable("justarod.slime.item_in",
                        this.storage.usedSpace(), total).withStyle(ChatFormatting.GREEN));
            } else {
                player.sendOverlayMessage(Component.translatable("justarod.slime.no_space")
                        .withStyle(ChatFormatting.RED));
            }
            return InteractionResult.CONSUME;
        }

        // (5) 空手普通右键：主人钻进去；其他人只提示归属（不给开背包）
        if (stack.isEmpty()) {
            if (this.isOwner(player)) {
                this.swallowOwner(player);
                player.sendOverlayMessage(Component.translatable("justarod.slime.enter"));
            } else {
                player.sendOverlayMessage(Component.translatable("justarod.slime.not_owner")
                        .withStyle(ChatFormatting.RED));
            }
            return InteractionResult.CONSUME;
        }

        return super.mobInteract(player, hand);
    }

    /**
     * 喂分解催化物：消耗一份，开启一批分解（最多 {@link #decomposeBatchSize()} 组，每组 4 秒）。
     *
     * <p>批次没跑完之前拒绝新的催化物，并且**先判定再消耗**——被拒时物品不会被吃掉。
     * 右键喂与 GUI 按钮（{@code ACTION_FEED_CATALYST}）共用这一份规则。
     */
    public InteractionResult useCatalyst(Player player, ItemStack stack) {
        if (!this.isOwner(player)) {
            player.sendOverlayMessage(Component.translatable("justarod.slime.not_owner")
                    .withStyle(ChatFormatting.RED));
            return InteractionResult.CONSUME;
        }
        if (stack.isEmpty() || !stack.is(JRItems.Companion.getDECOMPOSE_CATALYST())) {
            player.sendOverlayMessage(Component.translatable("justarod.slime.decompose.need_catalyst")
                    .withStyle(ChatFormatting.YELLOW));
            return InteractionResult.CONSUME;
        }
        if (this.isDecomposing()) {
            player.sendOverlayMessage(Component.translatable("justarod.slime.decompose.busy",
                    this.getDecomposeTasks()).withStyle(ChatFormatting.YELLOW));
            return InteractionResult.CONSUME;
        }
        if (!this.storage.hasContent()) {
            player.sendOverlayMessage(Component.translatable("justarod.slime.decompose.empty")
                    .withStyle(ChatFormatting.YELLOW));
            return InteractionResult.CONSUME;
        }
        if (!this.addDecomposeTask()) {
            // 理论上到不了这里（上面已经判过 isDecomposing），保险起见不消耗物品
            return InteractionResult.CONSUME;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        this.level().playSound(null, this.blockPosition(), SoundEvents.BREWING_STAND_BREW,
                SoundSource.NEUTRAL, 0.9F, 0.9F);
        player.sendOverlayMessage(Component.translatable("justarod.slime.decompose.started",
                this.getDecomposeTasks()).withStyle(ChatFormatting.LIGHT_PURPLE));
        return InteractionResult.CONSUME;
    }

    /** 铃铛召回：把史莱姆传送到主人身边（可跨维度） */
    public void recallTo(Player player) {
        if (this.level() != player.level()) {
            if (player.level() instanceof ServerLevel target) {
                this.teleportTo(target, player.getX(), player.getY(), player.getZ(), java.util.Set.of(), player.getYRot(), player.getXRot(), false);
            }
        } else {
            this.teleportTo(player.getX(), player.getY(), player.getZ());
        }
        this.recallCooldown = 20 * 30;
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.BELL_RESONATE,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    player.getX(), player.getY() + 1.0D, player.getZ(), 20, 0.5D, 0.5D, 0.5D, 0.05D);
        }
        player.sendOverlayMessage(Component.translatable("justarod.slime.recalled")
                .withStyle(ChatFormatting.GREEN));
    }

    /** 打开体内容器（服务端）：只有主人能开，别处再调也拦得住 */
    protected void openContainer(Player player) {
        if (!this.isOwner(player)) return;
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            org.cneko.justarod.event.JRSlimeNetworking.open(serverPlayer, this);
        }
    }

    // ==================== 存档 ====================

    @Override
    public void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putBoolean("TameData", tameData);
        out.putInt("TierLevel", tierLevel);
        out.putInt("Trust", trust);
        out.putInt("Command", command);
        out.putInt("GrowthProgress", growthProgress);
        out.putInt("DecomposeTasks", decomposeTasks);
        out.putInt("DevourCooldown", devourCooldown);
        if (ownerUuid != null) {
            out.store("Owner", net.minecraft.core.UUIDUtil.CODEC, ownerUuid);
        }
        out.putString("OwnerName", ownerName);
        storage.save(out.child("SlimeStorage"));
    }

    @Override
    public void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setTameData(in.getBooleanOr("TameData", false));
        this.tierLevel = Math.max(0, Math.min(SlimeTier.MAX_LEVEL, in.getIntOr("TierLevel", 0)));
        entityData.set(DATA_TIER, tierLevel);
        this.trust = in.getIntOr("Trust", 0);
        this.command = in.getIntOr("Command", COMMAND_FOLLOW);
        this.growthProgress = in.getIntOr("GrowthProgress", 0);
        this.decomposeTasks = in.getIntOr("DecomposeTasks", 0);
        this.devourCooldown = Math.max(0, in.getIntOr("DevourCooldown", 0));
        this.ownerUuid = in.read("Owner", net.minecraft.core.UUIDUtil.CODEC).orElse(null);
        this.ownerName = in.getStringOr("OwnerName", "");
        this.entityData.set(DATA_OWNER_NAME, this.ownerName);
        storage.load(in.childOrEmpty("SlimeStorage"));
        storage.ensureSlots(SlimeTier.of(this.tierLevel).capacity());
        this.setSize(tierLevel + 1, false);
        this.applyHealthModifier();
        this.refreshDimensions();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide() && reason != RemovalReason.CHANGED_DIMENSION
                && reason != RemovalReason.UNLOADED_TO_CHUNK) {
            // 死亡 / 被清除：体内物品全部掉出，实体全部释放
            for (ItemStack stack : storage.drainItems()) {
                this.spawnAtLocation((ServerLevel) this.level(), stack);
            }
            releaseAllEntities();
        }
        super.remove(reason);
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity entity) {
        // 驯服后的史莱姆不再把生物弹开，避免把主人推开
    }

    @Override
    protected boolean isDealsDamage() {
        return this.isEffectiveAi() || this.getControllingPassenger() != null;
    }

    @Override
    public void playerTouch(Player player) {
        // 不因碰撞伤害玩家
    }
}
