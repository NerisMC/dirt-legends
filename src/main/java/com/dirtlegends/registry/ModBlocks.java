package com.dirtlegends.registry;

import com.dirtlegends.DirtLegends;
import com.dirtlegends.block.DirtRampBlock;
import com.dirtlegends.block.FuelPumpBlock;
import com.dirtlegends.block.KickerRampBlock;
import com.dirtlegends.block.MudBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public final class ModBlocks {
    public static final Block TRACK_DIRT = register("track_dirt", new Block(dirt()));
    public static final Block DIRT_RAMP = register("dirt_ramp", new DirtRampBlock(TRACK_DIRT.defaultBlockState(), dirt()));
    public static final Block DIRT_SLAB = register("dirt_slab", new SlabBlock(dirt()));
    public static final Block KICKER_RAMP = register("kicker_ramp", new KickerRampBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD).strength(1.5f).sound(SoundType.WOOD).noOcclusion()));
    public static final Block START_FINISH_LINE = register("start_finish_line", new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.SNOW).strength(0.8f).sound(SoundType.ROOTED_DIRT)));
    public static final Block CHECKPOINT = register("checkpoint", new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_YELLOW).strength(0.8f).sound(SoundType.ROOTED_DIRT)));

    public static final Block FUEL_PUMP = register("fuel_pump", new FuelPumpBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_RED).strength(2.0f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops()));
    /** The liquid mud block (no item; you carry it in a Mud Bucket). */
    public static final Block MUD = Registry.register(BuiltInRegistries.BLOCK, DirtLegends.id("mud"),
            new MudBlock(ModFluids.STILL_MUD, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).replaceable()
                    .noCollission().strength(100.0f).pushReaction(PushReaction.DESTROY).noLootTable().liquid()
                    .sound(SoundType.EMPTY)));

    private ModBlocks() {
    }

    private static BlockBehaviour.Properties dirt() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).strength(0.6f).sound(SoundType.ROOTED_DIRT);
    }

    private static Block register(String name, Block block) {
        Block registered = Registry.register(BuiltInRegistries.BLOCK, DirtLegends.id(name), block);
        BlockItem item = new BlockItem(registered, new Item.Properties());
        item.registerBlocks(Item.BY_BLOCK, item);
        Registry.register(BuiltInRegistries.ITEM, DirtLegends.id(name), item);
        return registered;
    }

    public static void init() {
    }
}
