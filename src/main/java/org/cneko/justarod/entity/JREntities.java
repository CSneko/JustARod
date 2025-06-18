package org.cneko.justarod.entity;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.util.Identifier;
import org.cneko.toneko.common.mod.entities.NekoEntity;

import static org.cneko.justarod.Justarod.MODID;

/*
可恶的连接器有Bug喵！不让咱用Fabric的属性喵！！害得我整夜整夜地修复。
超市你喵！超市你喵！超市你喵！超市你喵！超市你喵！超市你喵！超市你喵！超市你喵！超市你喵！超市你喵！超市你喵！
超市你喵！超市你喵！超市你喵！超市你喵！超市你喵！超市你喵！超市你喵！超市你喵！超市你喵！超市你喵！超市你喵！
超市你喵！超市你喵！超市你喵！超市你喵！超市你喵！超市你喵！超市你喵！超市你喵！超市你喵！
 */
public class JREntities {
    public static final Identifier SEEEEEX_NEKO_ID = Identifier.of(MODID, "seeeeeex_neko");
    public static final EntityType<SeeeeexNekoEntity> SEEEEEX_NEKO = Registry.register(
            Registries.ENTITY_TYPE,
            SEEEEEX_NEKO_ID,
            EntityType.Builder.create(SeeeeexNekoEntity::new,SpawnGroup.CREATURE)//.createMob(SeeeeexNekoEntity::new, SpawnGroup.CREATURE,
                    //builder ->builder.defaultAttributes(SeeeeexNekoEntity::createNekoAttributes))
                    .dimensions(0.5f,1.7f).eyeHeight(1.6f)
                    .build()
    );
    public static final Identifier LOLI_NEKO_ID = Identifier.of(MODID, "loli_neko");
    public static final EntityType<LoliNekoEntity> LOLI_NEKO = Registry.register(
            Registries.ENTITY_TYPE,
            LOLI_NEKO_ID,
           // FabricEntityType.Builder.createMob(LoliNekoEntity::new,SpawnGroup.CREATURE,
           //         builder -> builder.defaultAttributes(NekoEntity::createNekoAttributes))
            EntityType.Builder.create(LoliNekoEntity::new,SpawnGroup.CREATURE)
            .dimensions(0.5f,1.7f).eyeHeight(0.4f)
                    .build()
    );
    public static final Identifier ROD_ID = Identifier.of(MODID, "rod");
    public static final EntityType<RodEntity> ROD = Registry.register(
            Registries.ENTITY_TYPE,
            ROD_ID,
          //  FabricEntityType.Builder.createMob(RodEntity::new,SpawnGroup.CREATURE,
            //        builder -> builder.defaultAttributes(RodEntity.Companion::createRodAttribute))
            EntityType.Builder.create(RodEntity::new, SpawnGroup.CREATURE)
            .dimensions(0.5f,0.5f).eyeHeight(0.4f)
                    .build()
    );
    public static final Identifier ICED_TEA_PROJECTILE_ID = Identifier.of(MODID, "iced_tea");
    public static final EntityType<IcedTeaProjectileEntity> ICED_TEA_PROJECTILE = Registry.register(
            Registries.ENTITY_TYPE,
            ICED_TEA_PROJECTILE_ID,
            EntityType.Builder.<IcedTeaProjectileEntity>create(IcedTeaProjectileEntity::new, SpawnGroup.MISC)
                    .dimensions(1f, 1f)
                    .trackingTickInterval(10)
                    .build()
    );
    public static void init(){
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_HILL), SpawnGroup.CREATURE, SEEEEEX_NEKO, 20, 1, 1);
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_BEACH), SpawnGroup.CREATURE, LOLI_NEKO, 10, 1, 1);
    }
}
