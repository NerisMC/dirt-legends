package com.dirtlegends.registry;

import com.dirtlegends.DirtLegends;
import com.dirtlegends.item.FuelCanItem;
import com.dirtlegends.item.TrackKitItem;
import com.dirtlegends.item.VehicleItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public final class ModItems {
    public static final String[] COLORS = {"red", "blue", "green", "yellow"};
    public static final Item[] DIRT_BIKES = new Item[COLORS.length];
    public static final Item[] ATVS = new Item[COLORS.length];

    static {
        for (int i = 0; i < COLORS.length; i++) {
            DIRT_BIKES[i] = register("dirt_bike_" + COLORS[i], new VehicleItem(new Item.Properties().stacksTo(1), () -> ModEntities.DIRT_BIKE, i));
            ATVS[i] = register("atv_" + COLORS[i], new VehicleItem(new Item.Properties().stacksTo(1), () -> ModEntities.ATV, i));
        }
    }

    public static final Item ENGINE = register("engine", new Item(new Item.Properties()));
    public static final Item TIRE = register("tire", new Item(new Item.Properties()));
    public static final Item FUEL_CAN = register("fuel_can", new FuelCanItem(new Item.Properties().stacksTo(16)));
    public static final Item MUD_BUCKET = register("mud_bucket", new BucketItem(ModFluids.STILL_MUD,
            new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
    public static final Item TRACK_KIT = register("track_kit", new TrackKitItem(new Item.Properties().stacksTo(16)));

    public static final Item MX_HELMET = register("mx_helmet", new ArmorItem(ModArmor.MOTOCROSS, ArmorItem.Type.HELMET,
            new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(12))));
    public static final Item MX_JERSEY = register("mx_jersey", new ArmorItem(ModArmor.MOTOCROSS, ArmorItem.Type.CHESTPLATE,
            new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(12))));
    public static final Item MX_PANTS = register("mx_pants", new ArmorItem(ModArmor.MOTOCROSS, ArmorItem.Type.LEGGINGS,
            new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(12))));
    public static final Item MX_BOOTS = register("mx_boots", new ArmorItem(ModArmor.MOTOCROSS, ArmorItem.Type.BOOTS,
            new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(12))));

    private ModItems() {
    }

    private static Item register(String name, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, DirtLegends.id(name), item);
    }

    public static void init() {
    }
}
