package com.neoncoast.registry;

import com.neoncoast.NeonCoast;
import com.neoncoast.block.ChopShopBlock;
import com.neoncoast.block.JobBoardBlock;
import com.neoncoast.block.ShopTerminalBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;

public final class ModBlocks {
    public static final Block ASPHALT = register("asphalt", new Block(stone(MapColor.BLACK)));
    public static final Block ROAD_LINE = register("road_line", new Block(stone(MapColor.BLACK)));
    public static final Block SIDEWALK = register("sidewalk", new Block(stone(MapColor.LIGHT_GRAY)));
    public static final Block SHOP_TERMINAL = register("shop_terminal", new ShopTerminalBlock(AbstractBlock.Settings.create()
            .mapColor(MapColor.GREEN).strength(3.0f).sounds(BlockSoundGroup.METAL).nonOpaque().luminance(s -> 8)));
    public static final Block JOB_BOARD = register("job_board", new JobBoardBlock(AbstractBlock.Settings.create()
            .mapColor(MapColor.OAK_TAN).strength(2.0f).sounds(BlockSoundGroup.WOOD)));
    public static final Block CHOP_SHOP = register("chop_shop", new ChopShopBlock(AbstractBlock.Settings.create()
            .mapColor(MapColor.GRAY).strength(3.0f).sounds(BlockSoundGroup.METAL)));

    private ModBlocks() {
    }

    private static AbstractBlock.Settings stone(MapColor color) {
        return AbstractBlock.Settings.create().mapColor(color).strength(1.5f, 6.0f).sounds(BlockSoundGroup.STONE);
    }

    private static Block register(String name, Block block) {
        Block registered = Registry.register(Registries.BLOCK, NeonCoast.id(name), block);
        BlockItem item = new BlockItem(registered, new Item.Settings());
        item.appendBlocks(Item.BLOCK_ITEMS, item);
        Registry.register(Registries.ITEM, NeonCoast.id(name), item);
        return registered;
    }

    public static void init() {
    }
}
