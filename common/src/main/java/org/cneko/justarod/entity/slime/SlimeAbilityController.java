package org.cneko.justarod.entity.slime;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.cneko.justarod.config.JRConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative ability lifecycle, kept separate from storage and riding logic. */
public final class SlimeAbilityController {
    private final TamedSlimeEntity slime;
    private SlimeAbility ability = SlimeAbility.NONE;
    private int cooldown;
    private int charge;
    private int fuseTotal;
    private int absorptionTicks;
    private record PreyState(boolean noAi, boolean noGravity) {}
    private final Map<UUID, PreyState> gathered = new HashMap<>();
    private int gatherTicks;
    private int gatherTotal;
    public boolean isGathering() { return !gathered.isEmpty(); }

    public SlimeAbilityController(TamedSlimeEntity slime) { this.slime = slime; }

    public void absorb(LivingEntity prey) {
        SlimeAbility gained = SlimeAbility.fromPrey(prey.getType());
        if (!JRConfig.isSlimeAbilitiesEnabled() || gained == SlimeAbility.NONE) return;
        if (slime.level() instanceof ServerLevel level) cancelGathering(level);
        ability = gained;
        cooldown = charge = fuseTotal = 0;
        absorptionTicks = 60;
        sync();
        if (slime.level() instanceof ServerLevel level) {
            level.playSound(null, slime.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
                    SoundSource.NEUTRAL, 1f, 0.8f);
            level.sendParticles(ParticleTypes.ENCHANT, slime.getX(), centerY(), slime.getZ(),
                    40, slime.getBbWidth() * 0.3, slime.getBbHeight() * 0.3, slime.getBbWidth() * 0.3, 0.15);
            var owner = slime.getOwnerUuid() == null ? null : level.getPlayerInAnyDimension(slime.getOwnerUuid());
            if (owner != null) owner.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(
                    "justarod.slime.ability.absorbed", net.minecraft.network.chat.Component.translatable(gained.translationKey())));
        }
    }

    public void tick() {
        if (!(slime.level() instanceof ServerLevel level)) return;
        if (cooldown > 0) cooldown--;
        if (slime.isUncontrolled()) cooldown = Math.min(cooldown, slime.abilityCooldownTicks());
        if (absorptionTicks > 0) absorptionTicks--;
        if (!JRConfig.isSlimeAbilitiesEnabled()) {
            cancelGathering(level);
            charge = fuseTotal = 0;
            sync();
            return;
        }
        if (isGathering()) tickGathering(level);
        if (charge > 0) {
            charge++;
            if (charge % 4 == 0) level.sendParticles(ParticleTypes.SMOKE,
                    slime.getX(), centerY(), slime.getZ(), 5, slime.getBbWidth() * 0.3, 0.3, slime.getBbWidth() * 0.3, 0.02);
            if (charge >= fuseTotal) {
                charge = 0;
                burst(level);
                cooldown = slime.abilityCooldownTicks();
            }
            sync();
            return;
        }
        // Driving is manual. Level 5 ignores stay; captive passengers do not stop AI.
        if (cooldown == 0 && ability != SlimeAbility.NONE && slime.getControllingPassenger() == null
                && slime.canAutoHunt() && !slime.isDevouring() && !isGathering()
                && slime.tickCount % 5 == 0) {
            double range = ability == SlimeAbility.ENDERMAN ? JRConfig.getSlimeTeleportRange()
                    : ability == SlimeAbility.SHULKER ? JRConfig.getSlimeLevitationRadius() : 3;
            LivingEntity target = slime.findAbilityPrey(range, false);
            if (target != null) activate(target);
        }
        if (slime.tickCount % 5 == 0 || cooldown == 0 || absorptionTicks > 0) sync();
    }

    public boolean activate(LivingEntity target) {
        if (!(slime.level() instanceof ServerLevel level) || !JRConfig.isSlimeAbilitiesEnabled()
                || ability == SlimeAbility.NONE || cooldown > 0 || charge > 0 || slime.isDevouring()) return false;
        if (isGathering()) return false;
        boolean activated = switch (ability) {
            case CREEPER -> {
                charge = 1;
                fuseTotal = JRConfig.getSlimeFuseTicks();
                level.playSound(null, slime.blockPosition(), SoundEvents.CREEPER_PRIMED, SoundSource.NEUTRAL, 1.2f, 0.65f);
                yield true;
            }
            case ENDERMAN -> teleportHunt(level, target);
            case SHULKER -> {
                yield levitate(level);
            }
            default -> false;
        };
        if (activated && ability != SlimeAbility.CREEPER) cooldown = slime.abilityCooldownTicks();
        sync();
        return activated;
    }

    private void burst(ServerLevel level) {
        slime.markAbilityPulse();
        // Damage AND knockback must be excluded: shouldDamageEntity alone does not protect passengers from launch.
        ExplosionDamageCalculator calculator = new ExplosionDamageCalculator() {
            @Override public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
                return !slime.isAbilityFriendly(entity);
            }
            @Override public float getKnockbackMultiplier(Entity entity) {
                return slime.isAbilityFriendly(entity) ? 0f : 1.25f;
            }
        };
        level.explode(slime, level.damageSources().explosion(slime, slime), calculator,
                slime.getX(), centerY(), slime.getZ(), JRConfig.getSlimeExplosionPower(), false,
                JRConfig.isSlimeExplosionBreakBlocks() ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, slime.getX(), centerY(), slime.getZ(), 2, 0.8, 0.5, 0.8, 0);
        ring(level, ParticleTypes.CLOUD, Math.min(12, JRConfig.getSlimeExplosionPower() * 1.4), 80);
        level.sendParticles(ParticleTypes.ITEM_SLIME, slime.getX(), centerY(), slime.getZ(), 70,
                slime.getBbWidth() * 0.45, slime.getBbHeight() * 0.4, slime.getBbWidth() * 0.45, 0.3);
    }

    private boolean teleportHunt(ServerLevel level, LivingEntity prey) {
        if (!slime.isSwallowReady() || !slime.canHunt(prey) || slime.distanceToSqr(prey) > Math.pow(JRConfig.getSlimeTeleportRange() + slime.getBbWidth() * 0.5, 2)) return false;
        // Try deterministic points around the prey. Validate the WHOLE slime box, support and loaded chunks.
        double radius = (slime.getBbWidth() + prey.getBbWidth()) * 0.5 + 0.35;
        double angle = Math.atan2(slime.getZ() - prey.getZ(), slime.getX() - prey.getX());
        for (int i = 0; i < 12; i++) {
            double a = angle + i * Math.PI / 6;
            for (int dy : new int[]{0, 1, -1, 2, -2}) {
                Vec3 destination = new Vec3(prey.getX() + Math.cos(a) * radius, prey.getY() + dy, prey.getZ() + Math.sin(a) * radius);
                AABB box = slime.getBoundingBox().move(destination.subtract(slime.position()));
                if (!safeDestination(level, box)) continue;
                portal(level, slime.position());
                slime.teleportTo(destination.x, destination.y, destination.z);
                slime.setDeltaMovement(Vec3.ZERO);
                portal(level, destination);
                slime.getLookControl().setLookAt(prey, 180, 180);
                return slime.startAbilitySwallow(prey);
            }
        }
        return false;
    }

    private boolean safeDestination(ServerLevel level, AABB box) {
        BlockPos min = BlockPos.containing(box.minX, box.minY, box.minZ);
        BlockPos max = BlockPos.containing(box.maxX, box.maxY, box.maxZ);
        if (box.minY < level.getMinY() || box.maxY > level.getMaxY()
                || !level.hasChunksAt(min, max) || !level.getWorldBorder().isWithinBounds(box)
                || !level.noCollision(slime, box) || level.containsAnyLiquid(box)) return false;
        // A broad support check keeps large slimes out of caves/air and prevents an immediate fall.
        return !level.noBlockCollision(slime, new AABB(box.minX + 0.05, box.minY - 0.15,
                box.minZ + 0.05, box.maxX - 0.05, box.minY, box.maxZ - 0.05));
    }

    private void portal(ServerLevel level, Vec3 position) {
        level.sendParticles(ParticleTypes.PORTAL, position.x, position.y + slime.getBbHeight() * 0.5, position.z,
                60, slime.getBbWidth() * 0.4, slime.getBbHeight() * 0.4, slime.getBbWidth() * 0.4, 0.25);
        level.playSound(null, position.x, position.y, position.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 1.2f, 0.8f);
    }

    private boolean levitate(ServerLevel level) {
        double radius = JRConfig.getSlimeLevitationRadius();
        var targets = level.getEntitiesOfClass(LivingEntity.class, slime.getBoundingBox().inflate(radius),
                e -> slime.canHunt(e) && !slime.isAbilityFriendly(e));
        targets.sort(java.util.Comparator.comparingDouble(slime::distanceToSqr));
        int space = slime.storage().freeSpace(slime.tier().internalSpace());
        for (LivingEntity target : targets) {
            if (gathered.size() >= slime.getTierLevel()) break;
            int volume = SlimeTier.volumeOf(target);
            if (volume > space || target.distanceToSqr(slime) > Math.pow(radius + slime.getBbWidth() * 0.5, 2)) continue;
            space -= volume;
            boolean noAi = target instanceof net.minecraft.world.entity.Mob mob && mob.isNoAi();
            gathered.put(target.getUUID(), new PreyState(noAi, target.isNoGravity()));
            if (target instanceof net.minecraft.world.entity.Mob mob) mob.setNoAi(true);
            if (!(target instanceof net.minecraft.world.entity.player.Player)) target.setNoGravity(true);
            target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, JRConfig.getSlimeLevitationTicks(), 1), slime);
            level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY(), target.getZ(), 15, 0.35, 0.2, 0.35, 0.06);
        }
        if (gathered.isEmpty()) return false;
        gatherTicks = 0;
        gatherTotal = JRConfig.getSlimeLevitationTicks();
        slime.markAbilityPulse();
        level.playSound(null, slime.blockPosition(), SoundEvents.SHULKER_OPEN, SoundSource.NEUTRAL, 1.5f, 0.7f);
        level.playSound(null, slime.blockPosition(), SoundEvents.SHULKER_SHOOT, SoundSource.NEUTRAL, 1.2f, 0.7f);
        ring(level, ParticleTypes.END_ROD, radius + slime.getBbWidth() * 0.5, 72);
        return true;
    }

    private void tickGathering(ServerLevel level) {
        gatherTicks++;
        var ready = new ArrayList<LivingEntity>();
        var iterator = gathered.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            var entity = level.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity target)) continue;
            if (!slime.canHunt(target) || slime.distanceToSqr(target) > 32 * 32) {
                restorePrey(target, entry.getValue());
                iterator.remove();
                continue;
            }
            Vec3 center = new Vec3(slime.getX(), slime.getY() + slime.getBbHeight() + 2, slime.getZ());
            Vec3 offset = center.subtract(target.position());
            if (gatherTicks <= gatherTotal) {
                // First lift visibly, then stabilize and gather above the head. Movement uses velocity and collision.
                if (gatherTicks > gatherTotal / 3) target.removeEffect(MobEffects.LEVITATION);
                double dy = gatherTicks <= gatherTotal / 3 ? Math.max(0.12, target.getDeltaMovement().y)
                        : Math.clamp(offset.y * 0.18, -0.3, 0.35);
                target.setDeltaMovement(Math.clamp(offset.x * 0.18, -0.35, 0.35), dy, Math.clamp(offset.z * 0.18, -0.35, 0.35));
            } else {
                target.removeEffect(MobEffects.LEVITATION);
                target.setNoGravity(false);
                target.setDeltaMovement(Math.clamp(offset.x * 0.18, -0.2, 0.2), target instanceof net.minecraft.world.entity.Mob ? Math.max(-1.0, target.getDeltaMovement().y - 0.08) : target.getDeltaMovement().y,
                        Math.clamp(offset.z * 0.18, -0.2, 0.2));
                if (target.getY() <= slime.getY() + slime.getBbHeight() + 0.3
                        && offset.horizontalDistanceSqr() < 1.0 && slime.isSwallowReady()) ready.add(target);
            }
            // NoAI mobs skip vanilla travel, so apply the controlled velocity with normal block collisions.
            // Players retain their own travel simulation and receive the synchronized velocity instead.
            if (target instanceof net.minecraft.world.entity.Mob) {
                target.move(net.minecraft.world.entity.MoverType.SELF, target.getDeltaMovement());
            }
            target.hurtMarked = true;
            target.resetFallDistance();
        }
        if (!ready.isEmpty() && !slime.isDevouring()) {
            // Restore before normal swallowing records original mob AI, so later release is correct.
            for (LivingEntity target : ready) restorePrey(target, gathered.get(target.getUUID()));
            if (slime.startAbilitySwallowBatch(ready)) ready.forEach(target -> gathered.remove(target.getUUID()));
        }
        if (gatherTicks > gatherTotal + 100) cancelGathering(level);
    }

    private void restorePrey(LivingEntity target, PreyState state) {
        target.removeEffect(MobEffects.LEVITATION);
        target.setNoGravity(state.noGravity());
        if (target instanceof net.minecraft.world.entity.Mob mob) mob.setNoAi(state.noAi());
        target.resetFallDistance();
    }
    public void cancelGathering(ServerLevel level) {
        gathered.forEach((id, state) -> { if (level.getEntity(id) instanceof LivingEntity target) restorePrey(target, state); });
        gathered.clear();
    }

    private void ring(ServerLevel level, net.minecraft.core.particles.SimpleParticleType type, double radius, int count) {
        for (int i = 0; i < count; i++) {
            double a = Math.PI * 2 * i / count;
            level.sendParticles(type, slime.getX() + Math.cos(a) * radius, slime.getY() + 0.4,
                    slime.getZ() + Math.sin(a) * radius, 1, 0, 0.15, 0, 0.03);
        }
    }

    private double centerY() { return slime.getY() + slime.getBbHeight() * 0.5; }
    private void sync() { slime.syncAbility(ability, cooldown, charge, fuseTotal, absorptionTicks); }

    public void save(ValueOutput out) {
        out.putInt("Ability", ability.id());
        out.putInt("Cooldown", cooldown);
        out.putInt("Charge", charge);
        out.putInt("FuseTotal", fuseTotal);
        out.putInt("AbsorptionTicks", absorptionTicks);
        out.putInt("GatherTicks", gatherTicks);
        out.putInt("GatherTotal", gatherTotal);
        var ids = out.list("Gathered", net.minecraft.core.UUIDUtil.CODEC);
        var ai = out.list("GatheredNoAi", com.mojang.serialization.Codec.BOOL);
        var gravity = out.list("GatheredNoGravity", com.mojang.serialization.Codec.BOOL);
        gathered.forEach((id, state) -> { ids.add(id); ai.add(state.noAi()); gravity.add(state.noGravity()); });
    }

    public void load(ValueInput in) {
        ability = SlimeAbility.byId(in.getIntOr("Ability", 0));
        cooldown = Math.max(0, in.getIntOr("Cooldown", 0));
        fuseTotal = Math.clamp(in.getIntOr("FuseTotal", 0), 0, 200);
        charge = ability == SlimeAbility.CREEPER && fuseTotal > 0
                ? Math.clamp(in.getIntOr("Charge", 0), 0, fuseTotal - 1) : 0;
        absorptionTicks = Math.clamp(in.getIntOr("AbsorptionTicks", 0), 0, 60);
        gatherTotal = Math.clamp(in.getIntOr("GatherTotal", 0), 0, 400);
        gatherTicks = Math.clamp(in.getIntOr("GatherTicks", 0), 0, gatherTotal + 100);
        gathered.clear();
        var ids = new ArrayList<UUID>();
        var ai = new ArrayList<Boolean>();
        var gravity = new ArrayList<Boolean>();
        in.listOrEmpty("Gathered", net.minecraft.core.UUIDUtil.CODEC).forEach(ids::add);
        in.listOrEmpty("GatheredNoAi", com.mojang.serialization.Codec.BOOL).forEach(ai::add);
        in.listOrEmpty("GatheredNoGravity", com.mojang.serialization.Codec.BOOL).forEach(gravity::add);
        for (int i = 0; i < ids.size(); i++) gathered.put(ids.get(i), new PreyState(i < ai.size() && ai.get(i), i < gravity.size() && gravity.get(i)));
        sync();
    }
}
