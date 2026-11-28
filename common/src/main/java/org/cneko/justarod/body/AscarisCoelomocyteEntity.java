package org.cneko.justarod.body;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.cneko.justarod.ascaris.AscarisEntity;

import java.util.List;

/**
 * 粉白体腔免疫小细胞（Coelomocyte）：真实线虫假体腔内游动的星状/球状免疫共生体。
 * <ul>
 *   <li>平时：半透粉白微光在假体腔与暖窝周围优雅漂浮，好奇贴近并绕着玩家转圈，空手轻戳回血冒爱心，喂甜食会开心滋养玩家与宿主；</li>
 *   <li>免疫护体态：当玩家在体内乱拆组织方块弄痛宿主时，会瞬间泛红冲过来轻推防御；安抚围咽神经环后立刻平复回粉白。</li>
 * </ul>
 */
public class AscarisCoelomocyteEntity extends Entity {

    private static final EntityDataAccessor<Integer> DATA_ALERT_TICKS =
            SynchedEntityData.defineId(AscarisCoelomocyteEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HAPPY_TICKS =
            SynchedEntityData.defineId(AscarisCoelomocyteEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_PHASE =
            SynchedEntityData.defineId(AscarisCoelomocyteEntity.class, EntityDataSerializers.FLOAT);

    private static final DustParticleOptions PINK_GLOW = new DustParticleOptions(0xFFB6D4, 0.8f);
    private static final DustParticleOptions WARM_PEACH = new DustParticleOptions(0xFFE0B8, 0.85f);
    private static final DustParticleOptions ALERT_CRIMSON = new DustParticleOptions(0xE03064, 0.9f);

    private long bodySlot = -1L;
    private double homeX;
    private double homeY;
    private double homeZ;
    private int bumpCooldown = 0;
    private int petCooldown = 0;
    private int emptySlotTicks = 0;

    public AscarisCoelomocyteEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = false;
        this.setNoGravity(true);
    }

    public void configureForSlot(long slot, double hx, double hy, double hz, float phase) {
        this.bodySlot = slot;
        this.homeX = hx;
        this.homeY = hy;
        this.homeZ = hz;
        this.entityData.set(DATA_PHASE, phase);
        this.setPos(hx, hy, hz);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ALERT_TICKS, 0);
        builder.define(DATA_HAPPY_TICKS, 0);
        builder.define(DATA_PHASE, 0f);
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    public boolean isAlert() {
        return this.entityData.get(DATA_ALERT_TICKS) > 0;
    }

    public boolean isHappy() {
        return this.entityData.get(DATA_HAPPY_TICKS) > 0;
    }

    public float getPhase() {
        return this.entityData.get(DATA_PHASE);
    }

    public long getBodySlot() {
        return this.bodySlot;
    }

    public void setAlertTicks(int ticks) {
        this.entityData.set(DATA_ALERT_TICKS, Math.max(0, ticks));
        if (ticks > 0) {
            this.entityData.set(DATA_HAPPY_TICKS, 0);
        }
    }

    public void calmDown() {
        this.entityData.set(DATA_ALERT_TICKS, 0);
        this.entityData.set(DATA_HAPPY_TICKS, 60);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            tickClientParticles();
            return;
        }
        tickServer((ServerLevel) this.level());
    }

    private void tickClientParticles() {
        if (this.tickCount % 14 == 0) {
            DustParticleOptions dust = isAlert() ? ALERT_CRIMSON : (isHappy() ? WARM_PEACH : PINK_GLOW);
            this.level().addParticle(dust,
                    getX() + (random.nextDouble() - 0.5) * 0.45,
                    getY() + 0.25 + (random.nextDouble() - 0.5) * 0.35,
                    getZ() + (random.nextDouble() - 0.5) * 0.45,
                    0, 0.015, 0);
        }
    }

