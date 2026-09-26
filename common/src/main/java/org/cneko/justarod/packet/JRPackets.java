package org.cneko.justarod.packet;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class JRPackets {
    public static void init(){
        PayloadTypeRegistry.clientboundPlay().register(FrictionPayload.ID, FrictionPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(FullHeatPayload.ID, FullHeatPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(JRSyncPayload.ID, JRSyncPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(MatePayload.ID, MatePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(PassiveMatingPayload.ID, PassiveMatingPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RavennPassiveMatingPayload.ID,RavennPassiveMatingPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(BDSMPayload.ID, BDSMPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(MedicalPayload.ID, MedicalPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(XRayScanScreenPayload.ID, XRayScanScreenPayload.CODEC);
        // ===== 驯服史莱姆 =====
        PayloadTypeRegistry.serverboundPlay().register(SlimeContainerActionPayload.ID, SlimeContainerActionPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SlimeEntityListPayload.ID, SlimeEntityListPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SlimeMenuContextPayload.ID, SlimeMenuContextPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SlimeContentsPayload.ID, SlimeContentsPayload.CODEC);
        // 手动「吞噬」技能状态（冷却 / 附近可吞目标数 / 放大动画进度）
        PayloadTypeRegistry.clientboundPlay().register(SlimeDevourStatePayload.ID, SlimeDevourStatePayload.CODEC);
    }
}
