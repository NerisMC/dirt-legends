package com.dirtlegends.registry;

import com.dirtlegends.DirtLegends;
import com.dirtlegends.block.DirtRampBlock;
import com.dirtlegends.block.FuelPumpBlock;
import com.dirtlegends.block.KickerRampBlock;
import com.dirtlegends.block.MudBlock;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public final class ModBlocks {
    public static final Block TRACK_DIRT = register("track_dirt", Block::new, dirt());
    public static final Block DIRT_RAMP = register("dirt_ramp", p -> new DirtRampBlock(TRACK_DIRT.defaultBlockState(), p), dirt());
    public static final Block DIRT_SLAB = register("dirt_slab", SlabBlock::new, dirt());
    public static final Block KICKER_RAMP = register("kicker_ramp", KickerRampBlock::new, BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOD).strength(1.5f).sound(SoundType.WOOD).noOcclusion());
    public static final Block START_FINISH_LINE = register("start_finish_line", Block::new, BlockBehaviour.Properties.of()
            .mapColor(MapColor.SNOW).strength(0.8f).sound(SoundType.ROOTED_DIRT));
    public static final Block CHECKPOINT = register("checkpoint", Block::new, BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_YELLOW).strength(0.8f).sound(SoundType.ROOTED_DIRT));

    public static final Block FUEL_PUMP = register("fuel_pump", FuelPumpBlock::new, BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_RED).strength(2.0f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    /** The liquid mud block (no item; you carry it in a Mud Bucket). */
    public static final Block MUD = registerNoItem("mud", p -> new MudBlock(ModFluids.STILL_MUD, p),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).replaceable()
                    .noCollision().strength(100.0f).pushReaction(PushReaction.DESTROY).noLootTable().liquid()
                    .sound(SoundType.EMPTY));

    private ModBlocks() {
    }

    private static BlockBehaviour.Properties dirt() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).strength(0.6f).sound(SoundType.ROOTED_DIRT);
    }

    private static Block registerNoItem(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties props) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, DirtLegends.id(name));
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(props.setId(key)));
    }

    private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties props) {
        Block block = registerNoItem(name, factory, props);
        ModItems.register(name, p -> {
            BlockItem item = new BlockItem(block, p);
            item.registerBlocks(Item.BY_BLOCK, item);
            return item;
        }, new Item.Properties().useBlockDescriptionPrefix());
        return block;
    }

    public static void init() {
    }
}
