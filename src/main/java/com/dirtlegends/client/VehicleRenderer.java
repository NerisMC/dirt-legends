package com.dirtlegends.client;

import com.dirtlegends.DirtLegends;
import com.dirtlegends.entity.MotoVehicleEntity;
import com.dirtlegends.registry.ModItems;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class VehicleRenderer extends MobRenderer<MotoVehicleEntity, VehicleRenderState, EntityModel<VehicleRenderState>> {
    private final Identifier[] textures;

    public VehicleRenderer(EntityRendererProvider.Context context, EntityModel<VehicleRenderState> model, float shadowRadius, String name) {
        super(context, model, shadowRadius);
        textures = new Identifier[ModItems.COLORS.length];
        for (int i = 0; i < textures.length; i++) {
            textures[i] = DirtLegends.id("textures/entity/" + name + "_" + ModItems.COLORS[i] + ".png");
        }
    }

    @Override
    public VehicleRenderState createRenderState() {
        return new VehicleRenderState();
    }

    @Override
    public void extractRenderState(MotoVehicleEntity entity, VehicleRenderState state, float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        state.steer = entity.getSteerAngle(tickDelta);
        state.suspension = entity.getSuspension(tickDelta);
        state.bodyPitch = entity.getBodyPitch(tickDelta);
        state.lean = entity.getLean(tickDelta);
        state.wheelSpin = entity.getWheelSpin(tickDelta);
        state.variant = entity.getVariant();
    }

    @Override
    public Identifier getTextureLocation(VehicleRenderState state) {
        int v = state.variant;
        return textures[v >= 0 && v < textures.length ? v : 0];
    }
}
