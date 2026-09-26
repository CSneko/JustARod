package org.cneko.justarod.entity;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.cneko.toneko.common.mod.entities.NekoEntity;
import org.cneko.justarod.entity.slime.TamedSlimeEntity;

import static org.cneko.justarod.Justarod.MODID;

/*
可恶的连接器有Bug喵！不让咱用Fabric的属性喵！！害得我整夜整夜地修复。
超市你喵！超市你喵！超市你喵！超市你喵！超市你喵！超市你喵！超市你喵！超市你喵！超市你喵！超市你喵！超市你喵！
超市你喵！超市你喵！超市你喵！超市你喵！超市你喵！超市你喵！超市你喵！超市你喵！超市你喵！超市你喵！超市你喵！
超市你喵！超市你喵！超市你喵！超市你喵！超市你喵！超市你喵！超市你喵！超市你喵！超市你喵！
 */
public class JREntities {
    public static final Identifier SEEEEEX_NEKO_ID = Identifier.fromNamespaceAndPath(MODID, "seeeeeex_neko");
    public static final ResourceKey<EntityType<?>> SEEEEEX_NEKO_KEY = ResourceKey.create(Registries.ENTITY_TYPE, SEEEEEX_NEKO_ID);
    public static final EntityType<SeeeeexNekoEntity> SEEEEEX_NEKO = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            SEEEEEX_NEKO_ID,
            EntityType.Builder.of(SeeeeexNekoEntity::new,MobCategory.CREATURE)//.createMob(SeeeeexNekoEntity::new, SpawnGroup.CREATURE,
                    //builder ->builder.defaultAttributes(SeeeeexNekoEntity::createNekoAttributes))
                    .sized(0.5f,1.7f).eyeHeight(1.6f)
                    .build(SEEEEEX_NEKO_KEY)
    );
    public static final Identifier LOLI_NEKO_ID = Identifier.fromNamespaceAndPath(MODID, "loli_neko");
    public static final ResourceKey<EntityType<?>> LOLI_NEKO_KEY = ResourceKey.create(Registries.ENTITY_TYPE, LOLI_NEKO_ID);
    public static final EntityType<LoliNekoEntity> LOLI_NEKO = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            LOLI_NEKO_ID,
           // FabricEntityType.Builder.createMob(LoliNekoEntity::new,SpawnGroup.CREATURE,
           //         builder -> builder.defaultAttributes(NekoEntity::createNekoAttributes))
            EntityType.Builder.of(LoliNekoEntity::new,MobCategory.CREATURE)
            .sized(0.5f,1.7f).eyeHeight(0.4f)
                    .build(LOLI_NEKO_KEY)
    );
    public static final Identifier ROD_ID = Identifier.fromNamespaceAndPath(MODID, "rod");
    public static final ResourceKey<EntityType<?>> ROD_KEY = ResourceKey.create(Registries.ENTITY_TYPE, ROD_ID);
    public static final EntityType<RodEntity> ROD = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ROD_ID,
          //  FabricEntityType.Builder.createMob(RodEntity::new,SpawnGroup.CREATURE,
            //        builder -> builder.defaultAttributes(RodEntity.Companion::createRodAttribute))
            EntityType.Builder.of(RodEntity::new, MobCategory.CREATURE)
            .sized(0.5f,0.5f).eyeHeight(0.4f)
                    .build(ROD_KEY)
    );
    public static final Identifier ICED_TEA_PROJECTILE_ID = Identifier.fromNamespaceAndPath(MODID, "iced_tea");
    public static final ResourceKey<EntityType<?>> ICED_TEA_PROJECTILE_KEY = ResourceKey.create(Registries.ENTITY_TYPE, ICED_TEA_PROJECTILE_ID);
    public static final EntityType<IcedTeaProjectileEntity> ICED_TEA_PROJECTILE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ICED_TEA_PROJECTILE_ID,
            EntityType.Builder.<IcedTeaProjectileEntity>of(IcedTeaProjectileEntity::new, MobCategory.MISC)
                    .sized(1f, 1f)
                    .updateInterval(10)
                    .build(ICED_TEA_PROJECTILE_KEY)
    );
    // ===== 驯服史莱姆 =====
    public static final Identifier TAMED_SLIME_ID = Identifier.fromNamespaceAndPath(MODID, "tamed_slime");
    public static final ResourceKey<EntityType<?>> TAMED_SLIME_KEY = ResourceKey.create(Registries.ENTITY_TYPE, TAMED_SLIME_ID);
    public static final EntityType<TamedSlimeEntity> TAMED_SLIME = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            TAMED_SLIME_ID,
            EntityType.Builder.of(TamedSlimeEntity::new, MobCategory.CREATURE)
                    // 基准尺寸 0.5 格：渲染时按等级放大到 1/1.5/2/2.85/4/5.5 格
                    .sized(0.5f, 0.5f)
                    .eyeHeight(0.4f)
                    .build(TAMED_SLIME_KEY)
    );

    public static void init(){
        // 驯服史莱姆的属性（26.x 沿用 Fabric 默认属性注册表）
        FabricDefaultAttributeRegistry.register(TAMED_SLIME, TamedSlimeEntity.createSlimeAttributes());
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_HILL), MobCategory.CREATURE, SEEEEEX_NEKO, 20, 1, 1);
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_BEACH), MobCategory.CREATURE, LOLI_NEKO, 10, 1, 1);
    }
}
