package org.cneko.justarod.genetics

import net.minecraft.nbt.NbtCompound
import net.minecraft.util.Identifier
import org.cneko.justarod.Justarod.MODID
import org.cneko.justarod.entity.RodEntity
import org.cneko.toneko.common.mod.genetics.ToNekoLocus
import org.cneko.toneko.common.mod.genetics.api.*
import java.util.function.BiConsumer

/*
 * 长度、宽度、高潮强度...
 * 嗯？我在写什么怪东西？不管了，反正有人需要是吧！
 * 6号染色体管长短，7号染色体管粗细和爽感，每个基因都给一点加成，叠起来就厉害了
 */
object RodGenetics {
    // ========== 基因座 (6号染色体 - 长度) ==========
    val LENGTH_SLOT_0 = Locus(Identifier.of(MODID, "length_slot_0"))
    val LENGTH_SLOT_1 = Locus(Identifier.of(MODID, "length_slot_1"))
    val LENGTH_SLOT_2 = Locus(Identifier.of(MODID, "length_slot_2"))

    // ========== 基因座 (7号染色体 - 宽度 & 高潮强度) ==========
    val WIDTH_SLOT_0 = Locus(Identifier.of(MODID, "width_slot_0"))
    val WIDTH_SLOT_1 = Locus(Identifier.of(MODID, "width_slot_1"))
    val ORGASM_SLOT_0 = Locus(Identifier.of(MODID, "orgasm_slot_0"))
    val ORGASM_SLOT_1 = Locus(Identifier.of(MODID, "orgasm_slot_1"))

    // ========== 等位基因 - 长度 (每人贡献一部分，多个可叠加) ==========

    // LENGTH_SLOT_0: +0.0 / +0.2 / +0.4
    val LENGTH_0_NORMAL = Allele(Identifier.of(MODID, "length_0_normal"), 10, null, null)
    val LENGTH_0_LONGER = Allele(Identifier.of(MODID, "length_0_longer"), 15,
        { _, data -> data.putDouble("length_0", 0.2) },
        { _, data -> data.remove("length_0") })
    val LENGTH_0_LONG = Allele(Identifier.of(MODID, "length_0_long"), 5,
        { _, data -> data.putDouble("length_0", 0.4) },
        { _, data -> data.remove("length_0") })

    // LENGTH_SLOT_1: +0.0 / +0.15 / +0.3
    val LENGTH_1_NORMAL = Allele(Identifier.of(MODID, "length_1_normal"), 10, null, null)
    val LENGTH_1_LONGER = Allele(Identifier.of(MODID, "length_1_longer"), 15,
        { _, data -> data.putDouble("length_1", 0.15) },
        { _, data -> data.remove("length_1") })
    val LENGTH_1_LONG = Allele(Identifier.of(MODID, "length_1_long"), 5,
        { _, data -> data.putDouble("length_1", 0.3) },
        { _, data -> data.remove("length_1") })

    // LENGTH_SLOT_2: +0.0 / +0.1 / +0.25
    val LENGTH_2_NORMAL = Allele(Identifier.of(MODID, "length_2_normal"), 10, null, null)
    val LENGTH_2_LONGER = Allele(Identifier.of(MODID, "length_2_longer"), 15,
        { _, data -> data.putDouble("length_2", 0.1) },
        { _, data -> data.remove("length_2") })
    val LENGTH_2_LONG = Allele(Identifier.of(MODID, "length_2_long"), 5,
        { _, data -> data.putDouble("length_2", 0.25) },
        { _, data -> data.remove("length_2") })

    // ========== 等位基因 - 宽度 (每人贡献一部分，多个可叠加) ==========

    // WIDTH_SLOT_0: +0.0 / +0.2 / +0.35
    val WIDTH_0_NORMAL = Allele(Identifier.of(MODID, "width_0_normal"), 10, null, null)
    val WIDTH_0_WIDER = Allele(Identifier.of(MODID, "width_0_wider"), 15,
        { _, data -> data.putDouble("width_0", 0.2) },
        { _, data -> data.remove("width_0") })
    val WIDTH_0_THICK = Allele(Identifier.of(MODID, "width_0_thick"), 5,
        { _, data -> data.putDouble("width_0", 0.35) },
        { _, data -> data.remove("width_0") })

