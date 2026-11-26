package com.dirtlegends.entity;

import com.dirtlegends.registry.ModItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

public final class MotocrossGear {
    private MotocrossGear() {
    }

    /** Full helmet + jersey + pants + boots cuts crash and landing damage by 70%. */
    public static boolean isWearingFullSet(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.MX_HELMET)
                && entity.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.MX_JERSEY)
                && entity.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.MX_PANTS)
                && entity.getItemBySlot(EquipmentSlot.FEET).is(ModItems.MX_BOOTS);
    }
}
