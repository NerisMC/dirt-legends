package com.neoncoast.registry;

import com.neoncoast.NeonCoast;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;

public final class ModItemGroup {
    public static final ItemGroup MAIN = Registry.register(Registries.ITEM_GROUP, NeonCoast.id("main"),
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(ModItems.SPORTS_KEY))
                    .displayName(Text.translatable("itemGroup." + NeonCoast.MOD_ID))
                    .entries((context, entries) -> {
                        entries.add(ModItems.CITY_KIT);
                        entries.add(ModItems.SEDAN_KEY);
                        entries.add(ModItems.SPORTS_KEY);
                        entries.add(ModItems.POLICE_KEY);
                        entries.add(ModItems.REPAIR_KIT);
                        entries.add(ModItems.PACKAGE);
                        entries.add(ModBlocks.SHOP_TERMINAL);
                        entries.add(ModBlocks.JOB_BOARD);
                        entries.add(ModBlocks.CHOP_SHOP);
                        entries.add(ModBlocks.ASPHALT);
                        entries.add(ModBlocks.ROAD_LINE);
                        entries.add(ModBlocks.SIDEWALK);
                    })
                    .build());

    private ModItemGroup() {
    }

    public static void init() {
    }
}
