package com.dirtlegends.registry;

import com.dirtlegends.DirtLegends;
import com.dirtlegends.entity.AtvEntity;
import com.dirtlegends.entity.DirtBikeEntity;
import com.dirtlegends.entity.MotoVehicleEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
    public static final EntityType<DirtBikeEntity> DIRT_BIKE = Registry.register(BuiltInRegistries.ENTITY_TYPE, DirtLegends.id("dirt_bike"),
            EntityType.Builder.of(DirtBikeEntity::new, MobCategory.MISC)
                    .sized(1.0f, 1.1f)
                    .passengerAttachments(0.95f)
                    .clientTrackingRange(10)
                    .build(key("dirt_bike")));

    public static final EntityType<AtvEntity> ATV = Registry.register(BuiltInRegistries.ENTITY_TYPE, DirtLegends.id("atv"),
            EntityType.Builder.of(AtvEntity::new, MobCategory.MISC)
                    .sized(1.3f, 1.0f)
                    .passengerAttachments(0.85f)
                    .clientTrackingRange(10)
                    .build(key("atv")));

    private static net.minecraft.resources.ResourceKey<EntityType<?>> key(String name) {
        return net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, DirtLegends.id(name));
    }

    private ModEntities() {
    }

    public static void init() {
        FabricDefaultAttributeRegistry.register(DIRT_BIKE, MotoVehicleEntity.createVehicleAttributes());
        FabricDefaultAttributeRegistry.register(ATV, MotoVehicleEntity.createVehicleAttributes());
    }
}
