package com.dirtlegends.client;

import com.dirtlegends.DirtLegends;
import com.dirtlegends.entity.MotoVehicleEntity;
import com.dirtlegends.registry.ModItems;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.util.Identifier;

public class VehicleRenderer<M extends EntityModel<MotoVehicleEntity>> extends MobEntityRenderer<MotoVehicleEntity, M> {
    private final Identifier[] textures;

    public VehicleRenderer(EntityRendererFactory.Context context, M model, float shadowRadius, String name) {
        super(context, model, shadowRadius);
        textures = new Identifier[ModItems.COLORS.length];
        for (int i = 0; i < textures.length; i++) {
            textures[i] = DirtLegends.id("textures/entity/" + name + "_" + ModItems.COLORS[i] + ".png");
        }
    }

    @Override
    public Identifier getTexture(MotoVehicleEntity entity) {
        return textures[entity.getVariant()];
    }
}
