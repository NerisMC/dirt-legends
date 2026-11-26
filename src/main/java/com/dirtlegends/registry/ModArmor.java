package com.dirtlegends.registry;

import com.dirtlegends.DirtLegends;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Util;

import java.util.EnumMap;
import java.util.List;

public final class ModArmor {
    /** Between leather and chainmail on paper; the real benefit is crash protection on a vehicle. */
    public static final RegistryEntry<ArmorMaterial> MOTOCROSS = Registry.registerReference(Registries.ARMOR_MATERIAL,
            DirtLegends.id("motocross"),
            new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                        map.put(ArmorItem.Type.BOOTS, 2);
                        map.put(ArmorItem.Type.LEGGINGS, 3);
                        map.put(ArmorItem.Type.CHESTPLATE, 4);
                        map.put(ArmorItem.Type.HELMET, 2);
                        map.put(ArmorItem.Type.BODY, 4);
                    }),
                    15,
                    SoundEvents.ITEM_ARMOR_EQUIP_LEATHER,
                    () -> Ingredient.ofItems(Items.LEATHER),
                    List.of(new ArmorMaterial.Layer(DirtLegends.id("motocross"))),
                    0.0f,
                    0.0f));

    private ModArmor() {
    }

    public static void init() {
    }
}
