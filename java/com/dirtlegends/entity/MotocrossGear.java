package com.dirtlegends.entity;

import com.dirtlegends.registry.ModItems;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;

public final class MotocrossGear {
    private MotocrossGear() {
    }

    /** Full helmet + jersey + pants + boots cuts crash and landing damage by 70%. */
    public static boolean isWearingFullSet(LivingEntity entity) {
        return entity.getEquippedStack(EquipmentSlot.HEAD).isOf(ModItems.MX_HELMET)
                && entity.getEquippedStack(EquipmentSlot.CHEST).isOf(ModItems.MX_JERSEY)
                && entity.getEquippedStack(EquipmentSlot.LEGS).isOf(ModItems.MX_PANTS)
                && entity.getEquippedStack(EquipmentSlot.FEET).isOf(ModItems.MX_BOOTS);
    }
}
