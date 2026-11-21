package org.cneko.justarod.client.pet;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import org.cneko.justarod.entity.PetOwnerNekoEntity;
import org.cneko.toneko.common.mod.client.renderers.NekoRenderer;
import org.cneko.toneko.common.mod.client.renderers.TonekoLivingEntityGeoState;

/** Reuses the model and layers while keeping GeckoLib reads/writes on the same render-state map. */
public final class PetOwnerNekoRenderer extends NekoRenderer<PetOwnerNekoEntity> {
    public static final class State extends TonekoLivingEntityGeoState {
        boolean holding;
        float pat;
        @Override public <D> void addGeckolibData(DataTicket<D> ticket, D value) { getDataMap().put(ticket, value); }
        @Override public boolean hasGeckolibData(DataTicket<?> ticket) { return getDataMap().containsKey(ticket); }
    }
    public PetOwnerNekoRenderer(EntityRendererProvider.Context context) { super(context, new Model()); }
    private static final class Model extends GeoModel<PetOwnerNekoEntity> {
        @Override public Identifier getModelResource(GeoRenderState state) {
            return Identifier.fromNamespaceAndPath("justarod", "entity/pet_owner_neko");
        }
        @Override public Identifier getAnimationResource(PetOwnerNekoEntity npc) {
            return Identifier.fromNamespaceAndPath("justarod", "entity/pet_owner_neko");
        }
        @Override public Identifier getTextureResource(GeoRenderState state) {
            return Identifier.fromNamespaceAndPath("toneko", "textures/neko/shiuri_neko.png");
        }
    }
    @Override public TonekoLivingEntityGeoState createRenderState(PetOwnerNekoEntity npc, Void unused) { return new State(); }
    @Override public void captureDefaultRenderState(PetOwnerNekoEntity npc, Void unused, TonekoLivingEntityGeoState state, float partialTick) {
        super.captureDefaultRenderState(npc, unused, state, partialTick);
        State s = (State) state;
        s.holding = !npc.getMainHandItem().isEmpty();
        s.pat = npc.getAttackAnim(partialTick);
    }
    @Override public void applyAnimationControllers(RenderPassInfo<TonekoLivingEntityGeoState> info, BoneSnapshots snapshots) {
        super.applyAnimationControllers(info, snapshots);
        State s = (State) info.renderState();
        if (s.holding || s.pat > 0) snapshots.ifPresent("RightArm", arm ->
                arm.setRotX(-0.85f - (float) Math.sin(s.pat * Math.PI) * 0.4f));
    }
}
