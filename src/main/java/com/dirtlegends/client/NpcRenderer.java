package com.dirtlegends.client;

import com.dirtlegends.DirtLegends;
import java.util.function.ToIntFunction;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

/** Player-shaped renderer for the track NPCs (fans, racers, vendors), picking one of several skins. */
public class NpcRenderer<T extends Mob> extends HumanoidMobRenderer<T, NpcRenderState, HumanoidModel<NpcRenderState>> {
    private final Identifier[] skins;
    private final ToIntFunction<T> skinOf;

    public NpcRenderer(EntityRendererProvider.Context ctx, String folder, String prefix, int count, ToIntFunction<T> skinOf) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5f);
        this.skinOf = skinOf;
        this.skins = new Identifier[count];
        for (int i = 0; i < count; i++) skins[i] = DirtLegends.id("textures/entity/" + folder + "/" + prefix + "_" + i + ".png");
    }

    @Override
    public NpcRenderState createRenderState() {
        return new NpcRenderState();
    }

    @Override
    public void extractRenderState(T entity, NpcRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.skin = skinOf.applyAsInt(entity);
    }

    @Override
    public Identifier getTextureLocation(NpcRenderState state) {
        return skins[Math.floorMod(state.skin, skins.length)];
    }
}
