package org.cneko.justarod.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import org.cneko.justarod.Justarod;
import org.cneko.justarod.entity.Pregnant;

public class UterusScanScreen extends MedicalScanScreen {

    // =========================================================================
    // 【贴图清单】256x256 尺寸，在对应位置画好透明 PNG
    // =========================================================================

    // 1. 基础器官
    private static final Identifier TEX_UTERUS_BASE = id("uterus_base");           // 健康的子宫、阴道和输卵管底图
    private static final Identifier TEX_OVARY_NORMAL = id("ovary_normal");         // 正常卵巢
    //private static final Identifier TEX_OVARY_PCOS = id("ovary_pcos");             // 多囊卵巢(肿大，布满黑色未成熟小囊泡)

    // 2. 月经周期相关
    private static final Identifier TEX_FOLLICLE = id("cycle_follicle");           // 卵泡期：卵巢上的小亮点
    private static final Identifier TEX_OVUM = id("cycle_ovum");                   // 排卵期：悬浮在输卵管伞端的发光卵子
    private static final Identifier TEX_CORPUS_LUTEUM = id("cycle_luteum");        // 黄体期：卵巢上的黄色斑块，以及增厚的子宫内膜
    private static final Identifier TEX_MENSTRUAL_BLOOD = id("cycle_menstruation");// 月经期：宫腔和阴道内斑驳的暗红色液滴/血流

    // 3. 孕产相关
    //private static final Identifier TEX_FETUS = id("preg_fetus");                  // 正常怀孕：宫腔内的胎儿剪影 (可根据孕期控制透明度/大小)
    //private static final Identifier TEX_MOLE = id("preg_mole");                    // 葡萄胎：填满宫腔的半透明葡萄状水泡
    //private static final Identifier TEX_ECTOPIC = id("preg_ectopic");              // 宫外孕：卡在输卵管的胎囊，输卵管肿胀

    // 4. 疾病与急症
    //private static final Identifier TEX_CANCER_OVARIAN = id("disease_cancer");     // 卵巢癌：卵巢上的不规则暗色恶性肿块
    //private static final Identifier TEX_UTERINE_COLD = id("disease_cold");         // 宫寒：覆盖在整个子宫表面的冰霜/雪花滤镜

    // 黄体破裂 (内出血)
    //private static final Identifier TEX_HEMORRHAGE_LIGHT = id("rupture_light");    // 盆腔底部的小片暗红积血
    //private static final Identifier TEX_HEMORRHAGE_SEVERE = id("rupture_severe");  // 填满盆腔的巨大血泊

    // 处女膜与闭锁
    //private static final Identifier TEX_HYMEN_NORMAL = id("hymen_normal");         // 正常的处女膜 (阴道口的一道半透明膜)
    //private static final Identifier TEX_HYMEN_TORN = id("hymen_torn");             // 破裂的处女膜 (锯齿状边缘)
    //private static final Identifier TEX_HYMEN_IMPERF = id("hymen_imperforate");    // 闭锁的处女膜 (粗重、完全封死的膜)
    //private static final Identifier TEX_HEMATOCOLPOS = id("disease_hematocolpos"); // 经血潴留：闭锁导致的巨大暗红血囊，撑满阴道和子宫

    // =========================================================================

    private static Identifier id(String name) {
        return Identifier.of(Justarod.MODID, "textures/gui/medical/" + name + ".png");
    }

    public UterusScanScreen(LivingEntity targetEntity) {
        super(targetEntity);
    }