    private void tickServer(ServerLevel level) {
        int alert = this.entityData.get(DATA_ALERT_TICKS);
        if (alert > 0) this.entityData.set(DATA_ALERT_TICKS, alert - 1);
        int happy = this.entityData.get(DATA_HAPPY_TICKS);
        if (happy > 0) this.entityData.set(DATA_HAPPY_TICKS, happy - 1);
        if (bumpCooldown > 0) bumpCooldown--;
        if (petCooldown > 0) petCooldown--;

        // 若不在体内维度或所属槽位长期无访客，则自然消散释放内存
        if (bodySlot >= 0 && level.dimension().equals(BodyWorld.DIMENSION)) {
            List<ServerPlayer> visitors = level.getEntitiesOfClass(ServerPlayer.class,
                    this.getBoundingBox().inflate(52.0), p -> p.isAlive() && !p.isSpectator());
            if (visitors.isEmpty()) {
                if (++emptySlotTicks > 200) {
                    this.discard();
                    return;
                }
            } else {
                emptySlotTicks = 0;
            }
        }

        Player nearest = level.getNearestPlayer(this, 14.0);
        double t = (this.tickCount * 0.045) + getPhase();
        Vec3 target;

        // 检查本槽位是否有需要体腔免疫细胞前往凝集修复的组织破口或高代谢废物负荷
        AscarisPhysiologyState phys = (bodySlot >= 0 && level.dimension().equals(BodyWorld.DIMENSION))
                ? BodyRegionData.get(level.getServer()).physiologyFor(bodySlot)
                : null;
        AscarisPhysiologyState.TissueBreach repairTarget = null;
        if (alert == 0 && phys != null && !phys.breaches.isEmpty() && phys.glycogen >= 15.0f && !phys.isBlockedByAlbendazole()) {
            int idx = Math.abs(Math.round(getPhase() * 10f)) % phys.breaches.size();
            repairTarget = phys.breaches.get(idx);
        }

        if (alert > 0 && nearest != null && nearest.isAlive() && !nearest.isSpectator()) {
            // 免疫警戒：主动冲向正在拆方块的访客进行轻推防御！
            target = nearest.position().add(0, 1.0, 0);
            if (bumpCooldown == 0 && this.distanceToSqr(nearest) <= 1.6) {
                bumpCooldown = 36;
                Vec3 push = nearest.position().subtract(this.position());
                double horiz = Math.max(0.1, push.horizontalDistance());
                nearest.push(push.x / horiz * 0.38, 0.12, push.z / horiz * 0.38);
                nearest.hurtMarked = true;
                if (nearest.getHealth() > 6.0f && nearest instanceof ServerPlayer sp) {
                    sp.hurtServer(level, level.damageSources().generic(), 0.5f);
                }
                level.playSound(null, blockPosition(), SoundEvents.SLIME_SQUISH_SMALL, SoundSource.NEUTRAL, 0.6f, 0.85f);
                level.sendParticles(ALERT_CRIMSON, getX(), getY() + 0.3, getZ(), 8, 0.3, 0.3, 0.3, 0.04);
            }
        } else if (repairTarget != null) {
            // 活体免疫修复态：飞向组织破口处环绕释放凝血微粒，消耗宿主少量糖原自主修补微小破损！
            net.minecraft.core.BlockPos bPos = repairTarget.worldPos(bodySlot);
            target = new Vec3(
                    bPos.getX() + 0.5 + Math.cos(t * 1.6) * 0.9,
                    bPos.getY() + 0.5 + Math.sin(t * 1.4) * 0.5,
                    bPos.getZ() + 0.5 + Math.sin(t * 1.6) * 0.9
            );
            if (this.tickCount % 20 == 0 && this.position().distanceToSqr(bPos.getX() + 0.5, bPos.getY() + 0.5, bPos.getZ() + 0.5) <= 9.0) {
                AscarisPhysiologyEngine.advanceCoelomocyteRepair(level, bodySlot, repairTarget, 20);
            }
        } else if (phys != null && phys.metabolicWaste >= 60.0f && Math.round(getPhase()) % 2 == 0) {
            // 代谢负荷高时，部分体腔细胞主动聚向 H 形排泄管横桥协助吞噬清除毒素！
            int cx = BodyRegionData.centerX(bodySlot);
            int cz = BodyRegionData.centerZ(bodySlot);
            target = new Vec3(
                    cx + AscarisAnatomyBuilder.EXCRETORY_BRIDGE_X + Math.cos(t) * 1.8,
                    AscarisAnatomyBuilder.FLOOR_Y + 1.6 + Math.sin(t * 1.4) * 0.5,
                    cz + Math.sin(t * 0.7) * 5.5
            );
            if (this.tickCount % 40 == 0) {
                phys.metabolicWaste = Math.max(0f, phys.metabolicWaste - 0.6f);
            }
        } else if (nearest != null && nearest.isAlive() && !nearest.isSpectator() && this.distanceToSqr(nearest) <= 64.0) {
            // 友好陪伴：在玩家身旁轻盈环绕飘浮（手持甜食时凑得更近更欢快！）
            boolean sweet = AscarisEntity.isSweetTreat(nearest.getMainHandItem()) || AscarisEntity.isSweetTreat(nearest.getOffhandItem());
            double orbitR = sweet ? 1.55 : 2.6;
            double ang = t * (sweet ? 1.45 : 0.75);
            target = new Vec3(
                    nearest.getX() + Math.cos(ang) * orbitR,
                    nearest.getY() + 1.15 + Math.sin(t * 1.4) * 0.42,
                    nearest.getZ() + Math.sin(ang) * orbitR
            );
            // 贴心小陪伴：玩家受伤时极温柔地帮忙恢复一点点血量
            if (this.tickCount % 160 == 0 && nearest.getHealth() < nearest.getMaxHealth() && this.distanceToSqr(nearest) <= 16.0) {
                nearest.heal(0.5f);
                level.sendParticles(ParticleTypes.HEART, getX(), getY() + 0.5, getZ(), 2, 0.2, 0.2, 0.2, 0.01);
            }
        } else {
            // 自由巡游：围绕诞生点在假体腔里缓慢画 8 字飘浮
            target = new Vec3(
                    homeX + Math.sin(t * 0.65) * 4.5,
                    homeY + Math.sin(t * 1.3) * 0.65,
                    homeZ + Math.cos(t * 0.5) * 3.0
            );
        }

        // 将目标限制在虫体内部安全范围内，允许修补高处肠壁/体壁破口而不穿出外壁
        if (bodySlot >= 0) {
            int cx = BodyRegionData.centerX(bodySlot);
            int cz = BodyRegionData.centerZ(bodySlot);
            double maxY = (repairTarget != null) ? (AscarisAnatomyBuilder.CY + 13.0) : (AscarisAnatomyBuilder.GUT_FLOOR_Y - 1.1);
            target = new Vec3(
                    Mth.clamp(target.x, cx - 36.0, cx + 36.0),
                    Mth.clamp(target.y, AscarisAnatomyBuilder.FLOOR_Y + 1.1, maxY),
                    Mth.clamp(target.z, cz - 13.0, cz + 13.0)
            );
        }

        Vec3 toTarget = target.subtract(this.position());
        double speed = alert > 0 ? 0.14 : 0.055;
        Vec3 desiredVel = toTarget.lengthSqr() > 0.01 ? toTarget.scale(speed) : Vec3.ZERO;
        if (desiredVel.length() > (alert > 0 ? 0.26 : 0.11)) {
            desiredVel = desiredVel.normalize().scale(alert > 0 ? 0.26 : 0.11);
        }
        Vec3 smoothed = this.getDeltaMovement().lerp(desiredVel, 0.22);
        this.setDeltaMovement(smoothed);
        this.move(MoverType.SELF, smoothed);

        if (smoothed.horizontalDistanceSqr() > 0.0004) {
            float yaw = (float) (Mth.atan2(smoothed.z, smoothed.x) * Mth.RAD_TO_DEG) - 90f;
            this.setYRot(yaw);
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ServerLevel level = (ServerLevel) this.level();
        ItemStack stack = player.getItemInHand(hand);

        // 喂甜食：平复警戒、开心地滋养玩家与外侧粉白蛔虫娘！
        if (AscarisEntity.isSweetTreat(stack)) {
            if (!player.isCreative()) stack.shrink(1);
            calmDown();
            player.heal(4.0f);
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, true, false));
            level.playSound(null, blockPosition(), SoundEvents.HONEY_DRINK.value(), SoundSource.NEUTRAL, 0.55f, 1.25f);
            level.sendParticles(ParticleTypes.HEART, getX(), getY() + 0.5, getZ(), 6, 0.35, 0.3, 0.35, 0.02);
            level.sendParticles(WARM_PEACH, getX(), getY() + 0.35, getZ(), 10, 0.35, 0.3, 0.35, 0.02);
            if (player instanceof ServerPlayer sp) {
                AscarisBodyInteractions.onCoelomocyteFedSweet(sp);
                sp.sendOverlayMessage(Component.literal("§d粉白体腔小细胞开心地吞下了甜食，暖融融的光晕包裹了你~♡"));
            }
            return InteractionResult.SUCCESS;
        }

