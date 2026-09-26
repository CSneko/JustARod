package org.cneko.justarod.config;

import java.nio.file.Path;

/**
 * JustARod 配置：聚焦生理/医学系统的可调参数。
 * 配置文件位于游戏目录下 config/justarod.json，结构参考 toNeko 的配置体系。
 */
public class JRConfig {
    public static final String CONFIG_FILE = "config/justarod.json";

    public static final ConfigBuilder CONFIG_BUILDER = new ConfigBuilder(Path.of(CONFIG_FILE))
            // ===== 怀孕 =====
            .addFloat("pregnancy.ectopic_chance", 0.02f, null,
                    "每次怀孕时发生宫外孕的基础概率（受体质等影响后仍不会低于原版的保底值，设为 0 可完全禁用宫外孕）",
                    "Base chance of ectopic pregnancy per conception (set 0 to disable)")
            .addFloat("pregnancy.hydatidiform_mole_chance", 0.01f, null,
                    "每次怀孕时出现葡萄胎的基础概率（设为 0 可完全禁用葡萄胎）",
                    "Base chance of hydatidiform mole per conception (set 0 to disable)")
            .addFloat("pregnancy.birth_control_fail_chance", 0.1f, null,
                    "避孕药的失败概率（服药状态下仍有该概率怀孕；设为 0 则药效绝对可靠）",
                    "Chance that birth control pills fail while active (0 = always works)")
            .addInt("pregnancy.duration_days", 5, null,
                    "怀孕总时长（单位：游戏日）。分娩伤害与孕期事件按此比例推进",
                    "Total pregnancy duration in in-game days")
            .addFloat("pregnancy.childbirth_damage", 6.0f, null,
                    "自然分娩时受到的伤害（颗心会疼的喵）",
                    "Damage taken during childbirth")
            .addFloat("pregnancy.miscarry_damage", 10.0f, null,
                    "流产时受到的伤害",
                    "Damage taken when miscarrying")
            .addFloat("pregnancy.corpus_luteum_severe_chance", 0.2f, null,
                    "黄体破裂发展为重症（腹腔大出血）的概率，其余为轻症",
                    "Chance that a ruptured corpus luteum becomes severe (internal bleeding)")
            // ===== 医疗 =====
            .addFloat("medical.gender_anomaly_chance", 0.2f, null,
                    "使用性别转换药水时出现性别异常（双性别/无性别）的概率",
                    "Chance of a gender anomaly when using gender change potions")
            .addInt("medical.chemo_reduction_days", 3, null,
                    "每粒化疗药物可削减的卵巢癌病程（游戏日）；乳腺癌辅助抑制效果固定为 1 日不变",
                    "Ovarian cancer progression (in days) removed per chemo dose")
            .addInt("medical.sanitary_towel_comfort_minutes", 10, null,
                    "卫生巾舒适效果的持续时间（现实分钟）",
                    "Comfort effect duration of sanitary towels (real minutes)")
            // ===== 激素 =====
            .addInt("hormone.hrt_flip_days", 10, null,
                    "激素替代疗法（HRT）完成性别重塑所需的持续服药天数",
                    "Days on HRT within the golden hormone range required for transition")
            .addFloat("hormone.t_overdose_threshold", 1200.0f, null,
                    "血液睾酮（T）过量阈值，超过后开始出现痤疮、脱发等副作用",
                    "Blood testosterone level above which overdose side effects appear")
            .addFloat("hormone.e_overdose_threshold", 800.0f, null,
                    "血液雌二醇（E2）过量阈值，超过后开始出现副作用",
                    "Blood estradiol level above which overdose side effects appear")
            // ===== 熬夜 =====
            .addBoolean("fatigue.enabled", false, null,
                    "是否启用熬夜机制：夜里不睡觉会积累疲劳，过劳会导致虚弱甚至走着走着睡着",
                    "Enable the stay-up-late system: staying awake at night builds fatigue")
            .addFloat("fatigue.microsleep_chance", 0.35f, null,
                    "重度以上疲劳时每次微睡判定的成功概率（0 为完全不微睡）",
                    "Chance for a microsleep roll to succeed while heavily fatigued (0 disables microsleep)")
            .addFloat("fatigue.sudden_death_chance", 0.05f, null,
                    "极度疲劳时每次微睡演变成猝死（心脏骤停）的概率，设为 0 可完全禁用猝死",
                    "Chance for a microsleep at extreme fatigue to become sudden cardiac death (0 disables it)")
            .addInt("fatigue.pregnant_multiplier", 2, null,
                    "怀孕时疲劳积累速度的倍数（1 表示与常人相同）",
                    "Fatigue accumulation multiplier while pregnant (1 = same as normal)")
            .addInt("fatigue.shenbao_relief_ticks", 7200, null,
                    "每份肾宝可以消除的疲劳值（20 tick = 1 秒）",
                    "Fatigue removed per serving of Shenbao (20 ticks = 1 second)");

