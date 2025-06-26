package org.cneko.justarod.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.cneko.justarod.damage.JRDamageTypes;
import org.cneko.justarod.entity.Fallible;
import org.cneko.justarod.entity.Insertable;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

@Mixin(Entity.class)
public class EntityMixin implements Fallible, Insertable {
    @Unique
    private Entity fallenBy;
    @Unique
    private int fallTicks;
    @Unique
    private double startFallHeight;
    @Unique
    private Boolean justARod$originalNoClip = null; // 记录原始noClip状态


    @Override
    public Entity justARod$getFallenBy() {
        return fallenBy;
    }

    @Override
    public void justARod$setFallenBy(Entity fallenBy) {
        this.fallenBy = fallenBy;
        this.fallTicks = 0;
        this.startFallHeight = ((Entity) (Object) this).getY();
        // 禁用noClip
        Entity self = (Entity) (Object) this;
        // 只在第一次被坠机时记录并禁用
        if (justARod$originalNoClip == null) {
            justARod$originalNoClip = self.noClip;
            self.noClip = false;
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void justARod$fallTick(CallbackInfo ci) {
        if (fallenBy != null) {
            Entity self = (Entity) (Object) this;
            fallTicks++;

            // 持续禁用noClip，防止被其他mod/AI改回
            self.noClip = false;

            if (fallTicks < 100) {
                // 让y速度为负，模拟持续下落
                self.addVelocity(0f, -0.1, 0f);
            }

            boolean shouldApplyFallDamage = false;
            if (self instanceof EnderDragonEntity) {
                // 末影龙：只要Y坐标下降超过3格就直接判定为落地
                double fallDistance = startFallHeight - self.getY();
                if (fallDistance > 3) {
                    shouldApplyFallDamage = true;
                }
            } else {
                // 其他实体：正常落地判定
                if (self.isOnGround()) {
                    shouldApplyFallDamage = true;
                }
            }

            if (shouldApplyFallDamage) {
                double fallDistance = startFallHeight - self.getY();
                if (fallDistance > 3) {
                    float damage = (float) (fallDistance - 3);
                    if (self instanceof EnderDragonEntity dragon) {
                        float newHealth = dragon.getHealth() - damage;
                        dragon.setHealth(newHealth);
                        if (newHealth <= 0.0F) {
                            dragon.onDeath(JRDamageTypes.icedTea(fallenBy));
                        }
                    } else {
                        self.damage(JRDamageTypes.icedTea(fallenBy), damage);
                    }
                    if (self instanceof LivingEntity livingSelf) {
                        fallenBy.sendMessage(Text.of("§c"+livingSelf.getName().getString()+"坠机了！"));
                    }
                }
                // 重置
                fallenBy = null;
                fallTicks = 0;
                startFallHeight = 0;
                if (justARod$originalNoClip != null) {
                    self.noClip = justARod$originalNoClip;
                    justARod$originalNoClip = null;
                }
            } else if (fallTicks >= 100) {
                // 超时未落地，重置
                fallenBy = null;
                fallTicks = 0;
                startFallHeight = 0;
                if (justARod$originalNoClip != null) {
                    self.noClip = justARod$originalNoClip;
                    justARod$originalNoClip = null;
                }
            }
        }
    }


}