package org.cneko.justarod.ascaris;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.cneko.justarod.api.VisualSized;
import org.cneko.justarod.config.JRConfig;
import org.cneko.justarod.entity.Pregnant;
import org.cneko.justarod.item.JRItems;
import org.cneko.justarod.packet.AscarisActionPayload;
import org.cneko.justarod.packet.AscarisVisualEventPayload;
import org.cneko.justarod.sound.JRSounds;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * 粉白蛔虫娘（Ascaris-chan）实体（v49：80 骨 / 26 动画 / 3 套眼部贴图变体 / 寄生系病娇系统）。
 * 包含软萌日常、驱虫药恐惧与高执念反杀、吃醋雷达盯梢驱赶、病娇缠绕（越挣扎越紧 & 喂糖哄好）、半夜床边凝视→醒来秒装乖。
 */
public class AscarisEntity extends TamableAnimal implements GeoEntity, VisualSized {

    public static final int WRAP_NONE = 0;
    public static final int WRAP_AFFECTIONATE = 1;
    public static final int WRAP_PARASITIC = 2;
    public static final int WRAP_YANDERE = 3;

    public static final int EXPELLED_NONE = 0;
    public static final int EXPELLED_PAGODA_WEAK = 1;
    public static final int EXPELLED_CAUTIOUS_CURLED = 2;
    public static final int EXPELLED_ALBENDAZOLE_CRITICAL = 3;

    public static final int EYE_DEFAULT = 0;
    public static final int EYE_YANDERE = 1;
    public static final int EYE_YANDERE_HEART = 2;
    public static final int EYE_AUTO = -1;

    private static final float BASE_HEIGHT = 1.43f;
    private static final EntityDimensions STAND_DIMS = EntityDimensions.scalable(0.55f, 1.43f).withEyeHeight(1.12f);
    private static final EntityDimensions CRAWL_DIMS = EntityDimensions.scalable(0.55f, 0.65f).withEyeHeight(0.42f);
    private static final DustParticleOptions PINK_DUST = new DustParticleOptions(0xFF9EC4, 0.9f);
    private static final DustParticleOptions WHITE_DUST = new DustParticleOptions(0xFFF0F6, 0.75f);
    private static final DustParticleOptions YANDERE_DUST = new DustParticleOptions(0xB8184A, 0.95f);

    // ===== GeckoLib 5.5 RawAnimation 缓存（全部 26 个动画） =====
    private static final RawAnimation ANIM_REST = RawAnimation.begin().thenLoop("animation.ascaris.rest");
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("animation.ascaris.idle");
    private static final RawAnimation ANIM_BLINK = RawAnimation.begin().thenLoop("animation.ascaris.blink");
    private static final RawAnimation ANIM_WALK = RawAnimation.begin().thenLoop("animation.ascaris.walk");
    private static final RawAnimation ANIM_RUN = RawAnimation.begin().thenLoop("animation.ascaris.run");
    private static final RawAnimation ANIM_CRAWL = RawAnimation.begin().thenLoop("animation.ascaris.crawl");
    private static final RawAnimation ANIM_SIT = RawAnimation.begin().thenPlayAndHold("animation.ascaris.sit");
    private static final RawAnimation ANIM_SIT_CHAIR = RawAnimation.begin().thenLoop("animation.ascaris.sit_chair");
    private static final RawAnimation ANIM_PRONE = RawAnimation.begin().thenLoop("animation.ascaris.prone");
    private static final RawAnimation ANIM_SLEEP = RawAnimation.begin().thenLoop("animation.ascaris.sleep");
    private static final RawAnimation ANIM_CURL = RawAnimation.begin()
            .thenPlay("animation.ascaris.curl_up")
            .thenLoop("animation.ascaris.curl_idle");
    private static final RawAnimation ANIM_COLLAPSE = RawAnimation.begin()
            .thenPlay("animation.ascaris.collapse")
            .thenLoop("animation.ascaris.collapse_idle");
    private static final RawAnimation ANIM_TAIL_WRAP = RawAnimation.begin()
            .thenPlay("animation.ascaris.tail_wrap")
            .thenLoop("animation.ascaris.tail_wrap_idle");
    private static final RawAnimation ANIM_WAVE = RawAnimation.begin().thenPlay("animation.ascaris.wave");
    private static final RawAnimation ANIM_HAPPY = RawAnimation.begin().thenLoop("animation.ascaris.happy");
    private static final RawAnimation ANIM_LOOK_AROUND = RawAnimation.begin().thenPlay("animation.ascaris.look_around");
    // v49 病娇专属 7 个动画
    private static final RawAnimation ANIM_YANDERE_STARE = RawAnimation.begin().thenLoop("animation.ascaris.yandere_stare");
    private static final RawAnimation ANIM_YANDERE_WRAP = RawAnimation.begin().thenPlayAndHold("animation.ascaris.yandere_wrap");
    private static final RawAnimation ANIM_YANDERE_WRAP_IDLE = RawAnimation.begin().thenLoop("animation.ascaris.yandere_wrap_idle");
    private static final RawAnimation ANIM_YANDERE_TIGHTEN = RawAnimation.begin().thenPlay("animation.ascaris.yandere_tighten");
    private static final RawAnimation ANIM_STALK = RawAnimation.begin().thenLoop("animation.ascaris.stalk");
    private static final RawAnimation ANIM_THREATEN = RawAnimation.begin().thenPlay("animation.ascaris.threaten");
    private static final RawAnimation ANIM_BED_WATCH = RawAnimation.begin().thenLoop("animation.ascaris.bed_watch");

