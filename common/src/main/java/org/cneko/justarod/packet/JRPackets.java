package org.cneko.justarod.packet;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class JRPackets {
    public static void init(){
        PayloadTypeRegistry.clientboundPlay().register(DisassemblyViewPayload.ID, DisassemblyViewPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DisassemblyActionPayload.ID, DisassemblyActionPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DisassemblyStatePayload.ID, DisassemblyStatePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(PetActionPayload.ID, PetActionPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(PetMenuPayload.ID, PetMenuPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(PetStatePayload.ID, PetStatePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(EmpathyReactionPayload.ID, EmpathyReactionPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(EndRodGunShotPayload.ID, EndRodGunShotPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(FrictionPayload.ID, FrictionPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(FullHeatPayload.ID, FullHeatPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(JRSyncPayload.ID, JRSyncPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(MatePayload.ID, MatePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(PassiveMatingPayload.ID, PassiveMatingPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RavennPassiveMatingPayload.ID,RavennPassiveMatingPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(BDSMPayload.ID, BDSMPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(MedicalPayload.ID, MedicalPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(XRayScanScreenPayload.ID, XRayScanScreenPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(InjectableFluidPayload.ID, InjectableFluidPayload.CODEC);
        // ===== 驯服史莱姆 =====
        PayloadTypeRegistry.serverboundPlay().register(SlimeContainerActionPayload.ID, SlimeContainerActionPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(BodyTravelPayload.ID, BodyTravelPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SlimeEntityListPayload.ID, SlimeEntityListPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SlimeMenuContextPayload.ID, SlimeMenuContextPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SlimeContentsPayload.ID, SlimeContentsPayload.CODEC);
        // 手动「吞噬」技能状态（冷却 / 附近可吞目标数 / 放大动画进度）
        PayloadTypeRegistry.clientboundPlay().register(SlimeDevourStatePayload.ID, SlimeDevourStatePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SlimeCaptivityStatePayload.ID, SlimeCaptivityStatePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SlimeFloatingPositionsPayload.ID, SlimeFloatingPositionsPayload.CODEC);
        // ===== 蛔虫感染 & 粉白蛔虫娘 =====
        PayloadTypeRegistry.clientboundPlay().register(AscarisVisualEventPayload.ID, AscarisVisualEventPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(AscarisActionPayload.ID, AscarisActionPayload.CODEC);
    }
}
