package com.dirtlegends.registry;

import com.dirtlegends.DirtLegends;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ModItemGroup {
    public static final CreativeModeTab MAIN = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, DirtLegends.id("main"),
            FabricCreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.DIRT_BIKES[0]))
                    .title(Component.translatable("itemGroup." + DirtLegends.MOD_ID))
                    .displayItems((context, entries) -> {
                        for (Item bike : ModItems.DIRT_BIKES) entries.accept(bike);
                        for (Item atv : ModItems.ATVS) entries.accept(atv);
                        entries.accept(ModItems.MX_HELMET);
                        entries.accept(ModItems.MX_JERSEY);
                        entries.accept(ModItems.MX_PANTS);
                        entries.accept(ModItems.MX_BOOTS);
                        entries.accept(ModBlocks.TRACK_DIRT);
                        entries.accept(ModBlocks.DIRT_RAMP);
                        entries.accept(ModBlocks.DIRT_SLAB);
                        entries.accept(ModBlocks.KICKER_RAMP);
                        entries.accept(ModBlocks.START_FINISH_LINE);
                        entries.accept(ModBlocks.CHECKPOINT);
                        entries.accept(ModItems.TRACK_KIT);
                        entries.accept(ModBlocks.FUEL_PUMP);
                        entries.accept(ModItems.FUEL_CAN);
                        entries.accept(ModItems.MUD_BUCKET);
                        entries.accept(ModItems.ENGINE);
                        entries.accept(ModItems.TIRE);
                        entries.accept(ModItems.HOT_DOG);
                        entries.accept(ModItems.BURGER);
                        entries.accept(ModItems.NACHOS);
                        entries.accept(ModItems.POPCORN);
                        entries.accept(ModItems.SODA);
                        entries.accept(ModItems.ENERGY_DRINK);
                    })
                    .build());

    private ModItemGroup() {
    }

    public static void init() {
    }
}