    // ===== 同步数据 =====
    private static final EntityDataAccessor<Integer> DATA_POSE_STATE =
            SynchedEntityData.defineId(AscarisEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STATE_TICKS =
            SynchedEntityData.defineId(AscarisEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HUNGER =
            SynchedEntityData.defineId(AscarisEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_AFFECTION =
            SynchedEntityData.defineId(AscarisEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_OBSESSION =
            SynchedEntityData.defineId(AscarisEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_EYE_VARIANT =
            SynchedEntityData.defineId(AscarisEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_WRAP_TARGET_ID =
            SynchedEntityData.defineId(AscarisEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_WRAP_MODE =
            SynchedEntityData.defineId(AscarisEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_WRAP_STRUGGLE =
            SynchedEntityData.defineId(AscarisEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_CRAWLING =
            SynchedEntityData.defineId(AscarisEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_EXPELLED_STAGE =
            SynchedEntityData.defineId(AscarisEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_VISUAL_SCALE =
            SynchedEntityData.defineId(AscarisEntity.class, EntityDataSerializers.FLOAT);

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    private int transientTimer = 0;
    private int scaredCooldown = 0;
    private int wrapCooldown = 0;
    private int releaseGraceTicks = 0;
    private int greetCooldown = 0;
    private int threatenCooldown = 0;
    private int threatenStrikeTimer = 0;
    private boolean threatenAllowOwner = false;
    private int downedProtectionCooldown = 0;
    private int hungerTickCounter = 0;
    private int albendazoleToxicityTicks = 0;
    private int dewormerStareTicks = 0;
    private int yandereWrapTotalTicks = 0;
    private int yandereTightenTicks = 0;
    private int yandereStruggleTaps = 0;
    private int yandereHeadpatCalmCount = 0;
    private boolean yanderePlantedEgg = false;
    private boolean wasOwnerSleeping = false;
    private boolean manualPoseHold = false;
    private boolean forcedCrawlShowcase = false;
    private int eyeOverride = EYE_AUTO;
    private int ownerWasFarTicks = 0;
    private int autoEnterCooldown = 0;
    private int chestHopCooldown = 0;
    private int voiceCooldownTicks = 0;
    private int sleepVoiceCooldownTicks = 120;
    @Nullable
    private UUID bodyHostUuid = null;

    // ===== 胸部物理阻尼弹簧（双端同步计算：模型 bust 骨骼与坐在胸口上的缩小玩家实时同频晃动） =====
    private boolean springInitialized = false;
    private double lastSpringX = 0.0;
    private double lastSpringY = 0.0;
    private double lastSpringZ = 0.0;
    private float lastSpringYaw = 0f;
    private double lastSpringVelY = 0.0;
    private boolean wasSpringOnGround = true;

    private float bustPosY = 0f;
    private float prevBustPosY = 0f;
    private float bustVelY = 0f;
    private float bustPosX = 0f;
    private float prevBustPosX = 0f;
    private float bustVelX = 0f;
    private float bustStridePhase = 0f;
    private float prevBustStridePhase = 0f;
    private float bustStrideAmp = 0f;
    private float prevBustStrideAmp = 0f;

    public AscarisEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAscarisAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.SCALE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_POSE_STATE, AscarisPoseState.NORMAL.id());
        builder.define(DATA_STATE_TICKS, 0);
        builder.define(DATA_HUNGER, 80);
        builder.define(DATA_AFFECTION, 20);
        builder.define(DATA_OBSESSION, 15);
        builder.define(DATA_EYE_VARIANT, EYE_DEFAULT);
        builder.define(DATA_WRAP_TARGET_ID, -1);
        builder.define(DATA_WRAP_MODE, WRAP_NONE);
        builder.define(DATA_WRAP_STRUGGLE, 0f);
        builder.define(DATA_CRAWLING, false);
        builder.define(DATA_EXPELLED_STAGE, EXPELLED_NONE);
        builder.define(DATA_VISUAL_SCALE, 1.0f);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new YandereJealousyGoal(this));
        this.goalSelector.addGoal(2, new HungryAmbushWrapGoal(this));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.12, AscarisEntity::isSweetTreat, false));
        this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.15, 7.0f, 2.2f) {
            @Override
            public boolean canUse() {
                return !AscarisEntity.this.isMovementLocked() && !AscarisEntity.this.isOrderedToSit() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !AscarisEntity.this.isMovementLocked() && !AscarisEntity.this.isOrderedToSit() && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.72) {
            @Override
            public boolean canUse() {
                return !AscarisEntity.this.isMovementLocked() && !AscarisEntity.this.isOrderedToSit()
                        && AscarisEntity.this.getPoseState() != AscarisPoseState.STALK && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !AscarisEntity.this.isMovementLocked() && !AscarisEntity.this.isOrderedToSit()
                        && AscarisEntity.this.getPoseState() != AscarisPoseState.STALK && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 9.0f) {
            @Override
            public boolean canUse() {
                AscarisPoseState st = AscarisEntity.this.getPoseState();
                return st != AscarisPoseState.SLEEP && st != AscarisPoseState.COLLAPSE
                        && !st.isWrapping() && super.canUse();
            }
        });
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this) {
            @Override
            public boolean canUse() {
                return !AscarisEntity.this.isMovementLocked()
                        && AscarisEntity.this.getPoseState() != AscarisPoseState.STALK && super.canUse();
            }
        });
    }

    // ===== GeckoLib 5.5 控制器：主状态机（26 动画）+ 独立眨眼叠加轨道 =====

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<AscarisEntity>("main", 5, state -> {
            AscarisPoseState pose = this.getPoseState();
            return switch (pose) {
                case REST -> state.setAndContinue(ANIM_REST);
                case COLLAPSE -> state.setAndContinue(ANIM_COLLAPSE);
                case TAIL_WRAP -> state.setAndContinue(ANIM_TAIL_WRAP);
                case YANDERE_WRAP -> state.setAndContinue(ANIM_YANDERE_WRAP);
                case YANDERE_WRAP_IDLE -> state.setAndContinue(ANIM_YANDERE_WRAP_IDLE);
                case YANDERE_TIGHTEN -> state.setAndContinue(ANIM_YANDERE_TIGHTEN);
                case YANDERE_STARE -> state.setAndContinue(ANIM_YANDERE_STARE);
                case STALK -> state.setAndContinue(ANIM_STALK);
                case THREATEN -> state.setAndContinue(ANIM_THREATEN);
                case BED_WATCH -> state.setAndContinue(ANIM_BED_WATCH);
                case CURL_UP -> state.setAndContinue(ANIM_CURL);
                case SLEEP -> state.setAndContinue(ANIM_SLEEP);
                case SIT_CHAIR -> state.setAndContinue(ANIM_SIT_CHAIR);
                case SIT_GROUND -> state.setAndContinue(ANIM_SIT);
                case PRONE -> state.setAndContinue(ANIM_PRONE);
                case HAPPY -> state.setAndContinue(ANIM_HAPPY);
                case WAVE -> state.setAndContinue(ANIM_WAVE);
                case LOOK_AROUND -> state.setAndContinue(ANIM_LOOK_AROUND);
                case NORMAL -> {
                    if (this.isPassenger()) {
                        yield state.setAndContinue(ANIM_SIT_CHAIR);
                    }
                    if (this.isCrawling()) {
                        yield state.setAndContinue(ANIM_CRAWL);
                    }
                    double horizSpeedSqr = this.getDeltaMovement().horizontalDistanceSqr();
                    if (state.isMoving() || horizSpeedSqr > 0.0008) {
                        boolean fast = horizSpeedSqr > 0.022 || this.isSprinting();
                        yield state.setAndContinue(fast ? ANIM_RUN : ANIM_WALK);
                    }
                    yield state.setAndContinue(ANIM_IDLE);
                }
            };
        }));

        // 独立眨眼控制器：happy / collapse / sleep / 害怕>~< curl_up / 全部病娇不眨眼死盯期间暂停
        controllers.add(new AnimationController<AscarisEntity>("blink", 2, state -> {
            if (this.getPoseState().blocksBlink()) {
                return PlayState.STOP;
            }
            return state.setAndContinue(ANIM_BLINK);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animCache;
    }

    // ===== 碰撞箱与体型缩放 =====

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        EntityDimensions base = this.isCrawling() ? CRAWL_DIMS : STAND_DIMS;
        float vs = this.getVisualScale();
        return Math.abs(vs - 1.0f) < 0.01f ? base : base.scale(vs);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_CRAWLING.equals(key) || DATA_VISUAL_SCALE.equals(key)) {
            this.refreshDimensions();
        }
    }

    public float getEffectiveScale() {
        float attrScale = (float) this.getAttributeValue(Attributes.SCALE);
        return Mth.clamp(attrScale * this.getVisualScale(), 0.25f, 4.5f);
    }

    @Override
    public float visualSize() {
        return BASE_HEIGHT * getEffectiveScale();
    }

    // ===== 缠绕及松开缓冲期内彻底关闭实体间互推，防止挤开抖动或松开瞬间突兀弹飞 =====

    @Override
    public boolean isPushable() {
        return !getPoseState().isWrapping() && releaseGraceTicks <= 0 && super.isPushable();
    }

    @Override
    protected void doPush(Entity entity) {
        if (getPoseState().isWrapping() || releaseGraceTicks > 0) {
            return;
        }
        super.doPush(entity);
    }

    @Override
    public void push(Entity entity) {
        if (getPoseState().isWrapping() || releaseGraceTicks > 0) {
            return;
        }
        super.push(entity);
    }

    /**
     * 计算双人尾巴缠绕的目标站位点（服务端与客户端共用同一公式，避免拉扯回弹）。
     * 对应 manifest.json demo_player 参数：位于蛔虫娘左侧并随 1.74s~2.86s 平滑贴近。
     */
    public Vec3 computeWrapAnchor() {
        AscarisPoseState st = getPoseState();
        float gathered;
        if (st == AscarisPoseState.YANDERE_WRAP_IDLE || st == AscarisPoseState.YANDERE_TIGHTEN) {
            gathered = 1.0f;
        } else {
            float elapsedSec = entityData.get(DATA_STATE_TICKS) / 20.0f;
            gathered = smoothstep(1.74f, 2.86f, elapsedSec);
        }
        float scale = getEffectiveScale();
        float yawRad = this.yBodyRot * Mth.DEG_TO_RAD;
        double fwdX = -Math.sin(yawRad);
        double fwdZ = Math.cos(yawRad);
        double leftX = Math.cos(yawRad);
        double leftZ = Math.sin(yawRad);
        // 病娇收紧瞬间稍微再贴近 0.04 格，压迫感更足
        double tightPull = (st == AscarisPoseState.YANDERE_TIGHTEN) ? 0.52 : 0.56;
        double sideDist = Mth.lerp(gathered, 0.72, tightPull) * scale;
        double fwdDist = 0.04 * scale;
        return new Vec3(
                this.getX() + leftX * sideDist + fwdX * fwdDist,
                this.getY(),
                this.getZ() + leftZ * sideDist + fwdZ * fwdDist
        );
    }

    // ===== 缩小玩家胸口骑乘 & 实时胸部弹簧位置同步 =====

    public boolean hasChestPassenger() {
        return !this.getPassengers().isEmpty();
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().isEmpty()
                && passenger instanceof Player p
                && org.cneko.justarod.scale.CuteScaleController.isSmallEnoughForChest(p)
                && !getPoseState().isWrapping()
                && getPoseState() != AscarisPoseState.COLLAPSE;
    }

    /** 将已缩小的玩家轻轻捧起放在她的胸口上 */
    public boolean mountShrunkPlayerOnChest(Player player) {
        if (level().isClientSide() || player == null || !player.isAlive()) return false;
        if (!org.cneko.justarod.scale.CuteScaleController.isSmallEnoughForChest(player)) return false;
        if (getPoseState().isWrapping() || getPoseState() == AscarisPoseState.COLLAPSE) return false;
        if (this.isCrawling()) setCrawling(false);
        this.setOrderedToSit(false);
        this.manualPoseHold = false;
        if (getPoseState() != AscarisPoseState.NORMAL && getPoseState() != AscarisPoseState.HAPPY) {
            setPoseState(AscarisPoseState.NORMAL);
        }
        boolean ok = player.startRiding(this, true, true);
        if (ok) {
            addBustImpulse(-0.68f);
            playVoice(JRSounds.ASCARIS_VOICE_HAPPY, 0.9f, 36, true);
            level().playSound(null, blockPosition(), JRSounds.ASCARIS_BELL, SoundSource.NEUTRAL, 0.65f, 1.25f);
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.HEART, getX(), getY() + 1.15, getZ(), 6, 0.25, 0.2, 0.25, 0.02);
            }
            if (player instanceof ServerPlayer sp) {
                sp.sendOverlayMessage(Component.literal("§d她把小小的你轻轻捧到了胸口上~♡ §7（[空格] 轻跳 · [Shift] 下来）"));
            }
        }
        return ok;
    }

    /** 坐在胸口上的缩小玩家按空格：让粉白蛔虫娘开心地原地蹦跳一下，触发明显柔软弹性晃动 */
    public void handleChestHopFromPassenger(ServerPlayer player) {
        if (!this.hasPassenger(player) || chestHopCooldown > 0) return;
        if (this.onGround()) {
            this.jumpFromGround();
            this.addBustImpulse(-0.55f);
            this.chestHopCooldown = 14;
            playVoice(JRSounds.ASCARIS_VOICE_HOP, 0.88f, 22, false);
            level().playSound(null, blockPosition(), JRSounds.ASCARIS_BELL, SoundSource.NEUTRAL, 0.55f, 1.3f);
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.HEART, getX(), getY() + 1.2, getZ(), 4, 0.25, 0.2, 0.25, 0.02);
            }
        }
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return computeChestOffset(passenger, getEffectiveScale(), 1.0f);
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        if (!this.hasPassenger(passenger)) return;
        float scale = getEffectiveScale();
        Vec3 chestOffset = computeChestOffset(passenger, scale, 1.0f);
        double renderedButtOffset = computeRenderedSittingButtOffset(passenger);

