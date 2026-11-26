package com.dirtlegends.registry;

import com.dirtlegends.DirtLegends;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

public final class ModArmor {
    public static final ResourceKey<EquipmentAsset> MOTOCROSS_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, DirtLegends.id("motocross"));

    /** Between leather and chainmail on paper; the real benefit is crash protection on a vehicle. */
    public static final ArmorMaterial MOTOCROSS = new ArmorMaterial(12, defense(), 15, SoundEvents.ARMOR_EQUIP_LEATHER,
            0.0f, 0.0f, ItemTags.REPAIRS_LEATHER_ARMOR, MOTOCROSS_ASSET);

    private static Map<ArmorType, Integer> defense() {
        Map<ArmorType, Integer> map = new EnumMap<>(ArmorType.class);
        map.put(ArmorType.BOOTS, 2);
        map.put(ArmorType.LEGGINGS, 3);
        map.put(ArmorType.CHESTPLATE, 4);
        map.put(ArmorType.HELMET, 2);
        map.put(ArmorType.BODY, 4);
        return map;
    }

    private ModArmor() {
    }

    public static void init() {
    }
}
