package org.cneko.justarod.disassembly;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.cneko.justarod.JRIds;
import java.util.EnumMap;

public final class DisassemblyItems {
    private static final EnumMap<BodyPart, DetachedPartItem> ITEMS = new EnumMap<>(BodyPart.class);
    public static void init() {
        for (BodyPart part : BodyPart.values()) {
            String id = "detached_" + part.key();
            ITEMS.put(part, Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath("justarod", id),
                    new DetachedPartItem(JRIds.itemProps(id), part)));
        }
    }
    public static DetachedPartItem get(BodyPart part) { return ITEMS.get(part); }
}
