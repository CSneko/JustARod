package org.cneko.justarod.entity;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import static org.cneko.justarod.Justarod.MODID;

public class JREntities {
    public static final Identifier SEEEEEX_NEKO_ID = Identifier.of(MODID, "seeeeeex_neko");
    public static final EntityType<SeeeeexNekoEntity> SEEEEEX_NEKO = Registry.register(
            Registries.ENTITY_TYPE,
            SEEEEEX_NEKO_ID,
            FabricEntityType.Builder.createMob(SeeeeexNekoEntity::new, SpawnGroup.CREATURE,
                    builder ->builder.defaultAttributes(SeeeeexNekoEntity::createNekoAttributes))
                    .dimensions(0.5f,1.7f).eyeHeight(1.6f)
                    .build()
    );
    public static void init(){
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), SpawnGroup.CREATURE, SEEEEEX_NEKO, 10, 1, 1);
    }
}
