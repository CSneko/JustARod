package org.cneko.justarod.client.gui;


@SuppressWarnings("LombokGetterMayBeUsed")
public enum ScanType {
    UTERUS("uterus"),
    ABDOMEN("abdomen");

    private final String id;

    ScanType(String id) {
        this.id = id;
    }

    public static ScanType fromId(String id) {
        for (ScanType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }
    public String getId() {
        return id;
    }
}
