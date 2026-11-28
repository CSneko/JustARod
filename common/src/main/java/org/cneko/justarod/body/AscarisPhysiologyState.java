package org.cneko.justarod.body;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * 粉白蛔虫娘单槽位硬核生理内稳态模型（持久化在 {@link BodyRegionData} 中）。
 *
 * <p>涵盖真实似蚓蛔线虫（<em>Ascaris lumbricoides</em>）的六大生理系统：
 * <ol>
 *   <li>静水骨骼假体腔液压（Hydrostatic Skeleton，正常 ~14–18 kPa / 55–75%）；</li>
 *   <li>厌氧糖酵解糖原储备（Glycogen Reserve，由肠道微绒毛吸收食糜供能）；</li>
 *   <li>H 形排泄管渗透压与代谢废物负荷（Metabolic Waste / Ammonia & VFA Load）；</li>
 *   <li>核心体温与炎症发热响应（Core Temperature °C）；</li>
 *   <li>完整消化流水线（咽部负压抽吸 → 移动食糜团 → 微绒毛吸收 → 直肠充盈与排空）；</li>
 *   <li>双管生殖周期（盘绕卵巢卵母发育 → 输卵管/受精囊输送 → 双子宫充盈 → 阴道宫缩产卵）与双药药理发作。</li>
 * </ol>
 */
public final class AscarisPhysiologyState {

    public static final int DRUG_NONE = 0;
    /** 宝塔糖（枸橼酸哌嗪）：阻断神经肌肉接头 → 弛缓性麻痹、神经传导阻滞、静水压暴跌、括约肌松弛。 */
    public static final int DRUG_PIPERAZINE = 1;
    /** 阿苯达唑：抑制微管聚合 → 阻断肠上皮葡萄糖摄取、糖原枯竭、代谢废物堆积与炎症高热。 */
    public static final int DRUG_ALBENDAZOLE = 2;

    public static final int ACTIVITY_IDLE = 0;
    public static final int ACTIVITY_WALK = 1;
    public static final int ACTIVITY_RUN_JUMP = 2;
    public static final int ACTIVITY_CRAWL = 3;
    public static final int ACTIVITY_WRAP = 4;
    public static final int ACTIVITY_HURT_CURL = 5;
    public static final int ACTIVITY_SLEEP = 6;
    public static final int ACTIVITY_COLLAPSE = 7;

    public static final int ORGAN_CUTICLE = 0;
    public static final int ORGAN_MUSCLE = 1;
    public static final int ORGAN_GUT = 2;
    public static final int ORGAN_EXCRETORY = 3;
    public static final int ORGAN_NERVE = 4;
    public static final int ORGAN_REPRO = 5;

    public static final int MAX_BOLUSES = 6;
    public static final int MAX_BREACHES = 32;
    public static final int MAX_IMPULSES = 10;

    // ===== 核心内稳态（持久化） =====
    public float hydrostaticPressure = 64.0f; // 0..100 (换算 0..26 kPa)
    public float glycogen = 76.0f;            // 0..100
    public float metabolicWaste = 12.0f;      // 0..100
    public float temperatureC = 37.2f;        // 34.0..40.5 °C
    public float ovaryProgress = 28.0f;       // 0..100
    public float uterineLoad = 46.0f;         // 0..100
    public float rectalLoad = 14.0f;          // 0..100
    public int drugType = DRUG_NONE;
    public int drugTicks = 0;

    // ===== 物理流动食糜团与体壁破口（持久化） =====
    public final List<ChymeBolus> boluses = new ArrayList<>();
    public final List<TissueBreach> breaches = new ArrayList<>();

    // ===== 实时动态生理波（运行时态） =====
    public boolean peristalsisActive = false;
    public boolean hungerMmcWave = false;
    public double peristalsisX = -38.0;
    public int peristalsisCooldown = 40;

    public boolean ovipositing = false;
    public double ovipositionX = 12.0;
    public int ovipositionCooldown = 0;

    public int pharynxSuctionTicks = 0;
    public int microvilliBoostTicks = 0;
    public int hostActivity = ACTIVITY_IDLE;
    public float lastExcretoryEfficiency = 1.0f;
    public long lastSimTick = -1L;

    public final List<NerveImpulse> impulses = new ArrayList<>();

    public static final class ChymeBolus {
        public double rx;       // 相对 region center 的 X (-38 .. +42)
        public int rz;          // 肠道内侧车道 (-1, 0, +1)
        public int flavor;      // 0=粉蜜, 1=发光浆果, 2=蜂蜜金苹
        public float nutrients; // 0..100

        public ChymeBolus(double rx, int rz, int flavor, float nutrients) {
            this.rx = rx;
            this.rz = rz;
            this.flavor = flavor;
            this.nutrients = nutrients;
        }
    }

    public static final class TissueBreach {
        public final int rx;
        public final int y;
        public final int rz;
        public final int organKind;
        public final String blockId;
        public int repairProgress; // 0..100

