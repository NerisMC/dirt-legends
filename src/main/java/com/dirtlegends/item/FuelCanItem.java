package com.dirtlegends.item;

import net.minecraft.world.item.Item;

/** Jerry can. Right-click a vehicle to add half a tank (handled in MotoVehicleEntity.interactMob). */
public class FuelCanItem extends Item {
    public static final float FUEL_AMOUNT = 50.0f;

    public FuelCanItem(Properties settings) {
        super(settings);
    }
}
