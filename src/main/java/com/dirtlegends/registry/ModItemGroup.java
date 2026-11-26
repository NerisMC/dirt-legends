package com.dirtlegends.registry;

import com.dirtlegends.DirtLegends;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;

public final class ModItemGroup {
    public static final ItemGroup MAIN = Registry.register(Registries.ITEM_GROUP, DirtLegends.id("main"),
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(ModItems.DIRT_BIKES[0]))
                    .displayName(Text.translatable("itemGroup." + DirtLegends.MOD_ID))
                    .entries((context, entries) -> {
                        for (Item bike : ModItems.DIRT_BIKES) entries.add(bike);
                        for (Item atv : ModItems.ATVS) entries.add(atv);
                        entries.add(ModItems.MX_HELMET);
                        entries.add(ModItems.MX_JERSEY);
                        entries.add(ModItems.MX_PANTS);
                        entries.add(ModItems.MX_BOOTS);
                        entries.add(ModBlocks.TRACK_DIRT);
                        entries.add(ModBlocks.DIRT_RAMP);
                        entries.add(ModBlocks.DIRT_SLAB);
                        entries.add(ModBlocks.KICKER_RAMP);
                        entries.add(ModBlocks.START_FINISH_LINE);
                        entries.add(ModBlocks.CHECKPOINT);
                        entries.add(ModItems.TRACK_KIT);
                        entries.add(ModBlocks.FUEL_PUMP);
                        entries.add(ModItems.FUEL_CAN);
                        entries.add(ModItems.MUD_BUCKET);
                        entries.add(ModItems.ENGINE);
                        entries.add(ModItems.TIRE);
                    })
                    .build());

    private ModItemGroup() {
    }

    public static void init() {
    }
}
