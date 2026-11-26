package com.dirtlegends.client;

import com.dirtlegends.DirtLegends;
import com.dirtlegends.entity.MotoVehicleEntity;
import com.dirtlegends.registry.ModItems;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class VehicleRenderer<M extends EntityModel<MotoVehicleEntity>> extends MobRenderer<MotoVehicleEntity, M> {
    private final ResourceLocation[] textures;

    public VehicleRenderer(EntityRendererProvider.Context context, M model, float shadowRadius, String name) {
        super(context, model, shadowRadius);
        textures = new ResourceLocation[ModItems.COLORS.length];
        for (int i = 0; i < textures.length; i++) {
            textures[i] = DirtLegends.id("textures/entity/" + name + "_" + ModItems.COLORS[i] + ".png");
        }
    }

    @Override
    public ResourceLocation getTexture(MotoVehicleEntity entity) {
        return textures[entity.getVariant()];
    }
}