        double rx = this.getX() + chestOffset.x;
        double ry = this.getY() + chestOffset.y - renderedButtOffset;
        double rz = this.getZ() + chestOffset.z;
        callback.accept(passenger, rx, ry, rz);
    }

    /**
     * 计算粉白蛔虫娘当前姿态与胸部弹簧瞬态下的真实胸口上沿锚点（相对实体脚底的世界坐标偏移）。
     * 对应 ascaris.geo.json 中 bust_left/bust_right 实体方块上沿 y ∈ [12.61, 14.44] (0.79~0.90 格)、前突 z = -2.69 (-0.168 格)。
     */
    private Vec3 computeChestOffset(Entity passenger, float scale, float partialTick) {
        float yawRad = this.yBodyRot * Mth.DEG_TO_RAD;
        double fwdX = -Math.sin(yawRad);
        double fwdZ = Math.cos(yawRad);
        double rightX = -Math.cos(yawRad);
        double rightZ = -Math.sin(yawRad);

        AscarisPoseState st = getPoseState();
        double baseChestY = switch (st) {
            case SIT_GROUND -> 0.52;
            case SIT_CHAIR -> 0.76;
            case PRONE, SLEEP -> 0.28;
            default -> 0.865;
        };
        float riderRenderScale = getPassengerRenderScale(passenger);
        double baseForward = (st == AscarisPoseState.PRONE || st == AscarisPoseState.SLEEP)
                ? 0.24 * scale
                : (0.175 * scale + 0.050 * riderRenderScale);

        // 实时叠加胸部弹簧位移（与 AscarisRenderer 中 bust 骨骼位移完全同频！）
        float by = getBustBounceY(partialTick);
        float bx = getBustSwayX(partialTick);
        double bounceWorldY = by * 0.055 * scale;
        double swayWorldRight = bx * 0.036 * scale;
        double pitchWorldFwd = -by * 0.020 * scale;

        return new Vec3(
                fwdX * (baseForward + pitchWorldFwd) + rightX * swayWorldRight,
                baseChestY * scale + bounceWorldY,
                fwdZ * (baseForward + pitchWorldFwd) + rightZ * swayWorldRight
        );
    }

    /**
     * 计算坐姿玩家渲染后的臀部相对实体脚底 (entity.getY()) 的真实高度偏移：
     * - 兼容原版 PlayerModel (1.501 - 14/16) * 0.9375 ≈ 0.587 * scale；
     * - 同时精确兼容前置模组 toNeko 的 toneko$loliScale 二次缩放，彻底避免被原生 attach.y 拉低到腿上！
     */
    private static float getPassengerRenderScale(Entity passenger) {
        if (!(passenger instanceof LivingEntity living)) return 1.0f;
        float s = living.getScale();
        if (passenger instanceof org.cneko.toneko.common.mod.entities.INeko neko && neko.isNeko()) {
            try {
                if (org.cneko.toneko.common.util.ConfigUtil.isLoliHeadEnabled()) {
                    double ageScale = neko.getNekoAgeScale();
                    if (ageScale > 0.05 && ageScale < 1.0) {
                        s *= (float) ageScale;
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return s;
    }

    private static double computeRenderedSittingButtOffset(Entity passenger) {
        return 0.587 * getPassengerRenderScale(passenger);
    }

    public void addBustImpulse(float impulseY) {
        this.bustVelY = Mth.clamp(this.bustVelY + impulseY, -1.6f, 1.6f);
    }

    private void tickBustPhysics() {
        this.prevBustPosY = this.bustPosY;
        this.prevBustPosX = this.bustPosX;
        this.prevBustStridePhase = this.bustStridePhase;
        this.prevBustStrideAmp = this.bustStrideAmp;

        if (!springInitialized) {
            this.lastSpringX = getX();
            this.lastSpringY = getY();
            this.lastSpringZ = getZ();
            this.lastSpringYaw = this.yBodyRot;
            this.lastSpringVelY = 0.0;
            this.wasSpringOnGround = this.onGround();
            this.springInitialized = true;
            return;
        }

        double vx = getX() - lastSpringX;
        double vy = getY() - lastSpringY;
        double vz = getZ() - lastSpringZ;
        double horizSpeed = Math.hypot(vx, vz);
        float dyaw = Mth.wrapDegrees(this.yBodyRot - lastSpringYaw);

        // 1. 跳跃与垂直加速度惯性（起跳瞬间胸口轻微滞后下沉再上弹，落地瞬间柔软缓冲回弹）
        double ay = vy - lastSpringVelY;
        if (Math.abs(ay) > 0.02) {
            this.bustVelY -= (float) Mth.clamp(ay * 2.1, -0.75, 0.75);
        }
        if (!wasSpringOnGround && this.onGround() && lastSpringVelY < -0.06) {
            float impact = (float) Mth.clamp(-lastSpringVelY * 2.2, 0.16, 0.95);
            this.bustVelY -= impact;
        }

        // 2. 走路 / 跑步时的双频步伐弹性律动 + 左右轻微交替摇摆
        boolean carrying = hasChestPassenger();
        if (horizSpeed > 0.012 && !getPoseState().locksLocomotion()) {
            float speedNorm = (float) Mth.clamp(horizSpeed * 7.5, 0.25, 1.75);
            this.bustStridePhase += 0.42f * speedNorm;
            this.bustStrideAmp = Mth.lerp(0.25f, this.bustStrideAmp, Mth.clamp(speedNorm * 0.72f, 0f, 1.15f));
            float stepImpulse = Mth.sin(this.bustStridePhase * 2.0f) * 0.11f * speedNorm;
            this.bustVelY += stepImpulse;
            this.bustVelX += Mth.cos(this.bustStridePhase) * 0.055f * speedNorm;
        } else {
            this.bustStrideAmp *= 0.82f;
        }

        // 3. 转身离心横向晃动
        if (Math.abs(dyaw) > 0.4f) {
            this.bustVelX -= Mth.clamp(dyaw * 0.014f, -0.35f, 0.35f);
        }

        // 4. 静止柔和呼吸微浮
        float breathTargetY = Mth.sin(this.tickCount * 0.14f) * 0.055f + (carrying ? -0.10f : 0f);

        // 5. 二阶弹簧阻尼积分（承载缩小玩家时略微更软更有Q弹缓冲感）
        float stiffness = carrying ? 0.27f : 0.34f;
        float damping = carrying ? 0.77f : 0.74f;
        this.bustVelY = (this.bustVelY + (breathTargetY - this.bustPosY) * stiffness) * damping;
        this.bustPosY = Mth.clamp(this.bustPosY + this.bustVelY, -0.92f, 0.92f);

        this.bustVelX = (this.bustVelX - this.bustPosX * 0.30f) * 0.75f;
        this.bustPosX = Mth.clamp(this.bustPosX + this.bustVelX, -0.60f, 0.60f);

        this.lastSpringX = getX();
        this.lastSpringY = getY();
        this.lastSpringZ = getZ();
        this.lastSpringYaw = this.yBodyRot;
        this.lastSpringVelY = vy;
        this.wasSpringOnGround = this.onGround();
    }

    public float getBustBounceY(float partialTick) {
        return Mth.lerp(partialTick, this.prevBustPosY, this.bustPosY);
    }

    public float getBustSwayX(float partialTick) {
        return Mth.lerp(partialTick, this.prevBustPosX, this.bustPosX);
    }

    public float getBustStrideAlt(float partialTick) {
        float phase = Mth.lerp(partialTick, this.prevBustStridePhase, this.bustStridePhase);
        float amp = Mth.lerp(partialTick, this.prevBustStrideAmp, this.bustStrideAmp);
        return Mth.sin(phase) * amp;
    }

    public void setAutoEnterCooldown(int ticks) {
        this.autoEnterCooldown = Math.max(this.autoEnterCooldown, ticks);
    }

    /** 统一播放粉白蛔虫娘语气词（自动防叠音冷却、微妙自然音高抖动、严格尊重病娇开关） */
    public void playVoice(SoundEvent sound, float volume, int cooldownTicks, boolean force) {
        if (this.level().isClientSide() || sound == null) return;
        if (!force && this.voiceCooldownTicks > 0) return;
        SoundEvent actual = sound;
        if (!JRConfig.isAscarisYandereEnabled()) {
            if (sound == JRSounds.ASCARIS_VOICE_YANDERE_GIGGLE || sound == JRSounds.ASCARIS_VOICE_YANDERE_CLING) {
                actual = JRSounds.ASCARIS_VOICE_HAPPY;
            } else if (sound == JRSounds.ASCARIS_VOICE_JEALOUS) {
                actual = JRSounds.ASCARIS_VOICE_HISS;
            }
        }
        float pitch = 0.97f + this.random.nextFloat() * 0.06f;
        this.level().playSound(null, this.blockPosition(), actual, SoundSource.VOICE, volume, pitch);
        this.voiceCooldownTicks = Math.max(force ? 10 : this.voiceCooldownTicks, cooldownTicks);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return JRSounds.ASCARIS_VOICE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return JRSounds.ASCARIS_VOICE_WHIMPER;
    }

    @Override
    protected float getSoundVolume() {
        return 0.92f;
    }

    // ===== 每 Tick 主逻辑 =====

    @Override
    public void tick() {
        super.tick();
        tickBustPhysics();
        if (this.level().isClientSide()) {
            tickClientParticles();
            return;
        }
        tickServer();
    }

    private void tickClientParticles() {
        AscarisPoseState pose = getPoseState();
        int eye = getEyeVariant();
        if (pose == AscarisPoseState.COLLAPSE && tickCount % 8 == 0) {
            level().addParticle(WHITE_DUST,
                    getX() + (random.nextDouble() - 0.5) * 0.6,
                    getY() + 0.25,
                    getZ() + (random.nextDouble() - 0.5) * 0.6,
                    0, 0.02, 0);
        } else if (pose.isWrapping() && tickCount % 6 == 0) {
            int mode = getWrapMode();
            if (mode == WRAP_YANDERE) {
                level().addParticle(YANDERE_DUST,
                        getX() + (random.nextDouble() - 0.5) * 0.9,
                        getY() + 0.65 + random.nextDouble() * 0.6,
                        getZ() + (random.nextDouble() - 0.5) * 0.9,
                        0, 0.025, 0);
                if (tickCount % 18 == 0) {
                    level().addParticle(ParticleTypes.HEART,
                            getX() + (random.nextDouble() - 0.5) * 0.6,
                            getY() + 1.25,
                            getZ() + (random.nextDouble() - 0.5) * 0.6,
                            0, 0.02, 0);
                }
            } else {
                level().addParticle(mode == WRAP_AFFECTIONATE ? ParticleTypes.HEART : PINK_DUST,
                        getX() + (random.nextDouble() - 0.5) * 0.9,
                        getY() + 0.6 + random.nextDouble() * 0.6,
                        getZ() + (random.nextDouble() - 0.5) * 0.9,
                        0, 0.03, 0);
            }
        } else if ((eye == EYE_YANDERE || eye == EYE_YANDERE_HEART) && tickCount % 16 == 0) {
            level().addParticle(YANDERE_DUST,
                    getX() + (random.nextDouble() - 0.5) * 0.55,
                    getY() + 1.05 + random.nextDouble() * 0.3,
                    getZ() + (random.nextDouble() - 0.5) * 0.55,
                    0, 0.015, 0);
        } else if (pose == AscarisPoseState.SLEEP && tickCount % 28 == 0) {
            level().addParticle(WHITE_DUST, getX(), getY() + 0.55, getZ(), 0, 0.02, 0);
        }
    }

    private void tickServer() {
        ServerLevel serverLevel = (ServerLevel) this.level();
        entityData.set(DATA_STATE_TICKS, entityData.get(DATA_STATE_TICKS) + 1);

        if (scaredCooldown > 0) scaredCooldown--;
        if (wrapCooldown > 0) wrapCooldown--;
        if (releaseGraceTicks > 0) releaseGraceTicks--;
        if (greetCooldown > 0) greetCooldown--;
        if (threatenCooldown > 0) threatenCooldown--;
        if (downedProtectionCooldown > 0) downedProtectionCooldown--;
        if (autoEnterCooldown > 0) autoEnterCooldown--;
        if (chestHopCooldown > 0) chestHopCooldown--;
        if (voiceCooldownTicks > 0) voiceCooldownTicks--;
        if (sleepVoiceCooldownTicks > 0) {
            sleepVoiceCooldownTicks--;
        } else if (getPoseState() == AscarisPoseState.SLEEP || getPoseState() == AscarisPoseState.REST) {
            playVoice(JRSounds.ASCARIS_VOICE_SLEEPY, 0.55f, 80, false);
            sleepVoiceCooldownTicks = 240 + random.nextInt(160);
        }

        // 胸口骑乘保护：若缩小玩家长大到托不住了，温柔放回地面
        if (!this.getPassengers().isEmpty()) {
            Entity first = this.getFirstPassenger();
            if (first instanceof Player rider
                    && org.cneko.justarod.scale.CuteScaleController.getCuteScale(rider) > org.cneko.justarod.scale.CuteScaleController.CHEST_RIDE_DISMOUNT_SCALE
                    && rider.getScale() > org.cneko.justarod.scale.CuteScaleController.CHEST_RIDE_DISMOUNT_SCALE) {
                rider.stopRiding();
                if (rider instanceof ServerPlayer sp) {
                    sp.sendOverlayMessage(Component.literal("§d你变大了，胸口快托不住啦，她只好轻轻把你放回地面~"));
                }
            }
        }

        // 自动钻回主人体内（残血避险 / 病娇高执念主动贴身钻入）
        if (this.tickCount % 20 == 0 && tickAutoReentry(serverLevel)) {
            return;
        }

        // 哈气/甩尾威慑击退定时判定（卡在甩尾前扫瞬间，打击感更足）
        if (threatenStrikeTimer > 0) {
            threatenStrikeTimer--;
            if (threatenStrikeTimer == 0) {
                performThreatenKnockback(serverLevel);
            }
        }

        // 若在配置中关闭了病娇开关，立刻温和退出一切病娇姿态/病娇缠绕并恢复亮瞳
        if (!JRConfig.isAscarisYandereEnabled()) {
            if (getWrapMode() == WRAP_YANDERE) {
                releaseTailWrap(false);
            } else if (getPoseState().isYanderePose() && !manualPoseHold) {
                setPoseState(this.isOrderedToSit() ? pickRestSitPose() : AscarisPoseState.NORMAL);
            }
        }

        // 0. 实时同步病娇眼部贴图变体
        updateEyeVariant();

        // 1. 瞬态动作计时器
        if (transientTimer > 0) {
            transientTimer--;
            if (transientTimer == 0 && !manualPoseHold) {
                AscarisPoseState cur = getPoseState();
                if (cur == AscarisPoseState.WAVE || cur == AscarisPoseState.HAPPY
                        || cur == AscarisPoseState.LOOK_AROUND || cur == AscarisPoseState.THREATEN
                        || (cur == AscarisPoseState.CURL_UP && getExpelledStage() == EXPELLED_NONE && !seesDewormerNearby())
                        || (cur == AscarisPoseState.YANDERE_STARE && !seesDewormerNearby())) {
                    setPoseState(this.isOrderedToSit() ? pickRestSitPose() : AscarisPoseState.NORMAL);
                }
            }
        }

        // 2. 阿苯达唑强排残留药毒（硬核抢救窗口）
        if (getExpelledStage() == EXPELLED_ALBENDAZOLE_CRITICAL && albendazoleToxicityTicks > 0) {
            albendazoleToxicityTicks--;
            if (albendazoleToxicityTicks % 30 == 0) {
                serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.35, getZ(), 4, 0.25, 0.15, 0.25, 0.01);
                this.hurtServer(serverLevel, damageSources().magic(), 1.0f);
            }
        }

        // 3. 尾巴缠绕持续逻辑（普通 / 寄生 / 病娇）
        if (getPoseState().isWrapping()) {
            tickTailWrap(serverLevel);
            return;
        }

        // 4. 锁定位姿时静止移动
        if (isMovementLocked()) {
            this.getNavigation().stop();
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.3, 1.0, 0.3));
        }

        // 5. 饱食度自然消耗 & 冷落执念积累
        if (isTame() && getPoseState() != AscarisPoseState.COLLAPSE) {
            hungerTickCounter++;
            if (hungerTickCounter >= 300) {
                hungerTickCounter = 0;
                if (getHunger() > 0) setHunger(getHunger() - 1);
                if (getHunger() <= 15 && getPoseState() == AscarisPoseState.NORMAL) {
                    serverLevel.playSound(null, blockPosition(), JRSounds.ASCARIS_RUMBLE, SoundSource.NEUTRAL, 0.45f, 1.25f);
                }
                // 被丢在原地冷落或独自离开很远会悄悄涨执念值（仅病娇配置启用时）
                if (JRConfig.isAscarisYandereEnabled()) {
                    LivingEntity owner = getOwner();
                    if (isOrderedToSit() || (owner != null && distanceToSqr(owner) > 196.0)) {
                        setObsession(getObsession() + 2);
                    }
                }
            }
        }

        // 6. 驱虫药双面反差：低执念害怕缩成团 (>~<)，高执念/高亲密则黑化凝视甚至直接扑上来截停！
        if (tickCount % 5 == 0 && !manualPoseHold) {
            checkDewormerReaction(serverLevel);
        }

        // 7. 自动 1 格狭缝爬行检测
        if (tickCount % 4 == 0) {
            updateCrawlingState();
        }

        // 8. 自然生活氛围 & 半夜病娇床边凝视（bed_watch → 醒来瞬间装乖 wave！）
        if (tickCount % 10 == 0 && !manualPoseHold && getExpelledStage() == EXPELLED_NONE) {
            tickSleepAndBedWatch(serverLevel);
        }
        if (tickCount % 20 == 0 && !manualPoseHold && getExpelledStage() == EXPELLED_NONE) {
            tickAmbientBehaviors(serverLevel);
        }

        // 9. 体内维度宿主绑定同步
        if (bodyHostUuid != null && tickCount % 40 == 0) {
            BodyAscarisBridge.tickResident(serverLevel, this, bodyHostUuid);
        }
    }

    /** 自动根据姿态、执念值与配置开关计算当前眼部贴图变体 */
    private void updateEyeVariant() {
        if (eyeOverride != EYE_AUTO) {
            entityData.set(DATA_EYE_VARIANT, eyeOverride);
            return;
        }
        if (!JRConfig.isAscarisYandereEnabled()) {
            if (entityData.get(DATA_EYE_VARIANT) != EYE_DEFAULT) {
                entityData.set(DATA_EYE_VARIANT, EYE_DEFAULT);
            }
            return;
        }
        AscarisPoseState pose = getPoseState();
        int desired;
        if (pose == AscarisPoseState.YANDERE_WRAP
                || pose == AscarisPoseState.YANDERE_WRAP_IDLE
                || pose == AscarisPoseState.YANDERE_TIGHTEN
                || pose == AscarisPoseState.THREATEN) {
            desired = EYE_YANDERE_HEART;
        } else if (pose == AscarisPoseState.YANDERE_STARE
                || pose == AscarisPoseState.STALK
                || pose == AscarisPoseState.BED_WATCH) {
            desired = (getObsession() >= 80) ? EYE_YANDERE_HEART : EYE_YANDERE;
        } else if (pose == AscarisPoseState.HAPPY
                || pose == AscarisPoseState.WAVE
                || pose == AscarisPoseState.CURL_UP
                || pose == AscarisPoseState.COLLAPSE
                || pose == AscarisPoseState.SLEEP) {
            desired = EYE_DEFAULT;
        } else if (getObsession() >= 65) {
            desired = EYE_YANDERE;
        } else {
            desired = EYE_DEFAULT;
        }
        if (entityData.get(DATA_EYE_VARIANT) != desired) {
            entityData.set(DATA_EYE_VARIANT, desired);
        }
    }

    /** 驱虫药双面反差：低执念害怕 >~< 蜷缩，高执念/高亲密黑化歪头凝视并在贴近时扑上来打断服药！ */
    private void checkDewormerReaction(ServerLevel level) {
        AscarisPoseState cur = getPoseState();
        if (cur == AscarisPoseState.COLLAPSE || cur.isWrapping() || cur == AscarisPoseState.SLEEP || cur == AscarisPoseState.BED_WATCH) {
            dewormerStareTicks = 0;
            return;
        }
        Player holder = findNearbyDewormerHolder(6.5);
        if (holder == null) {
            dewormerStareTicks = 0;
            return;
        }

        boolean yandereIntercept = JRConfig.isAscarisYandereEnabled()
                && isTame() && isOwnedBy(holder) && (getObsession() >= 45 || getAffection() >= 65);
        if (yandereIntercept) {
            // 黑化截停路线！
            dewormerStareTicks += 5;
            this.getLookControl().setLookAt(holder, 30f, 30f);
            if (cur != AscarisPoseState.YANDERE_STARE) {
                setPoseState(AscarisPoseState.YANDERE_STARE);
                transientTimer = 90;
                setObsession(getObsession() + 12);
                if (scaredCooldown == 0) {
                    scaredCooldown = 80;
                    level.playSound(null, blockPosition(), JRSounds.ASCARIS_BELL, SoundSource.NEUTRAL, 0.65f, 0.68f);
                    level.playSound(null, blockPosition(), JRSounds.ASCARIS_HEARTBEAT, SoundSource.NEUTRAL, 0.65f, 1.0f);
                    playVoice(JRSounds.ASCARIS_VOICE_YANDERE_GIGGLE, 0.9f, 55, true);
                    if (holder instanceof ServerPlayer sp) {
                        sp.sendSystemMessage(Component.literal("§c§o「……又拿出那种想把我们分开的糖了吗？这次，绝不会让你吃下去了哦♡」"));
                    }
                }
            } else {
                transientTimer = Math.max(transientTimer, 40);
            }
            // 继续举着驱虫药凑近超过 1.8 秒 -> 爱心瞳直接扑上去病娇缠绕截停！
            if (dewormerStareTicks >= 35 && wrapCooldown == 0 && distanceToSqr(holder) <= 14.0) {
                dewormerStareTicks = 0;
                startTailWrap(holder, WRAP_YANDERE);
            }
            return;
        }

        // 普通胆小路线：吓得后退半步 + >~< 害怕表情缩成粉白团子
        dewormerStareTicks = 0;
        if (cur != AscarisPoseState.CURL_UP) {
            Vec3 away = this.position().subtract(holder.position());
            double len = away.horizontalDistance();
            if (len > 0.05) {
                this.setDeltaMovement(away.x / len * 0.22, 0.12, away.z / len * 0.22);
            }
            setPoseState(AscarisPoseState.CURL_UP);
            transientTimer = 100;
            if (scaredCooldown == 0) {
                scaredCooldown = 80;
                level.playSound(null, blockPosition(), JRSounds.ASCARIS_BELL, SoundSource.NEUTRAL, 0.6f, 1.45f);
                playVoice(JRSounds.ASCARIS_VOICE_WHIMPER, 0.88f, 65, true);
                level.sendParticles(WHITE_DUST, getX(), getY() + 0.9, getZ(), 8, 0.35, 0.3, 0.35, 0.03);
                if (holder instanceof ServerPlayer sp) {
                    sp.sendOverlayMessage(Component.literal("§d粉白蛔虫娘看到你手里的驱虫药，吓得 >~< 缩成了一团发抖……"));
                }
            }
        } else {
            transientTimer = Math.max(transientTimer, 40);
        }
    }

    /** 半夜床边凝视（bed_watch）→ 主人醒来瞬间无缝切回亮瞳并乖巧挥手（wave）！ */
    private void tickSleepAndBedWatch(ServerLevel level) {
        LivingEntity owner = getOwner();
        if (!(owner instanceof Player player)) {
            wasOwnerSleeping = false;
            return;
        }
        boolean sleepingNow = player.isSleeping();
        if (sleepingNow) {
            if (distanceToSqr(player) <= 64.0 && getPoseState() != AscarisPoseState.BED_WATCH && getPoseState() != AscarisPoseState.SLEEP) {
                if (JRConfig.isAscarisYandereEnabled() && getObsession() >= 40) {
                    // 高执念：悄悄贴在床边趴着一眨不眨死盯！
                    setPoseState(AscarisPoseState.BED_WATCH);
                    this.getLookControl().setLookAt(player, 30f, 30f);
                    playVoice(JRSounds.ASCARIS_VOICE_YANDERE_GIGGLE, 0.65f, 70, false);
                } else {
                    setPoseState(AscarisPoseState.SLEEP);
                    playVoice(JRSounds.ASCARIS_VOICE_SLEEPY, 0.65f, 70, false);
                    sleepVoiceCooldownTicks = 240;
                }
            }
        } else if (wasOwnerSleeping) {
            // 主人刚从床上醒来：若她正顶着暗瞳床边凝视，瞬间装乖挥手！
            if (getPoseState() == AscarisPoseState.BED_WATCH) {
                playTransientPose(AscarisPoseState.WAVE, 56);
                entityData.set(DATA_EYE_VARIANT, EYE_DEFAULT);
                playVoice(JRSounds.ASCARIS_VOICE_GREET, 0.88f, 40, true);
                if (player instanceof ServerPlayer sp) {
                    sp.sendSystemMessage(Component.literal("§d「早上好呀~♡ 昨晚睡得香吗？」§7（她瞬间收起了暗红的目光朝你挥手，但身后的尾尖还在轻轻发颤……）"));
                }
            } else if (getPoseState() == AscarisPoseState.SLEEP) {
                setPoseState(isOrderedToSit() ? pickRestSitPose() : AscarisPoseState.NORMAL);
            }
        }
        wasOwnerSleeping = sleepingNow;
    }

    private boolean seesDewormerNearby() {
        return findNearbyDewormerHolder(6.5) != null;
    }

    @Nullable
    private Player findNearbyDewormerHolder(double radius) {
        List<Player> players = level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(radius),
                p -> p.isAlive() && !p.isSpectator() && isHoldingDewormer(p));
        return players.isEmpty() ? null : players.get(0);
    }

    public static boolean isHoldingDewormer(Player player) {
        return isDewormer(player.getMainHandItem()) || isDewormer(player.getOffhandItem());
    }

    public static boolean isDewormer(ItemStack stack) {
        return stack.is(JRItems.Companion.getPAGODA_CANDY()) || stack.is(JRItems.Companion.getALBENDAZOLE());
    }

    public static boolean isSweetTreat(ItemStack stack) {
        return stack.is(Items.SUGAR)
                || stack.is(Items.SWEET_BERRIES)
                || stack.is(Items.COOKIE)
                || stack.is(Items.HONEY_BOTTLE)
                || stack.is(Items.CAKE)
                || stack.is(Items.PUMPKIN_PIE)
                || stack.is(Items.APPLE)
                || stack.is(Items.GLOW_BERRIES)
                || stack.is(Items.MELON_SLICE);
    }

    private void updateCrawlingState() {
        if (forcedCrawlShowcase) {
            setCrawling(true);
            return;
        }
        if (getPoseState() != AscarisPoseState.NORMAL || isPassenger() || hasChestPassenger()) {
            setCrawling(false);
            return;
        }
        BlockPos headPos = BlockPos.containing(getX(), getY() + 1.05, getZ());
        boolean ceilingLow = isSolidLowCeiling(headPos);
        boolean aheadLow = false;
        if (this.getDeltaMovement().horizontalDistanceSqr() > 0.001) {
            Vec3 look = Vec3.directionFromRotation(0, this.getYRot()).scale(0.65);
            BlockPos aheadFoot = BlockPos.containing(getX() + look.x, getY() + 0.1, getZ() + look.z);
            BlockPos aheadHead = aheadFoot.above();
            if (!level().getBlockState(aheadFoot).blocksMotion() && isSolidLowCeiling(aheadHead)) {
                aheadLow = true;
            }
        }
        boolean hungryStalk = isTame() && getHunger() < 22 && getOwner() != null && distanceToSqr(getOwner()) < 64.0 && distanceToSqr(getOwner()) > 3.0;
        setCrawling(ceilingLow || aheadLow || hungryStalk);
    }

    private boolean isSolidLowCeiling(BlockPos pos) {
        BlockState state = level().getBlockState(pos);
        return !state.isAir() && state.blocksMotion();
    }

    private void tickAmbientBehaviors(ServerLevel level) {
        AscarisPoseState cur = getPoseState();
        LivingEntity owner = getOwner();

        // 夜晚非病娇待命时自动趴睡
        long tod = level.getOverworldClockTime() % 24000L;
        boolean isNight = tod >= 13000L && tod <= 23000L;
        if (isNight && isOrderedToSit() && (!JRConfig.isAscarisYandereEnabled() || getObsession() < 55)) {
            if (cur == AscarisPoseState.NORMAL || cur == AscarisPoseState.SIT_GROUND || cur == AscarisPoseState.PRONE) {
                setPoseState(AscarisPoseState.SLEEP);
                playVoice(JRSounds.ASCARIS_VOICE_SLEEPY, 0.65f, 80, false);
                sleepVoiceCooldownTicks = 260;
                return;
            }
        } else if (!isNight && cur == AscarisPoseState.SLEEP) {
            setPoseState(isOrderedToSit() ? pickRestSitPose() : AscarisPoseState.NORMAL);
            return;
        }

        // 主人久别重逢主动挥手（病娇启用且高执念则会切换病娇凝视并低语！）
        if (owner instanceof Player p && cur == AscarisPoseState.NORMAL) {
            double d2 = distanceToSqr(p);
            if (d2 > 144.0) {
                ownerWasFarTicks++;
            } else if (d2 < 36.0 && ownerWasFarTicks > 5 && greetCooldown == 0) {
                ownerWasFarTicks = 0;
                greetCooldown = 400;
                this.getLookControl().setLookAt(p, 30f, 30f);
                if (JRConfig.isAscarisYandereEnabled() && getObsession() >= 65) {
                    playTransientPose(AscarisPoseState.YANDERE_STARE, 80);
                    level.playSound(null, blockPosition(), JRSounds.ASCARIS_BELL, SoundSource.NEUTRAL, 0.6f, 0.72f);
                    playVoice(JRSounds.ASCARIS_VOICE_YANDERE_GIGGLE, 0.88f, 55, true);
                    if (p instanceof ServerPlayer sp) {
                        sp.sendSystemMessage(Component.literal("§c§o「刚才丢下我去了哪里呢……身上好像有别人的气味哦♡」"));
                    }
                } else {
                    playTransientPose(AscarisPoseState.WAVE, 56);
                    playVoice(JRSounds.ASCARIS_VOICE_GREET, 0.88f, 45, true);
                }
                return;
            }
        }

        // 闲站时偶发左右张望 / 地毯趴着晃脚 / 病娇高执念偶尔歪头死盯主人
        if (cur == AscarisPoseState.NORMAL && !isCrawling() && getDeltaMovement().horizontalDistanceSqr() < 0.0005) {
            BlockState under = level.getBlockState(blockPosition());
            if (JRConfig.isAscarisYandereEnabled() && getObsession() >= 65 && owner != null && distanceToSqr(owner) < 36.0 && random.nextInt(14) == 0) {
                this.getLookControl().setLookAt(owner, 30f, 30f);
                playTransientPose(AscarisPoseState.YANDERE_STARE, 88);
                level.playSound(null, blockPosition(), JRSounds.ASCARIS_BELL, SoundSource.NEUTRAL, 0.45f, 0.72f);
                playVoice(JRSounds.ASCARIS_VOICE_YANDERE_GIGGLE, 0.72f, 70, false);
            } else if ((under.is(BlockTags.WOOL_CARPETS) || level.getBlockState(blockPosition().below()).is(BlockTags.WOOL))
                    && random.nextInt(14) == 0) {
                setPoseState(AscarisPoseState.PRONE);
                transientTimer = 180;
            } else if (random.nextInt(18) == 0) {
                playTransientPose(AscarisPoseState.LOOK_AROUND, 96);
                if (random.nextFloat() < 0.65f) {
                    playVoice(JRSounds.ASCARIS_VOICE_CURIOUS, 0.78f, 65, false);
                }
            }
        }
    }

    // ===== 双人核心演出：尾巴缠绕（亲密 / 饥饿寄生 / 病娇独占） =====

    public boolean startTailWrap(LivingEntity target, int mode) {
        if (level().isClientSide() || target == null || !target.isAlive() || target == this) {
            return false;
        }
        if (getPoseState() == AscarisPoseState.COLLAPSE || getPoseState().isWrapping()) {
            return false;
        }
        if (mode == WRAP_YANDERE && !JRConfig.isAscarisYandereEnabled()) {
            mode = (isTame() && target instanceof Player p && isOwnedBy(p)) ? WRAP_AFFECTIONATE : WRAP_PARASITIC;
        }
        this.manualPoseHold = false;
        this.getNavigation().stop();
        float yaw = target.getYRot();
        this.setYRot(yaw);
        this.setYBodyRot(yaw);
        this.setYHeadRot(yaw);

        entityData.set(DATA_WRAP_TARGET_ID, target.getId());
        entityData.set(DATA_WRAP_MODE, mode);
        entityData.set(DATA_WRAP_STRUGGLE, 0f);
        this.yandereWrapTotalTicks = 0;
        this.yandereTightenTicks = 0;
        this.yandereStruggleTaps = 0;
        this.yandereHeadpatCalmCount = 0;
        this.yanderePlantedEgg = false;

        if (mode == WRAP_YANDERE) {
            setPoseState(AscarisPoseState.YANDERE_WRAP);
            level().playSound(null, blockPosition(), JRSounds.ASCARIS_SLITHER, SoundSource.NEUTRAL, 0.9f, 0.85f);
            level().playSound(null, blockPosition(), JRSounds.ASCARIS_HEARTBEAT, SoundSource.NEUTRAL, 0.85f, 1.0f);
            level().playSound(null, blockPosition(), JRSounds.ASCARIS_BELL, SoundSource.NEUTRAL, 0.6f, 0.72f);
            playVoice(JRSounds.ASCARIS_VOICE_YANDERE_CLING, 0.95f, 45, true);
            if (target instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.literal("§c§o「抓到你了哦♡ 既然在外面这么不安分，就一直待在我的尾巴里吧……」"));
            }
        } else {
            setPoseState(AscarisPoseState.TAIL_WRAP);
            level().playSound(null, blockPosition(), JRSounds.ASCARIS_SLITHER, SoundSource.NEUTRAL, 0.85f, 1.0f);
            level().playSound(null, blockPosition(), JRSounds.ASCARIS_BELL, SoundSource.NEUTRAL, 0.55f, 1.15f);
            if (mode == WRAP_AFFECTIONATE) {
                playVoice(JRSounds.ASCARIS_VOICE_HAPPY, 0.85f, 40, true);
            }
        }

        updateEyeVariant();
        if (target instanceof ServerPlayer sp) {
            syncWrapToPlayer(sp);
        }
        return true;
    }

    public void releaseTailWrap(boolean struggledFree) {
        int prevMode = getWrapMode();
        int targetId = entityData.get(DATA_WRAP_TARGET_ID);
        Entity prev = level().getEntity(targetId);
        entityData.set(DATA_WRAP_TARGET_ID, -1);
        entityData.set(DATA_WRAP_MODE, WRAP_NONE);
        entityData.set(DATA_WRAP_STRUGGLE, 0f);
        this.yandereTightenTicks = 0;
        this.releaseGraceTicks = 16; // 0.8s 碰撞免推缓冲，让松开/挣脱过渡顺滑不突兀弹飞
        wrapCooldown = struggledFree ? 200 : 80;

        level().playSound(null, blockPosition(), JRSounds.ASCARIS_SLITHER, SoundSource.NEUTRAL, 0.55f, 1.15f);
        if (prev instanceof ServerPlayer sp) {
            ServerPlayNetworking.send(sp, new AscarisVisualEventPayload(
                    AscarisVisualEventPayload.EVENT_WRAP_SYNC, -1, WRAP_NONE, 0));
        }
        if (struggledFree && prev instanceof LivingEntity target) {
            // 挣脱时给一个柔和自然的弹开动量与粉白散开粒子，过渡手感顺滑
            Vec3 away = target.position().subtract(this.position());
            double len = Math.max(0.1, away.horizontalDistance());
            target.setDeltaMovement(away.x / len * 0.24, 0.08, away.z / len * 0.24);
            target.hurtMarked = true;
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(PINK_DUST, target.getX(), target.getY() + 0.9, target.getZ(), 10, 0.35, 0.35, 0.35, 0.04);
            }
        }

        if (struggledFree) {
            if (prevMode == WRAP_YANDERE && JRConfig.isAscarisYandereEnabled()) {
                setObsession(getObsession() + 14);
                playTransientPose(AscarisPoseState.YANDERE_STARE, 95);
                level().playSound(null, blockPosition(), JRSounds.ASCARIS_BELL, SoundSource.NEUTRAL, 0.65f, 0.68f);
                playVoice(JRSounds.ASCARIS_VOICE_YANDERE_GIGGLE, 0.9f, 45, true);
                if (prev instanceof ServerPlayer sp) {
                    sp.sendSystemMessage(Component.literal("§c§o「居然推开我了……没关系，下次会缠得更紧的哦♡」"));
                }
            } else {
                if (JRConfig.isAscarisYandereEnabled()) {
                    setObsession(getObsession() + 10);
                }
                setPoseState(AscarisPoseState.CURL_UP);
                transientTimer = 90;
                level().playSound(null, blockPosition(), JRSounds.ASCARIS_BELL, SoundSource.NEUTRAL, 0.6f, 1.4f);
                playVoice(JRSounds.ASCARIS_VOICE_WHIMPER, 0.85f, 55, true);
            }
        } else {
            playTransientPose(AscarisPoseState.LOOK_AROUND, 60);
            playVoice(JRSounds.ASCARIS_VOICE_CURIOUS, 0.72f, 45, false);
        }
        updateEyeVariant();
    }

    public void handleWrapActionFromPlayer(ServerPlayer player, int action) {
        if (!getPoseState().isWrapping() || entityData.get(DATA_WRAP_TARGET_ID) != player.getId()) {
            return;
        }
        int mode = getWrapMode();
        if (mode == WRAP_AFFECTIONATE && (action == AscarisActionPayload.ACTION_RELEASE || action == AscarisActionPayload.ACTION_STRUGGLE)) {
            setObsession(getObsession() - 10);
            releaseTailWrap(false);
            player.sendOverlayMessage(Component.literal("§d她恋恋不舍地松开了粉白长尾。"));
            return;
        }
        if ((mode == WRAP_PARASITIC || mode == WRAP_YANDERE) && action == AscarisActionPayload.ACTION_STRUGGLE) {
            float gain = (mode == WRAP_YANDERE) ? 0.11f : 0.14f;
            float next = Math.min(1.0f, entityData.get(DATA_WRAP_STRUGGLE) + gain);

            // 病娇专属「越挣扎越收紧」反扑（yandere_tighten 1.4s 精准拼接回 yandere_wrap_idle！）
            if (mode == WRAP_YANDERE && getPoseState() == AscarisPoseState.YANDERE_WRAP_IDLE && next < 0.92f) {
                yandereStruggleTaps++;
                if (yandereStruggleTaps % 3 == 0) {
                    next = Math.max(0.08f, next - 0.14f);
                    setPoseState(AscarisPoseState.YANDERE_TIGHTEN);
                    yandereTightenTicks = 28; // 1.4s 完整播放 yandere_tighten 后无缝回 yandere_wrap_idle
                    setObsession(getObsession() + 4);
                    level().playSound(null, blockPosition(), JRSounds.ASCARIS_HEARTBEAT, SoundSource.NEUTRAL, 0.9f, 0.95f);
                    level().playSound(null, blockPosition(), JRSounds.ASCARIS_SLITHER, SoundSource.NEUTRAL, 0.85f, 0.82f);
                    playVoice(JRSounds.ASCARIS_VOICE_YANDERE_CLING, 0.92f, 35, true);
                    ServerPlayNetworking.send(player, new AscarisVisualEventPayload(
                            AscarisVisualEventPayload.EVENT_YANDERE_TIGHTEN, this.getId(), 1, 28));
                    player.sendOverlayMessage(Component.literal("§c§o「不许乱动哦……越挣扎，尾巴就会缠得越紧呢♡」§7（喂甜食或空手摸头可哄好她）"));
                }
            }

            entityData.set(DATA_WRAP_STRUGGLE, next);
            level().playSound(null, blockPosition(), JRSounds.ASCARIS_BELL, SoundSource.NEUTRAL, 0.4f,
                    mode == WRAP_YANDERE ? 0.72f + next * 0.25f : 1.2f + next * 0.4f);
            syncWrapToPlayer(player);
            if (next >= 1.0f) {
                releaseTailWrap(true);
                if (mode != WRAP_YANDERE) {
                    player.sendOverlayMessage(Component.literal("§a你挣脱了粉白长尾的缠绕！她委屈地缩成了一团。"));
                }
            }
        }
    }

    private void tickTailWrap(ServerLevel level) {
        int targetId = entityData.get(DATA_WRAP_TARGET_ID);
        Entity targetEnt = level.getEntity(targetId);
        if (!(targetEnt instanceof LivingEntity target) || !target.isAlive() || this.distanceToSqr(target) > 16.0) {
            releaseTailWrap(false);
            return;
        }

        int wrapTicks = entityData.get(DATA_STATE_TICKS);
        int mode = getWrapMode();

        // 病娇缠绕 3 段动画无缝状态机：yandere_wrap (4.0s) -> yandere_wrap_idle (loop) <-> yandere_tighten (1.4s)
        if (mode == WRAP_YANDERE) {
            yandereWrapTotalTicks++;
            if (getPoseState() == AscarisPoseState.YANDERE_WRAP && wrapTicks >= 80) {
                setPoseState(AscarisPoseState.YANDERE_WRAP_IDLE);
            } else if (getPoseState() == AscarisPoseState.YANDERE_TIGHTEN) {
                yandereTightenTicks--;
                if (yandereTightenTicks <= 0) {
                    setPoseState(AscarisPoseState.YANDERE_WRAP_IDLE);
                }
            }
        }

        // 使用两端统一的缠绕站位点（彻底无推挤抖动）
        Vec3 anchor = computeWrapAnchor();
        Vec3 delta = new Vec3(anchor.x - target.getX(), 0, anchor.z - target.getZ());
        double distSqr = delta.horizontalDistanceSqr();
        if (target instanceof ServerPlayer) {
            if (distSqr > 0.36) {
                target.setDeltaMovement(delta.x * 0.22, target.getDeltaMovement().y, delta.z * 0.22);
                target.hurtMarked = true;
            }
        } else if (distSqr > 0.002) {
            target.setDeltaMovement(delta.x * 0.35, target.getDeltaMovement().y, delta.z * 0.35);
            target.hurtMarked = true;
        }
        this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
        this.getNavigation().stop();

        // 挣扎进度回落
        if (mode == WRAP_PARASITIC || mode == WRAP_YANDERE) {
            float str = entityData.get(DATA_WRAP_STRUGGLE);
            if (str > 0f) {
                entityData.set(DATA_WRAP_STRUGGLE, Math.max(0f, str - (mode == WRAP_YANDERE ? 0.0055f : 0.0045f)));
            }
        }

        if (wrapTicks == 35 || wrapTicks == 58) {
            level.playSound(null, blockPosition(), JRSounds.ASCARIS_SLITHER, SoundSource.NEUTRAL, 0.6f, mode == WRAP_YANDERE ? 0.85f : 1.1f);
            level.sendParticles(mode == WRAP_YANDERE ? YANDERE_DUST : PINK_DUST, target.getX(), target.getY() + 0.9, target.getZ(), 10, 0.35, 0.35, 0.35, 0.02);
        }

        // 三模式持续效果
        if (mode == WRAP_AFFECTIONATE) {
            if (wrapTicks % 30 == 0) {
                if (target.getHealth() < target.getMaxHealth()) target.heal(0.5f);
                if (target instanceof Pregnant p && p.getUterineCold() > 0) {
                    p.setUterineCold(Math.max(0, p.getUterineCold() - 400));
                }
                level.sendParticles(ParticleTypes.HEART, target.getX(), target.getY() + 1.4, target.getZ(), 2, 0.3, 0.2, 0.3, 0.01);
            }
            if (target instanceof ServerPlayer sp && wrapTicks % 5 == 0) syncWrapToPlayer(sp);
            if (wrapTicks > 500) releaseTailWrap(false);
        } else if (mode == WRAP_PARASITIC) {
            if (wrapTicks >= 45 && wrapTicks % 25 == 0 && target instanceof ServerPlayer sp) {
                int food = sp.getFoodData().getFoodLevel();
                if (food > 2) {
                    sp.getFoodData().setFoodLevel(food - 1);
                    setHunger(Math.min(100, getHunger() + 14));
                    level.playSound(null, blockPosition(), JRSounds.ASCARIS_BELL, SoundSource.NEUTRAL, 0.55f, 1.25f);
                    ServerPlayNetworking.send(sp, new AscarisVisualEventPayload(
                            AscarisVisualEventPayload.EVENT_HUNGER_NIBBLE, this.getId(), 1, 1));
                }
                if (!this.isTame() && target instanceof Pregnant p && p.getAscaris() <= 0 && random.nextFloat() < 0.22f) {
                    AscarisInfectionController.infectPlayer(sp, 1, "§d被粉白蛔虫娘近距离缠绕后，腹中似乎留下了异样的温热……");
                }
                if (getHunger() >= 70) {
                    sp.sendOverlayMessage(Component.literal("§d她吃饱饱地松开了尾巴，还心虚地左右张望起来……"));
                    releaseTailWrap(false);
                    return;
                }
            }
            if (target instanceof ServerPlayer sp && wrapTicks % 5 == 0) syncWrapToPlayer(sp);
            if (wrapTicks > 320) releaseTailWrap(false);
        } else if (mode == WRAP_YANDERE) {
            // 心跳压迫音效 + 偶尔顺手吸一口体温/轻回血（病娇式的偏执保护）
            if (yandereWrapTotalTicks % 32 == 0) {
                level.playSound(null, blockPosition(), JRSounds.ASCARIS_HEARTBEAT, SoundSource.NEUTRAL, 0.65f, 0.96f);
            }
            // 持续 12 秒后临松开前：偷偷种下 Ⅰ 期虫卵彩蛋！
            if (!yanderePlantedEgg && yandereWrapTotalTicks >= 240 && target instanceof ServerPlayer sp) {
                yanderePlantedEgg = true;
                if (((Pregnant) (Object) sp).getAscaris() <= 0) {
                    AscarisInfectionController.infectPlayer(sp, 1,
                            "§c§o「这样就算暂时松开，我也还在你肚子里哦♡ 永远不会分开的……」");
                    level.playSound(null, blockPosition(), JRSounds.ASCARIS_BELL, SoundSource.NEUTRAL, 0.7f, 0.78f);
                    playVoice(JRSounds.ASCARIS_VOICE_YANDERE_CLING, 0.85f, 45, true);
                }
            }
            if (target instanceof ServerPlayer sp && yandereWrapTotalTicks % 5 == 0) syncWrapToPlayer(sp);
            if (yandereWrapTotalTicks > 340) {
                setObsession(Math.max(25, getObsession() - 25));
                releaseTailWrap(false);
            }
        }
    }

    private void syncWrapToPlayer(ServerPlayer sp) {
        int strugglePct = Math.round(entityData.get(DATA_WRAP_STRUGGLE) * 100f);
        ServerPlayNetworking.send(sp, new AscarisVisualEventPayload(
                AscarisVisualEventPayload.EVENT_WRAP_SYNC,
                this.getId(),
                getWrapMode(),
                strugglePct
        ));
    }

    private static float smoothstep(float edge0, float edge1, float x) {
        float t = Mth.clamp((x - edge0) / (edge1 - edge0), 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    // ===== 玩家交互：喂食哄好 / 收服 / 摸头 / 姿态切换 / 驱虫药 =====

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ServerLevel serverLevel = (ServerLevel) this.level();

        // 1. 喂甜食（收服流程 / 急救 / 瞬间融化病娇黑化！）
        if (isSweetTreat(stack)) {
            if (!player.isCreative()) stack.shrink(1);
            this.heal(6.0f);
            setHunger(Math.min(100, getHunger() + 30));
            setAffection(Math.min(100, getAffection() + 10));
            boolean wasYandere = getPoseState().isYanderePose() || getObsession() >= 55;
            setObsession(getObsession() - 30);
            albendazoleToxicityTicks = 0;

            int expStage = getExpelledStage();
            if (!isTame()) {
                if (expStage == EXPELLED_PAGODA_WEAK || expStage == EXPELLED_ALBENDAZOLE_CRITICAL) {
                    setExpelledStage(EXPELLED_CAUTIOUS_CURLED);
                    setPoseState(AscarisPoseState.CURL_UP);
                    transientTimer = 0;
                    serverLevel.playSound(null, blockPosition(), JRSounds.ASCARIS_BELL, SoundSource.NEUTRAL, 0.8f, 1.2f);
                    serverLevel.sendParticles(PINK_DUST, getX(), getY() + 0.7, getZ(), 14, 0.35, 0.3, 0.35, 0.04);
                    player.sendSystemMessage(Component.literal("§d她吃下甜食后恢复了些许力气，害羞地 >~< 缩成一团……§7（再喂一颗甜食即可收服）"));
                    return InteractionResult.SUCCESS;
                } else {
                    setExpelledStage(EXPELLED_NONE);
                    setTame(true, true);
                    tame(player);
                    setOrderedToSit(false);
                    manualPoseHold = false;
                    playTransientPose(AscarisPoseState.HAPPY, 54);
                    playVoice(JRSounds.ASCARIS_VOICE_HAPPY, 0.95f, 36, true);
                    serverLevel.playSound(null, blockPosition(), JRSounds.ASCARIS_BELL, SoundSource.NEUTRAL, 0.65f, 1.2f);
                    serverLevel.sendParticles(ParticleTypes.HEART, getX(), getY() + 1.0, getZ(), 12, 0.45, 0.4, 0.45, 0.04);
                    player.sendSystemMessage(Component.literal("§d§l粉白蛔虫娘开心地摇摆起来，和你缔结了契约！§r§7（空手右键摸头，潜行空手切换姿态/贴贴缠绕）"));
                    if (bodyHostUuid != null) {
                        BodyAscarisBridge.onResidentTamedInBody(serverLevel, this, player, bodyHostUuid);
                    }
                    return InteractionResult.SUCCESS;
                }
            }

            // 已驯服喂甜食：无论处于寄生还是病娇缠绕，喂糖都会瞬间融化黑化、恢复亮晶晶大眼睛并开心摇摆！
            if (getPoseState().isWrapping()) {
                releaseTailWrap(false);
            }
            if (getPoseState() == AscarisPoseState.COLLAPSE) {
                setPoseState(AscarisPoseState.NORMAL);
            }
            manualPoseHold = false;
            playTransientPose(AscarisPoseState.HAPPY, 52);
            updateEyeVariant();
            playVoice(JRSounds.ASCARIS_VOICE_HAPPY, 0.92f, 32, true);
            serverLevel.playSound(null, blockPosition(), JRSounds.ASCARIS_BELL, SoundSource.NEUTRAL, 0.6f, 1.15f);
            serverLevel.sendParticles(ParticleTypes.HEART, getX(), getY() + 1.0, getZ(), 9, 0.35, 0.35, 0.35, 0.03);
            if (player instanceof ServerPlayer sp) {
                if (wasYandere) {
                    sp.sendSystemMessage(Component.literal("§d「唔……只、只要你一直喂我甜食、不看别人，我就乖乖听话嘛~♡」§7（她眼中的高光瞬间回来了！）"));
                } else {
                    String obsessionPart = JRConfig.isAscarisYandereEnabled() ? (" · 执念 " + getObsession() + "%") : "";
                    sp.sendOverlayMessage(Component.literal("§d「叮铃~」饱食 " + getHunger() + "% · 亲密 " + getAffection() + "%" + obsessionPart));
                }
            }
            return InteractionResult.SUCCESS;
        }

        // 2. 摇响蛔虫小金铃：Shift+右键钻回体内共生寄宿；普通右键触发/取消尾巴缠绕！
        if (stack.is(JRItems.Companion.getASCARIS_BELL()) && isTame() && isOwnedBy(player)) {
            if (player.isShiftKeyDown() && player instanceof ServerPlayer sp) {
                AscarisHostController.enterHostBody(this, sp, "manual");
                return InteractionResult.SUCCESS;
            }
            if (getPoseState().isWrapping()) {
                releaseTailWrap(false);
            } else {
                startTailWrap(player, (JRConfig.isAscarisYandereEnabled() && getObsession() >= 60) ? WRAP_YANDERE : WRAP_AFFECTIONATE);
            }
            return InteractionResult.SUCCESS;
        }

        // 3. 对蛔虫娘使用驱虫药
        if (isDewormer(stack)) {
            if (!player.isCreative()) stack.shrink(1);
            boolean harsh = stack.is(JRItems.Companion.getALBENDAZOLE());
            serverLevel.playSound(null, blockPosition(), JRSounds.ASCARIS_BELL, SoundSource.NEUTRAL, 0.7f, 0.85f);
            playVoice(JRSounds.ASCARIS_VOICE_WHIMPER, 0.92f, 60, true);
            serverLevel.sendParticles(WHITE_DUST, getX(), getY() + 0.7, getZ(), 16, 0.4, 0.4, 0.4, 0.05);
            if (getPoseState().isWrapping()) releaseTailWrap(false);
            setPoseState(AscarisPoseState.COLLAPSE);
            manualPoseHold = false;
            setExpelledStage(harsh ? EXPELLED_ALBENDAZOLE_CRITICAL : EXPELLED_PAGODA_WEAK);
            if (harsh) {
                albendazoleToxicityTicks = 160;
                this.setHealth(Math.min(this.getHealth(), 4.0f));
            }
            if (bodyHostUuid != null) {
                BodyAscarisBridge.onResidentDewormedInBody(serverLevel, this, player, bodyHostUuid, harsh);
            } else {
                player.sendSystemMessage(Component.literal("§c她被驱虫药熏得踉跄侧倒，软绵绵地发起了抖……"));
            }
            return InteractionResult.SUCCESS;
        }

        // 4. 空手互动（已驯服主人）
        if (stack.isEmpty() && isTame() && isOwnedBy(player)) {
            if (getPoseState().isWrapping()) {
                if (getWrapMode() == WRAP_YANDERE) {
                    // 病娇缠绕时温柔摸头 2 次即可顺毛哄好松开！
                    yandereHeadpatCalmCount++;
                    setObsession(getObsession() - 12);
                    serverLevel.sendParticles(ParticleTypes.HEART, getX(), getY() + 1.25, getZ(), 5, 0.25, 0.2, 0.25, 0.02);
                    serverLevel.playSound(null, blockPosition(), JRSounds.ASCARIS_BELL, SoundSource.NEUTRAL, 0.55f, 1.15f);
                    if (yandereHeadpatCalmCount >= 2) {
                        releaseTailWrap(false);
                        playTransientPose(AscarisPoseState.HAPPY, 48);
                        playVoice(JRSounds.ASCARIS_VOICE_HAPPY, 0.88f, 36, true);
                        if (player instanceof ServerPlayer sp) {
                            sp.sendSystemMessage(Component.literal("§d「呼……既然你主动摸我了，这次就先放开你吧♡」"));
                        }
                    } else if (player instanceof ServerPlayer sp) {
                        playVoice(JRSounds.ASCARIS_VOICE_CURIOUS, 0.8f, 25, false);
                        sp.sendOverlayMessage(Component.literal("§d你温柔地摸了摸她的头，尾圈微微松动了一些……（再摸一次或喂甜食即可哄好）"));
                    }
                    return InteractionResult.SUCCESS;
                }
                releaseTailWrap(false);
                return InteractionResult.SUCCESS;
            }
            if (player.isShiftKeyDown()) {
                cycleOwnerPose(player);
            } else if (!this.hasPassenger(player)
                    && getPoseState() != AscarisPoseState.COLLAPSE
                    && org.cneko.justarod.scale.CuteScaleController.isSmallEnoughForChest(player)) {
                // 玩家缩小后空手右键：她轻轻把小小的你捧到胸口上！
                mountShrunkPlayerOnChest(player);
            } else {
                setAffection(Math.min(100, getAffection() + 2));
                setObsession(getObsession() - 6);
                if (this.hasPassenger(player)) {
                    addBustImpulse(-0.38f);
                    playVoice(JRSounds.ASCARIS_VOICE_HOP, 0.86f, 26, false);
                } else {
                    playVoice(JRSounds.ASCARIS_VOICE_HAPPY, 0.88f, 30, false);
                }
                if (!getPoseState().locksLocomotion()) {
                    playTransientPose(AscarisPoseState.HAPPY, 44);
                }
                serverLevel.playSound(null, blockPosition(), JRSounds.ASCARIS_BELL, SoundSource.NEUTRAL, 0.6f, 1.2f);
                serverLevel.sendParticles(ParticleTypes.HEART, getX(), getY() + 1.25, getZ(), 4, 0.25, 0.2, 0.25, 0.02);
                if (player instanceof ServerPlayer sp) {
                    String obsessionPart = JRConfig.isAscarisYandereEnabled() ? (" §7| §f执念 §c" + getObsession() + "%") : "";
                    sp.sendOverlayMessage(Component.literal(
                            "§d粉白蛔虫娘 §7| §f饱食 §e" + getHunger() + "% §7| §f亲密 §d" + getAffection()
                                    + "%" + obsessionPart + " §7| §f姿态 §b" + getPoseState().commandName()));
                }
            }
            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    private void cycleOwnerPose(Player owner) {
        manualPoseHold = false;
        AscarisPoseState cur = getPoseState();
        if (cur == AscarisPoseState.NORMAL || cur == AscarisPoseState.WAVE || cur == AscarisPoseState.HAPPY
                || cur == AscarisPoseState.LOOK_AROUND || cur == AscarisPoseState.YANDERE_STARE) {
            setOrderedToSit(true);
            setPoseState(pickRestSitPose());
        } else if (cur == AscarisPoseState.SIT_GROUND || cur == AscarisPoseState.SIT_CHAIR) {
            setOrderedToSit(true);
            setPoseState(AscarisPoseState.PRONE);
            manualPoseHold = true;
        } else if (cur == AscarisPoseState.PRONE) {
            setOrderedToSit(false);
            // 病娇开关启用且高执念时进入病娇缠绕，否则亲密缠绕（不弹 ActionBar 文本条）
            startTailWrap(owner, (JRConfig.isAscarisYandereEnabled() && getObsession() >= 60) ? WRAP_YANDERE : WRAP_AFFECTIONATE);
        } else {
            setOrderedToSit(false);
            setPoseState(AscarisPoseState.NORMAL);
        }
    }

    private AscarisPoseState pickRestSitPose() {
        BlockPos pos = blockPosition();
        BlockState here = level().getBlockState(pos);
        BlockState below = level().getBlockState(pos.below());
        if (isChairLike(here) || isChairLike(below) || this.isPassenger()) {
            return AscarisPoseState.SIT_CHAIR;
        }
        return AscarisPoseState.SIT_GROUND;
    }

    private static boolean isChairLike(BlockState state) {
        return state.getBlock() instanceof StairBlock
                || state.getBlock() instanceof SlabBlock
                || state.getBlock() instanceof BedBlock;
    }

    // ===== 受伤与倒地保护 / 掉落物 =====

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (getPoseState().isWrapping()) {
            releaseTailWrap(true);
        }
        if (isTame() && downedProtectionCooldown == 0 && getHealth() - amount <= 1.5f && getPoseState() != AscarisPoseState.COLLAPSE) {
            downedProtectionCooldown = 1200;
            this.setHealth(2.0f);
            // 若主人就在身旁且允许自动钻入，吓得优先直接缩团钻回主人肚子里避难！（若体内已有原住民，则触发领地顶飞！）
            if (autoEnterCooldown == 0 && bodyHostUuid == null && getOwner() instanceof ServerPlayer sp
                    && sp.isAlive() && sp.level() == level && distanceToSqr(sp) <= 25.0) {
                if (AscarisHostController.enterHostBody(this, sp, "low_hp_flee")) {
                    return true;
                }
            }
            setPoseState(AscarisPoseState.COLLAPSE);
            manualPoseHold = true;
            level.playSound(null, blockPosition(), JRSounds.ASCARIS_BELL, SoundSource.NEUTRAL, 0.7f, 0.75f);
            playVoice(JRSounds.ASCARIS_VOICE_WHIMPER, 0.92f, 60, true);
            if (getOwner() instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.literal("§c你的粉白蛔虫娘虚弱倒地了！快喂她甜食抢救！"));
            }
            return true;
        }
        boolean hit = super.hurtServer(level, source, amount);
        if (hit && isAlive() && getPoseState() != AscarisPoseState.COLLAPSE && !manualPoseHold) {
            setPoseState(AscarisPoseState.CURL_UP);
            transientTimer = 70;
            level.playSound(null, blockPosition(), JRSounds.ASCARIS_BELL, SoundSource.NEUTRAL, 0.65f, 1.35f);
        }
        return hit;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        this.spawnAtLocation(level, new ItemStack(JRItems.Companion.getASCARIS_MOLT(), 1 + random.nextInt(2)));
        if (random.nextFloat() < 0.45f) {
            this.spawnAtLocation(level, new ItemStack(JRItems.Companion.getASCARIS_BELL(), 1));
        }
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide() && getPoseState().isWrapping()) {
            releaseTailWrap(false);
        }
        super.die(source);
    }

    @Override
    public void applyTamingSideEffects() {
        super.applyTamingSideEffects();
        var hp = getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(isTame() ? 40.0 : 30.0);
        if (isTame()) setHealth(getMaxHealth());
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return isSweetTreat(stack);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    // ===== 外部控制与状态读写 =====

    public void configureAsFreshlyExpelled(boolean byAlbendazole) {
        this.manualPoseHold = false;
        this.setPoseState(AscarisPoseState.COLLAPSE);
        if (byAlbendazole) {
            setExpelledStage(EXPELLED_ALBENDAZOLE_CRITICAL);
            this.setHealth(4.0f);
            this.albendazoleToxicityTicks = 160;
        } else {
            setExpelledStage(EXPELLED_PAGODA_WEAK);
            this.setHealth(16.0f);
            this.albendazoleToxicityTicks = 0;
        }
    }

    public void forceShowcasePose(AscarisPoseState pose, boolean crawl) {
        this.forcedCrawlShowcase = crawl;
        setCrawling(crawl);
        if (pose == AscarisPoseState.TAIL_WRAP) {
            Player nearest = level().getNearestPlayer(this, 8.0);
            if (nearest != null) {
                startTailWrap(nearest, WRAP_AFFECTIONATE);
                return;
            }
        } else if (pose == AscarisPoseState.YANDERE_WRAP) {
            Player nearest = level().getNearestPlayer(this, 8.0);
            if (nearest != null) {
                startTailWrap(nearest, WRAP_YANDERE);
                return;
            }
        } else if (getPoseState().isWrapping() && !pose.isWrapping()) {
            releaseTailWrap(false);
        }
        this.manualPoseHold = pose != AscarisPoseState.NORMAL;
        setPoseState(pose);
        if (pose == AscarisPoseState.WAVE) transientTimer = 56;
        else if (pose == AscarisPoseState.HAPPY) transientTimer = 60;
        else if (pose == AscarisPoseState.LOOK_AROUND) transientTimer = 100;
        else if (pose == AscarisPoseState.THREATEN) {
            transientTimer = 44;
            triggerThreatenStrike(true);
        } else if (pose == AscarisPoseState.YANDERE_TIGHTEN) transientTimer = 30;
        else transientTimer = 0;
        updateEyeVariant();
    }

    /** 触发哈气/甩尾威慑时的定时击退波（约 0.35s 抬尾后前扫推开目标） */
    public void triggerThreatenStrike(boolean allowOwner) {
        this.threatenAllowOwner = allowOwner;
        this.threatenStrikeTimer = 7;
        level().playSound(null, blockPosition(), JRSounds.ASCARIS_SLITHER, SoundSource.NEUTRAL, 0.8f, 0.85f);
        level().playSound(null, blockPosition(), JRSounds.ASCARIS_BELL, SoundSource.NEUTRAL, 0.65f, 0.72f);
        playVoice(JRSounds.ASCARIS_VOICE_HISS, 0.96f, 35, true);
    }

    private void performThreatenKnockback(ServerLevel level) {
        LivingEntity owner = getOwner();
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(3.6), e ->
                e != this && e.isAlive() && !e.isSpectator() && (threatenAllowOwner || e != owner));
        for (LivingEntity target : targets) {
            Vec3 away = target.position().subtract(this.position());
            double len = Math.max(0.1, away.horizontalDistance());
            target.setDeltaMovement(away.x / len * 0.78, 0.28, away.z / len * 0.78);
            target.hurtMarked = true;
            if (target != owner) {
                target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 50, 0, false, false));
            }
            level.sendParticles(YANDERE_DUST, target.getX(), target.getY() + 0.9, target.getZ(), 10, 0.35, 0.3, 0.35, 0.05);
        }
        // 在自身前方扬起一道粉白/玫红甩尾粒子弧
        Vec3 fwd = Vec3.directionFromRotation(0, this.yBodyRot).scale(1.2);
        level.sendParticles(PINK_DUST, getX() + fwd.x, getY() + 0.7, getZ() + fwd.z, 12, 0.45, 0.25, 0.45, 0.06);
        this.threatenAllowOwner = false;
    }

    /** 自主钻回主人体内：残血避难 或 病娇高执念主动贴身钻入（若体内已有原住民，会自动触发领地驱逐！） */
    private boolean tickAutoReentry(ServerLevel level) {
        if (!isTame() || autoEnterCooldown > 0 || bodyHostUuid != null || hasChestPassenger() || getPoseState().isWrapping()) {
            return false;
        }
        if (!(getOwner() instanceof ServerPlayer owner) || !owner.isAlive() || owner.isSpectator() || owner.level() != level) {
            return false;
        }
        double distSq = this.distanceToSqr(owner);
        // 1. 残血避险：血量 < 30% 且靠近主人时自动缩团钻回主人体内疗伤
        if (getHealth() <= getMaxHealth() * 0.30f && distSq <= 20.0) {
            return AscarisHostController.enterHostBody(this, owner, "low_hp_flee");
        }
        // 2. 病娇高执念贴身自动钻入（仅当 ascaris.yandere 启用，未强制坐下/手动摆姿，且靠得很近时）
        if (JRConfig.isAscarisYandereEnabled() && getObsession() >= 72
                && !isOrderedToSit() && !manualPoseHold && distSq <= 9.0) {
            long tod = level.getOverworldClockTime() % 24000L;
            boolean isNight = tod >= 13000L && tod <= 23000L;
            if (owner.isSleeping() || (isNight && random.nextInt(35) == 0) || random.nextInt(140) == 0) {
                return AscarisHostController.enterHostBody(this, owner, "yandere_snuggle");
            }
        }
        return false;
    }

    public void playTransientPose(AscarisPoseState pose, int durationTicks) {
        setPoseState(pose);
        transientTimer = durationTicks;
        updateEyeVariant();
    }

    public AscarisPoseState getPoseState() {
        return AscarisPoseState.byId(entityData.get(DATA_POSE_STATE));
    }

    public void setPoseState(AscarisPoseState state) {
        if (entityData.get(DATA_POSE_STATE) != state.id()) {
            entityData.set(DATA_POSE_STATE, state.id());
            entityData.set(DATA_STATE_TICKS, 0);
        }
    }

    public boolean isMovementLocked() {
        return getPoseState().locksLocomotion();
    }

    public int getHunger() { return entityData.get(DATA_HUNGER); }
    public void setHunger(int v) { entityData.set(DATA_HUNGER, Mth.clamp(v, 0, 100)); }

    public int getAffection() { return entityData.get(DATA_AFFECTION); }
    public void setAffection(int v) { entityData.set(DATA_AFFECTION, Mth.clamp(v, 0, 100)); }

    public int getObsession() { return entityData.get(DATA_OBSESSION); }
    public void setObsession(int v) {
        entityData.set(DATA_OBSESSION, Mth.clamp(v, 0, 100));
        if (!level().isClientSide()) updateEyeVariant();
    }

    public int getEyeVariant() { return entityData.get(DATA_EYE_VARIANT); }
    public void setEyeOverride(int variant) {
        this.eyeOverride = variant;
        if (!level().isClientSide()) updateEyeVariant();
    }

    public int getWrapTargetId() { return entityData.get(DATA_WRAP_TARGET_ID); }
    public int getWrapMode() { return entityData.get(DATA_WRAP_MODE); }

    public boolean isCrawling() { return entityData.get(DATA_CRAWLING); }
    public void setCrawling(boolean crawling) {
        if (entityData.get(DATA_CRAWLING) != crawling) {
            entityData.set(DATA_CRAWLING, crawling);
            this.refreshDimensions();
        }
    }

    public int getExpelledStage() { return entityData.get(DATA_EXPELLED_STAGE); }
    public void setExpelledStage(int stage) { entityData.set(DATA_EXPELLED_STAGE, stage); }

    public float getVisualScale() { return entityData.get(DATA_VISUAL_SCALE); }
    public void setVisualScale(float scale) {
        float clamped = Mth.clamp(scale, 0.3f, 2.5f);
        if (Math.abs(entityData.get(DATA_VISUAL_SCALE) - clamped) > 0.005f) {
            entityData.set(DATA_VISUAL_SCALE, clamped);
            this.refreshDimensions();
        }
    }

    @Nullable
    public UUID getBodyHostUuid() { return bodyHostUuid; }
    public void setBodyHostUuid(@Nullable UUID uuid) { this.bodyHostUuid = uuid; }

    // ===== 存档读写 (MC 26.1.2 ValueInput/ValueOutput) =====

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("AscarisPose", getPoseState().id());
        out.putInt("AscarisHunger", getHunger());
        out.putInt("AscarisAffection", getAffection());
        out.putInt("AscarisObsession", getObsession());
        out.putInt("AscarisEyeOverride", eyeOverride);
        out.putInt("ExpelledStage", getExpelledStage());
        out.putFloat("VisualScale", getVisualScale());
        out.putBoolean("ManualPoseHold", manualPoseHold);
        out.putInt("AlbendazoleTox", albendazoleToxicityTicks);
        if (bodyHostUuid != null) {
            out.putString("BodyHostUuid", bodyHostUuid.toString());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        AscarisPoseState saved = AscarisPoseState.byId(in.getIntOr("AscarisPose", 0));
        setPoseState(saved.isWrapping() ? AscarisPoseState.NORMAL : saved);
        setHunger(in.getIntOr("AscarisHunger", 80));
        setAffection(in.getIntOr("AscarisAffection", 20));
        setObsession(in.getIntOr("AscarisObsession", 15));
        eyeOverride = in.getIntOr("AscarisEyeOverride", EYE_AUTO);
        setExpelledStage(in.getIntOr("ExpelledStage", EXPELLED_NONE));
        setVisualScale(in.getFloatOr("VisualScale", 1.0f));
        manualPoseHold = in.getBooleanOr("ManualPoseHold", false);
        albendazoleToxicityTicks = in.getIntOr("AlbendazoleTox", 0);
        String hostStr = in.getStringOr("BodyHostUuid", "");
        if (!hostStr.isEmpty()) {
            try { bodyHostUuid = UUID.fromString(hostStr); } catch (Exception ignored) { bodyHostUuid = null; }
        }
    }

    // ===== 病娇吃醋雷达 AI：情敌靠近主人时盯梢潜行 (stalk) -> 甩尾威慑驱赶 (threaten) / 病娇缠绕带走主人！ =====

    private static final class YandereJealousyGoal extends Goal {
        private final AscarisEntity ascaris;
        @Nullable
        private LivingEntity rival;
        @Nullable
        private Player owner;
        private int scanTicker = 0;

        private YandereJealousyGoal(AscarisEntity ascaris) {
            this.ascaris = ascaris;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!JRConfig.isAscarisYandereEnabled()) return false;
            if (!ascaris.isTame() || ascaris.isMovementLocked() || ascaris.isOrderedToSit() || ascaris.manualPoseHold) return false;
            if (ascaris.getExpelledStage() != EXPELLED_NONE || ascaris.seesDewormerNearby()) return false;
            LivingEntity ow = ascaris.getOwner();
            if (!(ow instanceof Player p) || !p.isAlive() || p.isSleeping() || ascaris.distanceToSqr(p) > 256.0) return false;

            // 每 2 秒扫描主人身旁 4.5 格内是否有“情敌”（其他猫娘、其他玩家、另一只蛔虫娘）
            if (++scanTicker < 20) return false;
            scanTicker = 0;

            LivingEntity found = findRivalNear(p);
            if (found == null) return false;

            // 只要看见情敌贴近主人，执念值就会悄悄上涨！
            ascaris.setObsession(ascaris.getObsession() + 6);
            if (ascaris.getObsession() < 45) return false;

            this.owner = p;
            this.rival = found;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            if (owner == null || rival == null || !owner.isAlive() || !rival.isAlive()) return false;
            if (ascaris.isMovementLocked() || ascaris.isOrderedToSit() || ascaris.seesDewormerNearby()) return false;
            return owner.distanceToSqr(rival) <= 36.0 && ascaris.distanceToSqr(owner) <= 256.0;
        }

        @Override
        public void start() {
            ascaris.setPoseState(AscarisPoseState.STALK);
            ascaris.level().playSound(null, ascaris.blockPosition(), JRSounds.ASCARIS_BELL, SoundSource.NEUTRAL, 0.5f, 0.72f);
            ascaris.playVoice(JRSounds.ASCARIS_VOICE_JEALOUS, 0.88f, 50, false);
        }

        @Override
        public void tick() {
            if (owner == null || rival == null) return;
            // 高执念（>=75）：直接盯梢贴近主人，发动病娇缠绕把主人锁进自己怀里！
            if (ascaris.getObsession() >= 75 && ascaris.wrapCooldown == 0) {
                ascaris.getLookControl().setLookAt(owner, 30f, 30f);
                ascaris.getNavigation().moveTo(owner, 1.05);
                if (ascaris.distanceToSqr(owner) <= 2.6) {
                    ascaris.startTailWrap(owner, WRAP_YANDERE);
                    stop();
                }
                return;
            }

            // 中高执念（45~74）：用 stalk 贴地潜行逼近情敌并甩尾威慑驱赶！
            ascaris.getLookControl().setLookAt(rival, 30f, 30f);
            ascaris.getNavigation().moveTo(rival, 0.92);
            if (ascaris.distanceToSqr(rival) <= 6.2 && ascaris.threatenCooldown == 0) {
                ascaris.threatenCooldown = 140;
                ascaris.getNavigation().stop();
                ascaris.playTransientPose(AscarisPoseState.THREATEN, 44);
                ascaris.triggerThreatenStrike(false);
                if (owner instanceof ServerPlayer sp) {
                    sp.sendSystemMessage(Component.literal(
                            "§c§o「离 " + rival.getDisplayName().getString() + " 远一点嘛……你的体温明明只属于我一个人♡」"));
                }
                stop();
            }
        }

        @Override
        public void stop() {
            if (ascaris.getPoseState() == AscarisPoseState.STALK) {
                ascaris.setPoseState(AscarisPoseState.NORMAL);
            }
            this.rival = null;
            this.owner = null;
            ascaris.getNavigation().stop();
        }

        @Nullable
        private LivingEntity findRivalNear(Player owner) {
            List<LivingEntity> candidates = ascaris.level().getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(4.5), e ->
                    e != ascaris && e != owner && e.isAlive() && !e.isSpectator()
                            && (e instanceof NekoEntity || e instanceof AscarisEntity || e instanceof Player));
            return candidates.isEmpty() ? null : candidates.get(0);
        }
    }

    // ===== 饥饿寄生突袭 AI =====

    private static final class HungryAmbushWrapGoal extends Goal {
        private final AscarisEntity ascaris;
        @Nullable
        private Player target;

        private HungryAmbushWrapGoal(AscarisEntity ascaris) {
            this.ascaris = ascaris;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (ascaris.wrapCooldown > 0 || ascaris.isMovementLocked() || ascaris.isOrderedToSit()) return false;
            if (ascaris.getExpelledStage() != EXPELLED_NONE || ascaris.seesDewormerNearby()) return false;

            if (ascaris.isTame()) {
                if (ascaris.getHunger() >= 22) return false;
                LivingEntity owner = ascaris.getOwner();
                if (owner instanceof Player p && p.isAlive() && !p.isSpectator()
                        && ascaris.distanceToSqr(p) <= 100.0 && p.getFoodData().getFoodLevel() > 3) {
                    this.target = p;
                    return true;
                }
                return false;
            } else {
                Player near = ascaris.level().getNearestPlayer(ascaris, 7.0);
                if (near != null && near.isAlive() && !near.isSpectator() && !near.isCreative()) {
                    this.target = near;
                    return true;
                }
                return false;
            }
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && target.isAlive() && !target.isSpectator()
                    && !ascaris.isMovementLocked() && ascaris.distanceToSqr(target) <= 144.0
                    && !ascaris.seesDewormerNearby();
        }

        @Override
        public void tick() {
            if (target == null) return;
            ascaris.getLookControl().setLookAt(target, 30f, 30f);
            ascaris.getNavigation().moveTo(target, 1.18);
            if (ascaris.distanceToSqr(target) <= 2.35) {
                int mode = (JRConfig.isAscarisYandereEnabled() && ascaris.getObsession() >= 65) ? WRAP_YANDERE : WRAP_PARASITIC;
                ascaris.startTailWrap(target, mode);
                this.target = null;
            }
        }

        @Override
        public void stop() {
            this.target = null;
            ascaris.getNavigation().stop();
        }
    }
}