        public TissueBreach(int rx, int y, int rz, int organKind, String blockId, int repairProgress) {
            this.rx = rx;
            this.y = y;
            this.rz = rz;
            this.organKind = organKind;
            this.blockId = blockId;
            this.repairProgress = repairProgress;
        }

        public BlockPos worldPos(long slot) {
            return new BlockPos(BodyRegionData.centerX(slot) + rx, y, BodyRegionData.centerZ(slot) + rz);
        }
    }

    public static final class NerveImpulse {
        public static final int KIND_SENSORY = 0;
        public static final int KIND_MOTOR = 1;
        public static final int KIND_PAIN = 2;
        public static final int KIND_REFLEX = 3;

        public double rx;
        public int direction; // -1 朝围咽神经环(-31) / +1 朝尾端(+42)
        public int kind;
        public String reflexTag;
        public int ttl;

        public NerveImpulse(double rx, int direction, int kind, String reflexTag, int ttl) {
            this.rx = rx;
            this.direction = direction;
            this.kind = kind;
            this.reflexTag = reflexTag;
            this.ttl = ttl;
        }
    }

    public int wallIntegrityPct() {
        return Mth.clamp(100 - breaches.size() * 6, 15, 100);
    }

    public float pressureKpa() {
        // 似蚓蛔线虫正常静水压 ~14–18 kPa（最高 ~26 kPa）
        return Math.round((hydrostaticPressure * 0.25f) * 10.0f) / 10.0f;
    }

    public boolean isParalyzedByPiperazine() {
        return drugType == DRUG_PIPERAZINE && drugTicks > 0;
    }

    public boolean isBlockedByAlbendazole() {
        return drugType == DRUG_ALBENDAZOLE && drugTicks > 0;
    }

    public boolean isSphincterRelaxed() {
        return isParalyzedByPiperazine() || hydrostaticPressure <= 30.0f;
    }

    public boolean hasFever() {
        return temperatureC >= 38.4f;
    }

    public boolean isHypothermic() {
        return temperatureC <= 35.8f;
    }

    public boolean hasAcidosis() {
        return metabolicWaste >= 70.0f;
    }

    public boolean isHypoglycemic() {
        return glycogen <= 22.0f;
    }

    public void clampAll() {
        hydrostaticPressure = Mth.clamp(hydrostaticPressure, 8.0f, 100.0f);
        glycogen = Mth.clamp(glycogen, 0.0f, 100.0f);
        metabolicWaste = Mth.clamp(metabolicWaste, 0.0f, 100.0f);
        temperatureC = Mth.clamp(temperatureC, 34.0f, 40.5f);
        ovaryProgress = Mth.clamp(ovaryProgress, 0.0f, 100.0f);
        uterineLoad = Mth.clamp(uterineLoad, 0.0f, 100.0f);
        rectalLoad = Mth.clamp(rectalLoad, 0.0f, 100.0f);
        if (drugTicks <= 0) {
            drugTicks = 0;
            drugType = DRUG_NONE;
        }
    }

    public void addBolus(double rx, int rz, int flavor, float nutrients) {
        if (boluses.size() >= MAX_BOLUSES) {
            boluses.removeFirst();
        }
        boluses.add(new ChymeBolus(rx, Mth.clamp(rz, -1, 1), Mth.clamp(flavor, 0, 2), Mth.clamp(nutrients, 10f, 100f)));
    }

    public void recordBreach(int rx, int y, int rz, int organKind, String blockId) {
        for (TissueBreach b : breaches) {
            if (b.rx == rx && b.y == y && b.rz == rz) {
                b.repairProgress = 0;
                return;
            }
        }
        if (breaches.size() >= MAX_BREACHES) {
            breaches.removeFirst();
        }
        breaches.add(new TissueBreach(rx, y, rz, organKind, blockId, 0));
    }

    public void launchImpulse(double rx, int direction, int kind, String reflexTag) {
        if (isParalyzedByPiperazine()) {
            return; // 枸橼酸哌嗪阻断神经肌肉接头，动作电位无法传导！
        }
        if (impulses.size() >= MAX_IMPULSES) {
            impulses.removeFirst();
        }
        impulses.add(new NerveImpulse(rx, direction >= 0 ? 1 : -1, kind, reflexTag == null ? "" : reflexTag, 50));
    }

    public int packVitalsInt() {
        int p = Mth.clamp(Math.round(hydrostaticPressure), 0, 100);
        int g = Mth.clamp(Math.round(glycogen), 0, 100);
        int w = Mth.clamp(Math.round(metabolicWaste), 0, 100);
        int tempTenths = Mth.clamp(Math.round(temperatureC * 10.0f) - 300, 0, 127);
        return (p & 0x7F) | ((g & 0x7F) << 7) | ((w & 0x7F) << 14) | ((tempTenths & 0x7F) << 21);
    }