    // WIDTH_SLOT_1: +0.0 / +0.15 / +0.25
    val WIDTH_1_NORMAL = Allele(Identifier.of(MODID, "width_1_normal"), 10, null, null)
    val WIDTH_1_WIDER = Allele(Identifier.of(MODID, "width_1_wider"), 15,
        { _, data -> data.putDouble("width_1", 0.15) },
        { _, data -> data.remove("width_1") })
    val WIDTH_1_THICK = Allele(Identifier.of(MODID, "width_1_thick"), 5,
        { _, data -> data.putDouble("width_1", 0.25) },
        { _, data -> data.remove("width_1") })

    // ========== 等位基因 - 高潮强度 (每人贡献一部分，多个可叠加) ==========

    // ORGASM_SLOT_0: 基础 +0.0 / +0.5 / +1.0
    val ORGASM_0_NORMAL = Allele(Identifier.of(MODID, "orgasm_0_normal"), 10, null, null)
    val ORGASM_0_STRONG = Allele(Identifier.of(MODID, "orgasm_0_strong"), 15,
        { _, data -> data.putDouble("orgasm_0", 0.5) },
        { _, data -> data.remove("orgasm_0") })
    val ORGASM_0_INTENSE = Allele(Identifier.of(MODID, "orgasm_0_intense"), 5,
        { _, data -> data.putDouble("orgasm_0", 1.0) },
        { _, data -> data.remove("orgasm_0") })

    // ORGASM_SLOT_1: 基础 +0.0 / +0.3 / +0.6
    val ORGASM_1_NORMAL = Allele(Identifier.of(MODID, "orgasm_1_normal"), 10, null, null)
    val ORGASM_1_SENSITIVE = Allele(Identifier.of(MODID, "orgasm_1_sensitive"), 15,
        { _, data -> data.putDouble("orgasm_1", 0.3) },
        { _, data -> data.remove("orgasm_1") })
    val ORGASM_1_HYPER = Allele(Identifier.of(MODID, "orgasm_1_hyper"), 5,
        { _, data -> data.putDouble("orgasm_1", 0.6) },
        { _, data -> data.remove("orgasm_1") })

    // ========== 核型 ==========
    // 一共7对染色体，6号上挂3个长度基因，7号上挂2个宽度+2个高潮强度基因
    val KARYOTYPE = SpeciesKaryotype(ToNekoLocus.BASE_MOB_KARYOTYPE,2)
        .bindLocus(6, LENGTH_SLOT_0)
        .bindLocus(6, LENGTH_SLOT_1)
        .bindLocus(6, LENGTH_SLOT_2)
        .bindLocus(7, WIDTH_SLOT_0)
        .bindLocus(7, WIDTH_SLOT_1)
        .bindLocus(7, ORGASM_SLOT_0)
        .bindLocus(7, ORGASM_SLOT_1)

