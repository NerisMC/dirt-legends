package com.dirtlegends.registry;

import com.dirtlegends.DirtLegends;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

public final class ModArmor {
    /** Between leather and chainmail on paper; the real benefit is crash protection on a vehicle. */
    public static final Holder<ArmorMaterial> MOTOCROSS = Registry.registerForHolder(BuiltInRegistries.ARMOR_MATERIAL,
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
                    SoundEvents.ARMOR_EQUIP_LEATHER,
                    () -> Ingredient.of(Items.LEATHER),
                    List.of(new ArmorMaterial.Layer(DirtLegends.id("motocross"))),
                    0.0f,
                    0.0f));

    private ModArmor() {
    }

    public static void init() {
    }
}
