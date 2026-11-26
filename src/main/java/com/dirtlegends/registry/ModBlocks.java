package com.dirtlegends.registry;

import com.dirtlegends.DirtLegends;
import com.dirtlegends.block.DirtRampBlock;
import com.dirtlegends.block.FuelPumpBlock;
import com.dirtlegends.block.KickerRampBlock;
import com.dirtlegends.block.MudBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;

public final class ModBlocks {
    public static final Block TRACK_DIRT = register("track_dirt", new Block(dirt()));
    public static final Block DIRT_RAMP = register("dirt_ramp", new DirtRampBlock(TRACK_DIRT.getDefaultState(), dirt()));
    public static final Block DIRT_SLAB = register("dirt_slab", new SlabBlock(dirt()));
    public static final Block KICKER_RAMP = register("kicker_ramp", new KickerRampBlock(AbstractBlock.Settings.create()
            .mapColor(MapColor.OAK_TAN).strength(1.5f).sounds(BlockSoundGroup.WOOD).nonOpaque()));
    public static final Block START_FINISH_LINE = register("start_finish_line", new Block(AbstractBlock.Settings.create()
            .mapColor(MapColor.WHITE).strength(0.8f).sounds(BlockSoundGroup.ROOTED_DIRT)));
    public static final Block CHECKPOINT = register("checkpoint", new Block(AbstractBlock.Settings.create()
            .mapColor(MapColor.YELLOW).strength(0.8f).sounds(BlockSoundGroup.ROOTED_DIRT)));

    public static final Block FUEL_PUMP = register("fuel_pump", new FuelPumpBlock(AbstractBlock.Settings.create()
            .mapColor(MapColor.RED).strength(2.0f).sounds(BlockSoundGroup.METAL).nonOpaque().requiresTool()));
    /** The liquid mud block (no item; you carry it in a Mud Bucket). */
    public static final Block MUD = Registry.register(Registries.BLOCK, DirtLegends.id("mud"),
            new MudBlock(ModFluids.STILL_MUD, AbstractBlock.Settings.create().mapColor(MapColor.BROWN).replaceable()
                    .noCollision().strength(100.0f).pistonBehavior(PistonBehavior.DESTROY).dropsNothing().liquid()
                    .sounds(BlockSoundGroup.INTENTIONALLY_EMPTY)));

    private ModBlocks() {
    }

    private static AbstractBlock.Settings dirt() {
        return AbstractBlock.Settings.create().mapColor(MapColor.DIRT_BROWN).strength(0.6f).sounds(BlockSoundGroup.ROOTED_DIRT);
    }

    private static Block register(String name, Block block) {
        Block registered = Registry.register(Registries.BLOCK, DirtLegends.id(name), block);
        BlockItem item = new BlockItem(registered, new Item.Settings());
        item.appendBlocks(Item.BLOCK_ITEMS, item);
        Registry.register(Registries.ITEM, DirtLegends.id(name), item);
        return registered;
    }

    public static void init() {
    }
}