    public int packOrgansInt() {
        int u = Mth.clamp(Math.round(uterineLoad), 0, 100);
        int r = Mth.clamp(Math.round(rectalLoad), 0, 100);
        int wi = Mth.clamp(wallIntegrityPct(), 0, 100);
        int drug = Mth.clamp(drugType, 0, 3);
        int peri = peristalsisActive ? 1 : 0;
        int act = Mth.clamp(hostActivity, 0, 7);
        return (u & 0x7F) | ((r & 0x7F) << 7) | ((wi & 0x7F) << 14)
                | ((drug & 0x3) << 21) | (peri << 23) | ((act & 0x7) << 24);
    }

    public int packCycleFlagsInt() {
        int ov = Mth.clamp(Math.round(ovaryProgress), 0, 100);
        int bol = Mth.clamp(boluses.size(), 0, 15);
        int br = Mth.clamp(breaches.size(), 0, 31);
        int flags = 0;
        if (hasFever()) flags |= 1;
        if (isHypoglycemic()) flags |= 2;
        if (hasAcidosis()) flags |= 4;
        if (ovipositing) flags |= 8;
        if (hydrostaticPressure <= 35.0f) flags |= 16;
        if (hydrostaticPressure >= 80.0f) flags |= 32;
        if (isParalyzedByPiperazine()) flags |= 64;
        if (pharynxSuctionTicks > 0) flags |= 128;
        return (ov & 0x7F) | ((bol & 0xF) << 7) | ((br & 0x1F) << 11) | ((flags & 0xFF) << 16);
    }

    public CompoundTag toTag() {
        clampAll();
        CompoundTag tag = new CompoundTag();
        tag.putFloat("pressure", hydrostaticPressure);
        tag.putFloat("glycogen", glycogen);
        tag.putFloat("waste", metabolicWaste);
        tag.putFloat("temp", temperatureC);
        tag.putFloat("ovary", ovaryProgress);
        tag.putFloat("uterus", uterineLoad);
        tag.putFloat("rectal", rectalLoad);
        tag.putInt("drug_type", drugType);
        tag.putInt("drug_ticks", drugTicks);

        ListTag bolusList = new ListTag();
        for (ChymeBolus b : boluses) {
            CompoundTag bt = new CompoundTag();
            bt.putDouble("rx", b.rx);
            bt.putInt("rz", b.rz);
            bt.putInt("flavor", b.flavor);
            bt.putFloat("nutrients", b.nutrients);
            bolusList.add(bt);
        }
        tag.put("boluses", bolusList);

        ListTag breachList = new ListTag();
        for (TissueBreach br : breaches) {
            CompoundTag bt = new CompoundTag();
            bt.putInt("rx", br.rx);
            bt.putInt("y", br.y);
            bt.putInt("rz", br.rz);
            bt.putInt("organ", br.organKind);
            bt.putString("block", br.blockId);
            bt.putInt("repair", br.repairProgress);
            breachList.add(bt);
        }
        tag.put("breaches", breachList);
        return tag;
    }

    public static AscarisPhysiologyState fromTag(CompoundTag tag) {
        AscarisPhysiologyState st = new AscarisPhysiologyState();
        st.hydrostaticPressure = tag.getFloatOr("pressure", 64.0f);
        st.glycogen = tag.getFloatOr("glycogen", 76.0f);
        st.metabolicWaste = tag.getFloatOr("waste", 12.0f);
        st.temperatureC = tag.getFloatOr("temp", 37.2f);
        st.ovaryProgress = tag.getFloatOr("ovary", 28.0f);
        st.uterineLoad = tag.getFloatOr("uterus", 46.0f);
        st.rectalLoad = tag.getFloatOr("rectal", 14.0f);
        st.drugType = tag.getIntOr("drug_type", DRUG_NONE);
        st.drugTicks = tag.getIntOr("drug_ticks", 0);

        for (var entry : tag.getListOrEmpty("boluses")) {
            if (!(entry instanceof CompoundTag bt)) continue;
            st.addBolus(
                    bt.getDoubleOr("rx", -34.0),
                    bt.getIntOr("rz", 0),
                    bt.getIntOr("flavor", 0),
                    bt.getFloatOr("nutrients", 80.0f)
            );
        }
        for (var entry : tag.getListOrEmpty("breaches")) {
            if (!(entry instanceof CompoundTag bt)) continue;
            String blockId = bt.getStringOr("block", "justarod:ascaris_mucosa");
            if (st.breaches.size() < MAX_BREACHES) {
                st.breaches.add(new TissueBreach(
                        bt.getIntOr("rx", 0),
                        bt.getIntOr("y", BodyWorld.FLOOR_Y + 1),
                        bt.getIntOr("rz", 0),
                        bt.getIntOr("organ", ORGAN_CUTICLE),
                        blockId,
                        bt.getIntOr("repair", 0)
                ));
            }
        }
        st.clampAll();
        return st;
    }
}
