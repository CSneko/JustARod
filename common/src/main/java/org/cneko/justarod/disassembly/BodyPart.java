package org.cneko.justarod.disassembly;

import net.minecraft.network.chat.Component;
import java.util.Locale;

public enum BodyPart {
    HEAD, LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG, LEFT_EYE, RIGHT_EYE;
    public int bit() { return 1 << ordinal(); }
    public String key() { return name().toLowerCase(Locale.ROOT); }
    public Component label() { return Component.translatable("bodypart.justarod." + key()); }
    public boolean arm() { return this == LEFT_ARM || this == RIGHT_ARM; }
    public boolean leg() { return this == LEFT_LEG || this == RIGHT_LEG; }
    public boolean eye() { return this == LEFT_EYE || this == RIGHT_EYE; }
    public boolean camera() { return this == HEAD || eye(); }
    public static BodyPart byIndex(int index) { return index >= 0 && index < values().length ? values()[index] : null; }
}
