package org.cneko.justarod.property

import org.cneko.justarod.entity.Powerable
import org.cneko.justarod.entity.Pregnant
import java.util.*

object JRRegistry {
    val PROPERTIES = mutableListOf<JRProperty<*>>()

    // ==================== 基础与性别状态 ====================
    val POWER = registerDouble("power", "体力", { if (it is Powerable){it.power}else 0.0 }, { e, v -> if (e is Powerable){(e as Powerable).power = v} })
    val IS_MALE = registerBool("is_male", "男性", { it.isMale }, { e, v -> e.isMale = v })
    val IS_FEMALE = registerBool("is_female", "女性", { it.isFemale }, { e, v -> e.isFemale = v })
    val HAS_UTERUS = registerBool("has_uterus", "拥有子宫", { it.hasUterus() }, { e, v -> e.setHasUterus(v) })
    val STERILIZATION = registerBool("sterilization", "绝育", { it.isSterilization }, { e, v -> e.isSterilization = v })
    val HAS_HYMEN = registerBool("has_hymen", "处女膜完整", { it.hasHymen() }, { e, v -> e.setHasHymen(v) })

    // ==================== 怀孕与生理 ====================
    val PREGNANT = registerTime("pregnant", "怀孕时间", { it.pregnant }, { e, v -> e.pregnant = v })
    val BABY_COUNT = registerInt("baby_count", "胎儿数量", { it.babyCount }, { e, v -> e.babyCount = v })
    val CHILDREN_TYPE = registerOptEntity("children_type", "胚胎类型", { Optional.of(it.childrenType) }, { e, v -> e.childrenType = v.get() })

    val MENSTRUATION = registerTime("menstruation", "生理期时间", { it.menstruation }, { e, v -> e.menstruation = v })
    val MENSTRUATION_COMFORT = registerTime("menstruation_comfort", "卫生巾有效时间", { it.menstruationComfort }, { e, v -> e.menstruationComfort = v })

    val EXCRETION = registerTime("excretion", "憋粑粑时间", { it.excretion }, { e, v -> e.excretion = v })
    val URINATION = registerTime("urination", "憋尿时间", { it.urination }, { e, v -> e.urination = v })
    val BRITH_CONTROLLING = registerTime("brith_controlling", "避孕药效", { it.brithControlling }, { e, v -> e.brithControlling = v })

    // ==================== 激素系统 ====================
    val TESTOSTERONE = registerFloat("testosterone", "睾酮 (T)", { it.testosterone }, { e, v -> e.testosterone = v })
    val ESTROGEN = registerFloat("estrogen", "雌激素 (E)", { it.estrogen }, { e, v -> e.estrogen = v })
    val PROGESTERONE = registerFloat("progesterone", "孕酮 (P)", { it.progesterone }, { e, v -> e.progesterone = v })

    // ==================== 疾病与异常 (数值类) ====================
    val AIDS = registerTime("aids", "艾滋病", { it.aids }, { e, v -> e.aids = v }, isDisease = true)
    val HPV = registerTime("hpv", "HPV感染", { it.hpv }, { e, v -> e.hpv = v }, isDisease = true)
    val SYPHILIS = registerTime("syphilis", "梅毒", { it.syphilis }, { e, v -> e.syphilis = v }, isDisease = true)
    val UTERINE_COLD = registerTime("uterine_cold", "宫寒", { it.uterineCold }, { e, v -> e.uterineCold = v }, isDisease = true)
    val URETHRITIS = registerTime("urethritis", "尿道炎", { it.urethritis }, { e, v -> e.urethritis = v }, isDisease = true)
    val PROSTATITIS = registerTime("prostatitis", "前列腺炎", { it.prostatitis }, { e, v -> e.prostatitis = v }, isDisease = true)
    val HEMORRHOIDS = registerTime("hemorrhoids", "痔疮", { it.hemorrhoids }, { e, v -> e.hemorrhoids = v }, isDisease = true)
    val CATARACT = registerTime("cataract", "白内障", { it.cataract }, { e, v -> e.cataract = v }, isDisease = true)
    val OVARIAN_CANCER = registerTime("ovarian_cancer", "卵巢癌", { it.ovarianCancer }, { e, v -> e.ovarianCancer = v }, isDisease = true)
    val BREAST_CANCER = registerTime("breast_cancer", "乳腺癌", { it.breastCancer }, { e, v -> e.breastCancer = v }, isDisease = true)
    val CORPUS_LUTEUM_RUPTURE = registerTime("corpus_luteum_rupture", "黄体破裂内出血", { it.corpusLuteumRupture }, { e, v -> e.corpusLuteumRupture = v }, isDisease = true)

