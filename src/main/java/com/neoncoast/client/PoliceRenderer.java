package com.neoncoast.client;

import com.neoncoast.NeonCoast;
import com.neoncoast.npc.PoliceOfficerEntity;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.util.Identifier;

public class PoliceRenderer extends BipedEntityRenderer<PoliceOfficerEntity, BipedEntityModel<PoliceOfficerEntity>> {
    private static final Identifier TEXTURE = NeonCoast.id("textures/entity/police_officer.png");

    public PoliceRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new BipedEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public Identifier getTexture(PoliceOfficerEntity entity) {
        return TEXTURE;
    }
}
