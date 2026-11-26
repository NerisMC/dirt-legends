package com.dirtlegends.registry;

import com.dirtlegends.DirtLegends;
import com.dirtlegends.entity.AtvEntity;
import com.dirtlegends.entity.DirtBikeEntity;
import com.dirtlegends.entity.MotoVehicleEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModEntities {
    public static final EntityType<DirtBikeEntity> DIRT_BIKE = Registry.register(Registries.ENTITY_TYPE, DirtLegends.id("dirt_bike"),
            EntityType.Builder.create(DirtBikeEntity::new, SpawnGroup.MISC)
                    .dimensions(1.0f, 1.1f)
                    .passengerAttachments(0.95f)
                    .maxTrackingRange(10)
                    .build("dirt_bike"));

    public static final EntityType<AtvEntity> ATV = Registry.register(Registries.ENTITY_TYPE, DirtLegends.id("atv"),
            EntityType.Builder.create(AtvEntity::new, SpawnGroup.MISC)
                    .dimensions(1.3f, 1.0f)
                    .passengerAttachments(0.85f)
                    .maxTrackingRange(10)
                    .build("atv"));

    private ModEntities() {
    }

    public static void init() {
        FabricDefaultAttributeRegistry.register(DIRT_BIKE, MotoVehicleEntity.createVehicleAttributes());
        FabricDefaultAttributeRegistry.register(ATV, MotoVehicleEntity.createVehicleAttributes());
    }
}
