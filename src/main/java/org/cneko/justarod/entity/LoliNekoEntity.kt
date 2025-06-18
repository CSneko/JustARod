package org.cneko.justarod.entity

import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.data.DataTracker
import net.minecraft.entity.data.TrackedData
import net.minecraft.entity.data.TrackedDataHandlerRegistry
import net.minecraft.nbt.NbtCompound
import net.minecraft.server.world.ServerWorld
import net.minecraft.world.World
import org.cneko.toneko.common.mod.api.NekoSkinRegistry
import org.cneko.toneko.common.mod.entities.INeko
import org.cneko.toneko.common.mod.entities.NekoEntity
import org.cneko.toneko.common.mod.entities.ToNekoEntities
import org.spongepowered.asm.mixin.Unique

/*
萝莉控别看了，对，说的就是你！变态一只！
 */
class LoliNekoEntity(private val type: EntityType<LoliNekoEntity>, world: World): NekoEntity(type, world) {
    companion object{
        val SHOWING_AGE: TrackedData<Int?> = DataTracker.registerData(LoliNekoEntity::class.java, TrackedDataHandlerRegistry.INTEGER)
    }
    override fun getBreedOffspring(
        p0: ServerWorld?,
        p1: INeko?
    ): NekoEntity? {
        return LoliNekoEntity(type, world)
    }
    fun getShowingAge(): Int {
        return dataTracker.get(SHOWING_AGE)!!
    }
    fun setShowingAge(age: Int) {
        dataTracker.set(SHOWING_AGE, age)
    }

    override fun initDataTracker(builder: DataTracker.Builder) {
        super.initDataTracker(builder)
        builder.add(SHOWING_AGE,18)
    }
    override fun writeCustomDataToNbt(compound: NbtCompound) {
        super.writeCustomDataToNbt(compound)
        compound.putInt("showing_age", this.getShowingAge())
    }
    override fun readCustomDataFromNbt(compound: NbtCompound) {
        super.readCustomDataFromNbt(compound)
        if (compound.contains("showing_age")) {
            this.setShowingAge(compound.getInt("showing_age"))
        }else {
            this.setShowingAge(random.nextInt(1000)+18)
        }
    }

    override fun getBreedingAge(): Int {
        return -1
    }

    override fun getRandomSkin(): String? {
        return NekoSkinRegistry.getRandomSkin(ToNekoEntities.ADVENTURER_NEKO);
    }
}