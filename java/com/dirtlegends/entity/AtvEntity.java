package com.dirtlegends.entity;

import com.dirtlegends.registry.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class AtvEntity extends MotoVehicleEntity {
    public AtvEntity(EntityType<? extends MotoVehicleEntity> type, World world) {
        super(type, world);
    }

    @Override
    public VehicleStats getStats() {
        return VehicleStats.ATV;
    }

    @Override
    protected ItemStack getVariantStack(int variant) {
        return new ItemStack(ModItems.ATVS[variant]);
    }
}