    fun init() {
        // 注册基因座
        GeneticsRegistry.registerLocus(LENGTH_SLOT_0)
        GeneticsRegistry.registerLocus(LENGTH_SLOT_1)
        GeneticsRegistry.registerLocus(LENGTH_SLOT_2)
        GeneticsRegistry.registerLocus(WIDTH_SLOT_0)
        GeneticsRegistry.registerLocus(WIDTH_SLOT_1)
        GeneticsRegistry.registerLocus(ORGASM_SLOT_0)
        GeneticsRegistry.registerLocus(ORGASM_SLOT_1)

        // 注册等位基因 - 长度
        GeneticsRegistry.registerAllele(LENGTH_0_NORMAL)
        GeneticsRegistry.registerAllele(LENGTH_0_LONGER)
        GeneticsRegistry.registerAllele(LENGTH_0_LONG)
        GeneticsRegistry.registerAllele(LENGTH_1_NORMAL)
        GeneticsRegistry.registerAllele(LENGTH_1_LONGER)
        GeneticsRegistry.registerAllele(LENGTH_1_LONG)
        GeneticsRegistry.registerAllele(LENGTH_2_NORMAL)
        GeneticsRegistry.registerAllele(LENGTH_2_LONGER)
        GeneticsRegistry.registerAllele(LENGTH_2_LONG)

        // 注册等位基因 - 宽度
        GeneticsRegistry.registerAllele(WIDTH_0_NORMAL)
        GeneticsRegistry.registerAllele(WIDTH_0_WIDER)
        GeneticsRegistry.registerAllele(WIDTH_0_THICK)
        GeneticsRegistry.registerAllele(WIDTH_1_NORMAL)
        GeneticsRegistry.registerAllele(WIDTH_1_WIDER)
        GeneticsRegistry.registerAllele(WIDTH_1_THICK)

        // 注册等位基因 - 高潮强度
        GeneticsRegistry.registerAllele(ORGASM_0_NORMAL)
        GeneticsRegistry.registerAllele(ORGASM_0_STRONG)
        GeneticsRegistry.registerAllele(ORGASM_0_INTENSE)
        GeneticsRegistry.registerAllele(ORGASM_1_NORMAL)
        GeneticsRegistry.registerAllele(ORGASM_1_SENSITIVE)
        GeneticsRegistry.registerAllele(ORGASM_1_HYPER)

        // 注册核型
        GeneticsRegistry.registerKaryotype(RodEntity::class.java, KARYOTYPE)

        // 野生基因池 - 长度槽位 (权值: 普通60% / 较长25% / 长15%)
        addWild(LENGTH_SLOT_0.id(), LENGTH_0_NORMAL.id, 60)
        addWild(LENGTH_SLOT_0.id(), LENGTH_0_LONGER.id, 25)
        addWild(LENGTH_SLOT_0.id(), LENGTH_0_LONG.id, 15)

        addWild(LENGTH_SLOT_1.id(), LENGTH_1_NORMAL.id, 60)
        addWild(LENGTH_SLOT_1.id(), LENGTH_1_LONGER.id, 25)
        addWild(LENGTH_SLOT_1.id(), LENGTH_1_LONG.id, 15)

        addWild(LENGTH_SLOT_2.id(), LENGTH_2_NORMAL.id, 60)
        addWild(LENGTH_SLOT_2.id(), LENGTH_2_LONGER.id, 25)
        addWild(LENGTH_SLOT_2.id(), LENGTH_2_LONG.id, 15)

        // 野生基因池 - 宽度槽位
        addWild(WIDTH_SLOT_0.id(), WIDTH_0_NORMAL.id, 60)
        addWild(WIDTH_SLOT_0.id(), WIDTH_0_WIDER.id, 25)
        addWild(WIDTH_SLOT_0.id(), WIDTH_0_THICK.id, 15)

        addWild(WIDTH_SLOT_1.id(), WIDTH_1_NORMAL.id, 60)
        addWild(WIDTH_SLOT_1.id(), WIDTH_1_WIDER.id, 25)
        addWild(WIDTH_SLOT_1.id(), WIDTH_1_THICK.id, 15)

        // 野生基因池 - 高潮强度槽位
        addWild(ORGASM_SLOT_0.id(), ORGASM_0_NORMAL.id, 60)
        addWild(ORGASM_SLOT_0.id(), ORGASM_0_STRONG.id, 25)
        addWild(ORGASM_SLOT_0.id(), ORGASM_0_INTENSE.id, 15)

        addWild(ORGASM_SLOT_1.id(), ORGASM_1_NORMAL.id, 60)
        addWild(ORGASM_SLOT_1.id(), ORGASM_1_SENSITIVE.id, 25)
        addWild(ORGASM_SLOT_1.id(), ORGASM_1_HYPER.id, 15)
    }

    // ========== 工具方法：从遗传数据计算合成加成 ==========

    /** 计算总长度加成 */
    fun getTotalLengthBonus(data: NbtCompound): Double {
        return data.getDouble("length_0") +
               data.getDouble("length_1") +
               data.getDouble("length_2")
    }

    /** 计算总宽度加成 */
    fun getTotalWidthBonus(data: NbtCompound): Double {
        return data.getDouble("width_0") +
               data.getDouble("width_1")
    }

    /** 计算高潮强度倍率 (基础 1.0 + 各基因贡献) */
    fun getOrgasmMultiplier(data: NbtCompound): Double {
        return 1.0 + data.getDouble("orgasm_0") +
               data.getDouble("orgasm_1")
    }

    // ========== 私有辅助 ==========

    private fun addWild(locusId: Identifier, alleleId: Identifier, weight: Int) {
        GeneticsRegistry.addWildAllele(locusId, alleleId, weight)
    }
}