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
import java.util.List;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;

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

    // ---- concession stand food
    public static final Item HOT_DOG = register("hot_dog", Item::new, new Item.Properties().food(food(6, 0.6f)));
    public static final Item BURGER = register("burger", Item::new, new Item.Properties().food(food(8, 0.8f)));
    public static final Item NACHOS = register("nachos", Item::new, new Item.Properties().food(food(5, 0.5f)));
    public static final Item POPCORN = register("popcorn", Item::new, new Item.Properties().food(food(3, 0.3f),
            Consumable.builder().consumeSeconds(0.8f).animation(ItemUseAnimation.EAT).sound(SoundEvents.GENERIC_EAT).build()));
    public static final Item SODA = register("soda", Item::new, new Item.Properties().stacksTo(16).food(new FoodProperties.Builder()
            .nutrition(2).saturationModifier(0.2f).alwaysEdible().build(), Consumable.builder().consumeSeconds(1.2f)
            .animation(ItemUseAnimation.DRINK).sound(SoundEvents.GENERIC_DRINK)
            .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 20, 0))).build()));
    public static final Item ENERGY_DRINK = register("energy_drink", Item::new, new Item.Properties().stacksTo(16).food(new FoodProperties.Builder()
            .nutrition(1).saturationModifier(0.1f).alwaysEdible().build(), Consumable.builder().consumeSeconds(1.0f)
            .animation(ItemUseAnimation.DRINK).sound(SoundEvents.GENERIC_DRINK)
            .onConsume(new ApplyStatusEffectsConsumeEffect(List.of(new MobEffectInstance(MobEffects.SPEED, 20 * 60, 1),
                    new MobEffectInstance(MobEffects.HASTE, 20 * 60, 0), new MobEffectInstance(MobEffects.JUMP_BOOST, 20 * 60, 0)))).build()));

    private static FoodProperties food(int nutrition, float saturation) {
        return new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build();
    }

    private ModItems() {
    }

    public static <T extends Item> T register(String name, Function<Item.Properties, T> factory, Item.Properties props) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, DirtLegends.id(name));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(props.setId(key)));
    }

    public static void init() {
    }
}
