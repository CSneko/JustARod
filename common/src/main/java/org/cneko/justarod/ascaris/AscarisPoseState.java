package org.cneko.justarod.ascaris;

/**
 * High-level pose states for Ascaris-chan covering v53 animations in ascaris.animation.json (49 animations).
 */
public enum AscarisPoseState {
    NORMAL(0, "normal"),
    SIT_GROUND(1, "sit"),
    SIT_CHAIR(2, "sit_chair"),
    PRONE(3, "prone"),
    SLEEP(4, "sleep"),
    CURL_UP(5, "curl"),
    COLLAPSE(6, "collapse"),
    TAIL_WRAP(7, "tail_wrap"),
    WAVE(8, "wave"),
    HAPPY(9, "happy"),
    LOOK_AROUND(10, "look_around"),
    REST(11, "rest"),
    YANDERE_STARE(12, "yandere_stare"),
    YANDERE_WRAP(13, "yandere_wrap"),
    YANDERE_WRAP_IDLE(14, "yandere_wrap_idle"),
    YANDERE_TIGHTEN(15, "yandere_tighten"),
    STALK(16, "stalk"),
    THREATEN(17, "threaten"),
    BED_WATCH(18, "bed_watch"),
    // v53 新增姿态（保持 0..18 存档 ID 完全向后兼容）
    BOAT(19, "boat"),
    RIDE(20, "ride"),
    SWIM(21, "swim"),
    SWIM_STAND(22, "swim_stand"),
    JUMP(23, "jump"),
    SNEAK(24, "sneak"),
    SNEAKING(25, "sneaking"),
    LADDER_UP(26, "ladder_up"),
    LADDER_STILLNESS(27, "ladder_stillness"),
    LADDER_DOWN(28, "ladder_down"),
    HURT(29, "hurt"),
    FLY(30, "fly"),
    ELYTRA(31, "elytra");

    private final int id;
    private final String commandName;

    AscarisPoseState(int id, String commandName) {
        this.id = id;
        this.commandName = commandName;
    }

    public int id() {
        return id;
    }

    public String commandName() {
        return commandName;
    }

    /**
     * States that animate their own eyes (happy / collapse / sleep / scared >~< curl_up)
     * or intentionally hold an unblinking yandere stare must pause the independent blink controller.
     */
    public boolean blocksBlink() {
        return this == HAPPY
                || this == COLLAPSE
                || this == SLEEP
                || this == CURL_UP
                || this == YANDERE_STARE
                || this == YANDERE_WRAP
                || this == YANDERE_WRAP_IDLE
                || this == YANDERE_TIGHTEN
                || this == STALK
                || this == THREATEN
                || this == BED_WATCH;
    }

    public boolean isWrapping() {
        return this == TAIL_WRAP
                || this == YANDERE_WRAP
                || this == YANDERE_WRAP_IDLE
                || this == YANDERE_TIGHTEN;
    }

    public boolean isYanderePose() {
        return this == YANDERE_STARE
                || this == YANDERE_WRAP
                || this == YANDERE_WRAP_IDLE
                || this == YANDERE_TIGHTEN
                || this == STALK
                || this == THREATEN
                || this == BED_WATCH;
    }

    public boolean locksLocomotion() {
        return this == SIT_GROUND
                || this == SIT_CHAIR
                || this == PRONE
                || this == SLEEP
                || this == CURL_UP
                || this == COLLAPSE
                || this == TAIL_WRAP
                || this == REST
                || this == YANDERE_STARE
                || this == YANDERE_WRAP
                || this == YANDERE_WRAP_IDLE
                || this == YANDERE_TIGHTEN
                || this == THREATEN
                || this == BED_WATCH
                || this == BOAT
                || this == RIDE
                || this == SWIM
                || this == SWIM_STAND
                || this == JUMP
                || this == SNEAK
                || this == SNEAKING
                || this == LADDER_UP
                || this == LADDER_STILLNESS
                || this == LADDER_DOWN
                || this == FLY
                || this == ELYTRA;
    }

    public static AscarisPoseState byId(int id) {
        for (AscarisPoseState state : values()) {
            if (state.id == id) return state;
        }
        return NORMAL;
    }

    public static AscarisPoseState byName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase(java.util.Locale.ROOT);
        for (AscarisPoseState state : values()) {
            if (state.commandName.equals(lower) || state.name().toLowerCase(java.util.Locale.ROOT).equals(lower)) {
                return state;
            }
        }
        if ("idle".equals(lower) || "walk".equals(lower) || "run".equals(lower) || "crawl".equals(lower)) {
            return NORMAL;
        }
        if ("curl_up".equals(lower) || "curl_idle".equals(lower)) return CURL_UP;
        if ("collapse_idle".equals(lower)) return COLLAPSE;
        if ("tail_wrap_idle".equals(lower)) return TAIL_WRAP;
        if ("ladder".equals(lower)) return LADDER_UP;
        if ("swimming".equals(lower)) return SWIM;
        if ("elytra_fly".equals(lower)) return ELYTRA;
        if ("attacked".equals(lower)) return HURT;
        return null;
    }
}
