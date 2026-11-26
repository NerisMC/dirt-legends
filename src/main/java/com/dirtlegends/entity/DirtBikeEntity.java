package com.dirtlegends.entity;

import com.dirtlegends.registry.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class DirtBikeEntity extends MotoVehicleEntity {
    public DirtBikeEntity(EntityType<? extends MotoVehicleEntity> type, World world) {
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
