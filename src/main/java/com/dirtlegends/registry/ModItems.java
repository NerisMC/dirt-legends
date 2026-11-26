package com.dirtlegends.registry;

import com.dirtlegends.DirtLegends;
import com.dirtlegends.item.FuelCanItem;
import com.dirtlegends.item.TrackKitItem;
import com.dirtlegends.item.VehicleItem;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.ArmorType;

public final class ModItems {
    public static final String[] COLORS = {"red", "blue", "green", "yellow"};
    public static final Item[] DIRT_BIKES = new Item[COLORS.length];
    public static final Item[] ATVS = new Item[COLORS.length];

    static {
        for (int i = 0; i < COLORS.length; i++) {
            final int v = i;
            DIRT_BIKES[i] = register("dirt_bike_" + COLORS[i], p -> new VehicleItem(p, () -> ModEntities.DIRT_BIKE, v), new Item.Properties().stacksTo(1));
            ATVS[i] = register("atv_" + COLORS[i], p -> new VehicleItem(p, () -> ModEntities.ATV, v), new Item.Properties().stacksTo(1));
        }
    }

    public static final Item ENGINE = register("engine", Item::new, new Item.Properties());
    public static final Item TIRE = register("tire", Item::new, new Item.Properties());
    public static final Item FUEL_CAN = register("fuel_can", FuelCanItem::new, new Item.Properties().stacksTo(16));
    public static final Item MUD_BUCKET = register("mud_bucket", p -> new BucketItem(ModFluids.STILL_MUD, p),
            new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1));
    public static final Item TRACK_KIT = register("track_kit", TrackKitItem::new, new Item.Properties().stacksTo(16));

    public static final Item MX_HELMET = register("mx_helmet", Item::new, new Item.Properties().humanoidArmor(ModArmor.MOTOCROSS, ArmorType.HELMET));
    public static final Item MX_JERSEY = register("mx_jersey", Item::new, new Item.Properties().humanoidArmor(ModArmor.MOTOCROSS, ArmorType.CHESTPLATE));
    public static final Item MX_PANTS = register("mx_pants", Item::new, new Item.Properties().humanoidArmor(ModArmor.MOTOCROSS, ArmorType.LEGGINGS));
    public static final Item MX_BOOTS = register("mx_boots", Item::new, new Item.Properties().humanoidArmor(ModArmor.MOTOCROSS, ArmorType.BOOTS));

    private ModItems() {
    }

    public static <T extends Item> T register(String name, Function<Item.Properties, T> factory, Item.Properties props) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, DirtLegends.id(name));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(props.setId(key)));
    }

    public static void init() {
    }
}