    @Override
    protected void renderOrganLayers(DrawContext context, int x, int y, float delta) {
        // 如果没有子宫，屏幕中心画个空的或者保持扫描仪黑屏即可
        if (!pregnantData.hasUterus() || !pregnantData.isFemale()) {
            return;
        }

        // 获取时间，用于做闪烁或脉冲动画
        long time = targetEntity.getWorld().getTime();

        // -----------------------------------------------------------------
        // 第 1 层：卵巢与输卵管附件 (最底层)
        // -----------------------------------------------------------------
        if (pregnantData.isPCOS()) {
            //drawLayer(context, TEX_OVARY_PCOS, x, y);
        } else {
            drawLayer(context, TEX_OVARY_NORMAL, x, y);
        }

        // 卵巢癌肿瘤图层覆盖在卵巢上
        if (pregnantData.getOvarianCancer() > 0) {
            //drawLayer(context, TEX_CANCER_OVARIAN, x, y);
        }

        // -----------------------------------------------------------------
        // 第 2 层：生理周期细节 (附着在卵巢和内膜上)
        // -----------------------------------------------------------------
        Pregnant.MenstruationCycle cycle = pregnantData.getMenstruationCycle();
        if (!pregnantData.isPregnant() && !pregnantData.isPCOS()) {
            switch (cycle) {
                case FOLLICLE -> drawLayer(context, TEX_FOLLICLE, x, y);
                //case OVULATION -> drawLayer(context, TEX_OVUM, x, y);
                //case LUTEINIZATION -> drawLayer(context, TEX_CORPUS_LUTEUM, x, y);
                // 月经的血流因为有液体流动感，放在稍微靠前的位置画
            }
        }

        // -----------------------------------------------------------------
        // 第 3 层：子宫与阴道主体结构
        // -----------------------------------------------------------------
        drawLayer(context, TEX_UTERUS_BASE, x, y);

        // -----------------------------------------------------------------
        // 第 4 层：宫腔内容物 (怀孕状态)
        // -----------------------------------------------------------------
        if (pregnantData.isPregnant()) {
            if (pregnantData.isHydatidiformMole()) {
                // 葡萄胎
                //drawLayer(context, TEX_MOLE, x, y);
            } else if (pregnantData.isEctopicPregnancy()) {
                // 宫外孕 (让它微微闪烁红光以示警告)
                if (time % 20 < 10) {
                    RenderSystem.setShaderColor(1.0F, 0.5F, 0.5F, 1.0F);
                }
                //drawLayer(context, TEX_ECTOPIC, x, y);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F); // 恢复正常颜色
            } else {
                // 正常怀孕胎儿
                //drawLayer(context, TEX_FETUS, x, y);
            }
        }

        // -----------------------------------------------------------------
        // 第 5 层：液体/血液层 (月经、闭锁积血、黄体破裂内出血)
        // -----------------------------------------------------------------

        // A. 处女膜闭锁引发的经血潴留 (最极端的情况：大血袋撑满)
        if (pregnantData.isImperforateHymen() && cycle == Pregnant.MenstruationCycle.MENSTRUATION) {
            //drawLayer(context, TEX_HEMATOCOLPOS, x, y);
        }
        // B. 正常的月经流血
        else if (cycle == Pregnant.MenstruationCycle.MENSTRUATION && !pregnantData.isPregnant()) {
            //drawLayer(context, TEX_MENSTRUAL_BLOOD, x, y);
        }

        // C. 黄体破裂导致盆腔内出血 (在子宫外部底部积攒)
        int ruptureTime = pregnantData.getCorpusLuteumRupture();
        if (ruptureTime > 0) {
            if (pregnantData.isSevereCorpusLuteumRupture()) {
                // 重症大出血，血海闪烁
                if (time % 10 < 5) RenderSystem.setShaderColor(1.0F, 0.8F, 0.8F, 1.0F);
                //drawLayer(context, TEX_HEMORRHAGE_SEVERE, x, y);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            } else {
                // 轻症小量积血
                //drawLayer(context, TEX_HEMORRHAGE_LIGHT, x, y);
            }
        }

        // -----------------------------------------------------------------
        // 第 6 层：处女膜结构 (最外侧挡住阴道口)
        // -----------------------------------------------------------------
        if (pregnantData.isImperforateHymen()) {
            //drawLayer(context, TEX_HYMEN_IMPERF, x, y);
        } else if (pregnantData.hasHymen()) {
            //drawLayer(context, TEX_HYMEN_NORMAL, x, y);
        } else {
            //drawLayer(context, TEX_HYMEN_TORN, x, y); // 破裂后的边缘残余
        }

        // -----------------------------------------------------------------
        // 第 7 层：环境/全局滤镜 (宫寒冰霜覆盖)
        // -----------------------------------------------------------------
        if (pregnantData.isUterineCold()) {
            // 根据宫寒严重程度调整冰霜的透明度 (Alpha)
            int coldValue = pregnantData.getUterineCold();
            float alpha = Math.min(1.0F, coldValue / (20.0F * 60 * 20 * 5)); // 最高5天封顶
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);

            //drawLayer(context, TEX_UTERINE_COLD, x, y);

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
        }
    }
}