    /** 初始化配置文件（缺失的键会被补全），在模组初始化时调用 */
    public static void load() {
        CONFIG_BUILDER.build();
    }

    public static void save() {
        CONFIG_BUILDER.save();
    }

    private static float getFloat(String key, float min) {
        float v;
        try {
            v = CONFIG_BUILDER.getConfig().getFloat(key);
        } catch (Exception e) {
            v = ((Number) CONFIG_BUILDER.get(key).value).floatValue();
        }
        return Math.max(v, min);
    }

    private static boolean getBoolean(String key) {
        try {
            return CONFIG_BUILDER.getConfig().getBoolean(key);
        } catch (Exception e) {
            Object value = CONFIG_BUILDER.get(key).value;
            return value instanceof Boolean b && b;
        }
    }

    public static int getInt(String key, int min) {
        int v;
        try {
            v = CONFIG_BUILDER.getConfig().getInt(key);
        } catch (Exception e) {
            v = ((Number) CONFIG_BUILDER.get(key).value).intValue();
        }
        return Math.max(v, min);
    }

    // ===== 怀孕 =====

    public static float getEctopicPregnancyChance() {
        return getFloat("pregnancy.ectopic_chance", 0f);
    }

    public static float getHydatidiformMoleChance() {
        return getFloat("pregnancy.hydatidiform_mole_chance", 0f);
    }

    public static float getBirthControlFailChance() {
        float v = getFloat("pregnancy.birth_control_fail_chance", 0f);
        return Math.min(v, 1.0f);
    }

    public static int getPregnancyDurationDays() {
        return getInt("pregnancy.duration_days", 1);
    }

    public static float getChildbirthDamage() {
        return getFloat("pregnancy.childbirth_damage", 0f);
    }

    public static float getMiscarryDamage() {
        return getFloat("pregnancy.miscarry_damage", 0f);
    }

    public static float getCorpusLuteumSevereChance() {
        float v = getFloat("pregnancy.corpus_luteum_severe_chance", 0f);
        return Math.min(v, 1.0f);
    }

    // ===== 医疗 =====

    public static float getGenderAnomalyChance() {
        float v = getFloat("medical.gender_anomaly_chance", 0f);
        return Math.min(v, 1.0f);
    }

    public static int getChemoReductionDays() {
        return getInt("medical.chemo_reduction_days", 0);
    }

    public static int getSanitaryTowelComfortMinutes() {
        return getInt("medical.sanitary_towel_comfort_minutes", 0);
    }

    // ===== 激素 =====

    public static int getHrtFlipDays() {
        return getInt("hormone.hrt_flip_days", 1);
    }

    public static float getTOverdoseThreshold() {
        return getFloat("hormone.t_overdose_threshold", 1f);
    }

    public static float getEOverdoseThreshold() {
        return getFloat("hormone.e_overdose_threshold", 1f);
    }

    // ===== 熬夜 =====

    public static boolean isStayUpLateEnabled() {
        return getBoolean("fatigue.enabled");
    }

    public static float getMicrosleepChance() {
        float v = getFloat("fatigue.microsleep_chance", 0f);
        return Math.min(v, 1.0f);
    }

    public static float getSuddenDeathChance() {
        float v = getFloat("fatigue.sudden_death_chance", 0f);
        return Math.min(v, 1.0f);
    }

    public static int getPregnantFatigueMultiplier() {
        return getInt("fatigue.pregnant_multiplier", 1);
    }

    public static int getShenbaoReliefTicks() {
        return getInt("fatigue.shenbao_relief_ticks", 0);
    }
}
