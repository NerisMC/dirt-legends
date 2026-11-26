package com.dirtlegends.entity;

import com.dirtlegends.registry.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class DirtBikeEntity extends MotoVehicleEntity {
    public DirtBikeEntity(EntityType<? extends MotoVehicleEntity> type, Level world) {
        super(type, world);
    }

    @Override
    public VehicleStats getStats() {
        return VehicleStats.DIRT_BIKE;
    }

    @Override
    protected ItemStack getVariantStack(int variant) {
        return new ItemStack(ModItems.DIRT_BIKES[variant]);
    }
}