        // 空手轻抚：软弹一下冒爱心、温柔回血并平复警戒！
        if (stack.isEmpty() && petCooldown == 0) {
            petCooldown = 24;
            calmDown();
            player.heal(1.5f);
            this.setDeltaMovement(this.getDeltaMovement().add(0, 0.14, 0));
            level.playSound(null, blockPosition(), SoundEvents.SLIME_SQUISH_SMALL, SoundSource.NEUTRAL, 0.45f, 1.35f);
            level.sendParticles(ParticleTypes.HEART, getX(), getY() + 0.45, getZ(), 4, 0.25, 0.25, 0.25, 0.02);
            if (player instanceof ServerPlayer sp) {
                sp.sendOverlayMessage(Component.literal("§d你轻轻戳了戳软乎乎的体腔小细胞，它舒服地晃动着蹭了蹭你~♡"));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        // 体腔细胞被轻打时不致死，而是软弹退开并进入免疫警戒；病娇模式下攻击它们会令宿主执念飙升！
        setAlertTicks(160);
        level.playSound(null, blockPosition(), SoundEvents.SLIME_SQUISH_SMALL, SoundSource.NEUTRAL, 0.6f, 0.9f);
        level.sendParticles(ALERT_CRIMSON, getX(), getY() + 0.3, getZ(), 8, 0.3, 0.3, 0.3, 0.04);
        if (source.getEntity() instanceof ServerPlayer sp && level.dimension().equals(BodyWorld.DIMENSION)) {
            AscarisBodyInteractions.onCoelomocyteAttacked(sp, this);
        }
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        this.bodySlot = in.getLongOr("BodySlot", -1L);
        this.homeX = in.getDoubleOr("HomeX", getX());
        this.homeY = in.getDoubleOr("HomeY", getY());
        this.homeZ = in.getDoubleOr("HomeZ", getZ());
        this.entityData.set(DATA_PHASE, in.getFloatOr("Phase", 0f));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        out.putLong("BodySlot", this.bodySlot);
        out.putDouble("HomeX", this.homeX);
        out.putDouble("HomeY", this.homeY);
        out.putDouble("HomeZ", this.homeZ);
        out.putFloat("Phase", getPhase());
    }
}
