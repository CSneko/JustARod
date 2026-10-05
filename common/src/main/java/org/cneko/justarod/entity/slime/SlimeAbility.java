package org.cneko.justarod.entity.slime;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Stable saved/network IDs; ordinary prey never clears an absorbed ability. */
public enum SlimeAbility {
    NONE(0, "none", Items.SLIME_BALL),
    CREEPER(1, "creeper", Items.CREEPER_HEAD),
    ENDERMAN(2, "enderman", Items.ENDER_PEARL),
    SHULKER(3, "shulker", Items.SHULKER_SHELL);

    private final int id;
    private final String key;
    private final Item icon;

    SlimeAbility(int id, String key, Item icon) {
        this.id = id;
        this.key = key;
        this.icon = icon;
    }

    public int id() { return id; }
    public String translationKey() { return "justarod.slime.ability." + key; }
    public Item icon() { return icon; }

    public static SlimeAbility byId(int id) {
        for (SlimeAbility ability : values()) if (ability.id == id) return ability;
        return NONE;
    }

    public static SlimeAbility fromPrey(EntityType<?> type) {
        if (type == EntityType.CREEPER) return CREEPER;
        if (type == EntityType.ENDERMAN) return ENDERMAN;
        if (type == EntityType.SHULKER) return SHULKER;
        return NONE;
    }
}