    // ==================== 疾病与异常 (布尔类) ====================
    val IMMUNE_AIDS = registerBool("immune_aids", "艾滋病免疫", { it.isImmune2Aids }, { e, v -> e.isImmune2Aids = v })
    val IMMUNE_HPV = registerBool("immune_hpv", "HPV免疫", { it.isImmune2HPV }, { e, v -> e.isImmune2HPV = v })
    val PCOS = registerBool("pcos", "多囊卵巢综合征", { it.isPCOS }, { e, v -> e.isPCOS = v }, isDisease = true)
    val ECTOPIC_PREGNANCY = registerBool("ectopic_pregnancy", "宫外孕", { it.isEctopicPregnancy }, { e, v -> e.isEctopicPregnancy = v }, isDisease = true)
    val HYDATIDIFORM_MOLE = registerBool("hydatidiform_mole", "葡萄胎", { it.isHydatidiformMole }, { e, v -> e.isHydatidiformMole = v }, isDisease = true)
    val IMPERFORATE_HYMEN = registerBool("imperforate_hymen", "处女膜闭锁", { it.isImperforateHymen() }, { e, v -> e.setImperforateHymen(v) }, isDisease = true)
    val SEVERE_RUPTURE = registerBool("severe_rupture", "重症黄体破裂", { it.isSevereCorpusLuteumRupture }, { e, v -> e.isSevereCorpusLuteumRupture = v }, isDisease = true)

    // ==================== 外科特征 / 改造 ====================
    val ORCHIECTOMY = registerBool("orchiectomy", "睾丸切除", { it.isOrchiectomy }, { e, v -> e.isOrchiectomy = v }, isDisease = true)
    val AMPUTATED = registerBool("amputated", "截肢", { it.isAmputated }, { e, v -> e.isAmputated = v }, isDisease = true)
    val PROTOGYNY_ENABLED = registerBool("protogyny_enabled", "雌转雄体质启用", { it.isProtogynyEnabled }, { e, v -> e.isProtogynyEnabled = v })
    val UNDERGOING_PROTOGYNY = registerBool("undergoing_protogyny", "正在雌转雄", { it.isUndergoingProtogyny }, { e, v -> e.isUndergoingProtogyny = v })
    val PROTOGYNY_PROGRESS = registerInt("protogyny_progress", "雌转雄进度", { it.protogynyProgress }, { e, v -> e.protogynyProgress = v })


    // ===== 内部辅助注册方法 =====
    private fun registerBool(name: String, desc: String, get: (Pregnant) -> Boolean, set: (Pregnant, Boolean) -> Unit, isDisease: Boolean = false): JRProperty<Boolean> {
        val p = JRBoolProperty(name, desc, get, set, isDisease = isDisease)
        PROPERTIES.add(p)
        return p
    }
    private fun registerTime(name: String, desc: String, get: (Pregnant) -> Int, set: (Pregnant, Int) -> Unit, isDisease: Boolean = false): JRProperty<Int> {
        val p = JRTimeProperty(name, desc, get, set, isDisease = isDisease)
        PROPERTIES.add(p)
        return p
    }
    private fun registerInt(name: String, desc: String, get: (Pregnant) -> Int, set: (Pregnant, Int) -> Unit): JRProperty<Int> {
        val p = JRIntProperty(name, desc, get, set)
        PROPERTIES.add(p)
        return p
    }
    private fun registerFloat(name: String, desc: String, get: (Pregnant) -> Float, set: (Pregnant, Float) -> Unit): JRProperty<Float> {
        val p = JRFloatProperty(name, desc, get, set)
        PROPERTIES.add(p)
        return p
    }
    private fun registerDouble(name: String, desc: String, get: (Pregnant) -> Double, set: (Pregnant, Double) -> Unit): JRProperty<Double> {
        val p = JRDoubleProperty(name, desc, get, set)
        PROPERTIES.add(p)
        return p
    }
    private fun registerOptEntity(name: String, desc: String, get: (Pregnant) -> java.util.Optional<net.minecraft.entity.EntityType<*>>, set: (Pregnant, java.util.Optional<net.minecraft.entity.EntityType<*>>) -> Unit): JRProperty<java.util.Optional<net.minecraft.entity.EntityType<*>>> {
        val p = JREntityTypeProperty(name, desc, get, set)
        PROPERTIES.add(p)
        return p
    }
}