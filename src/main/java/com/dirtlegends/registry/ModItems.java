package com.dirtlegends.registry;

import com.dirtlegends.DirtLegends;
import com.dirtlegends.item.FuelCanItem;
import com.dirtlegends.item.TrackKitItem;
import com.dirtlegends.item.VehicleItem;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.BucketItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModItems {
    public static final String[] COLORS = {"red", "blue", "green", "yellow"};
    public static final Item[] DIRT_BIKES = new Item[COLORS.length];
    public static final Item[] ATVS = new Item[COLORS.length];

    static {
        for (int i = 0; i < COLORS.length; i++) {
            DIRT_BIKES[i] = register("dirt_bike_" + COLORS[i], new VehicleItem(new Item.Settings().maxCount(1), () -> ModEntities.DIRT_BIKE, i));
            ATVS[i] = register("atv_" + COLORS[i], new VehicleItem(new Item.Settings().maxCount(1), () -> ModEntities.ATV, i));
        }
    }

    public static final Item ENGINE = register("engine", new Item(new Item.Settings()));
    public static final Item TIRE = register("tire", new Item(new Item.Settings()));
    public static final Item FUEL_CAN = register("fuel_can", new FuelCanItem(new Item.Settings().maxCount(16)));
    public static final Item MUD_BUCKET = register("mud_bucket", new BucketItem(ModFluids.STILL_MUD,
            new Item.Settings().recipeRemainder(Items.BUCKET).maxCount(1)));
    public static final Item TRACK_KIT = register("track_kit", new TrackKitItem(new Item.Settings().maxCount(16)));

    public static final Item MX_HELMET = register("mx_helmet", new ArmorItem(ModArmor.MOTOCROSS, ArmorItem.Type.HELMET,
            new Item.Settings().maxDamage(ArmorItem.Type.HELMET.getMaxDamage(12))));
    public static final Item MX_JERSEY = register("mx_jersey", new ArmorItem(ModArmor.MOTOCROSS, ArmorItem.Type.CHESTPLATE,
            new Item.Settings().maxDamage(ArmorItem.Type.CHESTPLATE.getMaxDamage(12))));
    public static final Item MX_PANTS = register("mx_pants", new ArmorItem(ModArmor.MOTOCROSS, ArmorItem.Type.LEGGINGS,
            new Item.Settings().maxDamage(ArmorItem.Type.LEGGINGS.getMaxDamage(12))));
    public static final Item MX_BOOTS = register("mx_boots", new ArmorItem(ModArmor.MOTOCROSS, ArmorItem.Type.BOOTS,
            new Item.Settings().maxDamage(ArmorItem.Type.BOOTS.getMaxDamage(12))));

    private ModItems() {
    }

    private static Item register(String name, Item item) {
        return Registry.register(Registries.ITEM, DirtLegends.id(name), item);
    }

    public static void init() {
    }
}
