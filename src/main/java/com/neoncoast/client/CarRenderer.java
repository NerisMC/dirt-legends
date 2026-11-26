package com.neoncoast.client;

import com.neoncoast.NeonCoast;
import com.neoncoast.vehicle.CarEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.util.Identifier;

public class CarRenderer<M extends EntityModel<CarEntity>> extends MobEntityRenderer<CarEntity, M> {
    private final Identifier[] textures;
    private final Identifier flash;

    public CarRenderer(EntityRendererFactory.Context ctx, M model, String name, boolean police) {
        super(ctx, model, 1.2f);
        if (police) {
            textures = new Identifier[]{NeonCoast.id("textures/entity/" + name + ".png")};
            flash = NeonCoast.id("textures/entity/" + name + "_flash.png");
        } else {
            textures = new Identifier[CarEntity.COLORS.length];
            for (int i = 0; i < textures.length; i++) {
                textures[i] = NeonCoast.id("textures/entity/" + name + "_" + CarEntity.COLORS[i] + ".png");
            }
            flash = null;
        }
    }

    @Override
    public Identifier getTexture(CarEntity car) {
        if (flash != null) {
            return car.isSirenOn() && (car.age / 4) % 2 == 0 ? flash : textures[0];
        }
        return textures[Math.min(car.getVariant(), textures.length - 1)];
    }
}
