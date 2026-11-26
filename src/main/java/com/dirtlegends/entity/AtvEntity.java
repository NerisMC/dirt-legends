package com.dirtlegends.entity;

import com.dirtlegends.registry.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class AtvEntity extends MotoVehicleEntity {
    public AtvEntity(EntityType<? extends MotoVehicleEntity> type, Level world) {
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
