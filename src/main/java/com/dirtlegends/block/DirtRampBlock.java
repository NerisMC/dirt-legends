package com.dirtlegends.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.StairsBlock;

/** Stair-shaped dirt ramp. Hit it fast and the vehicle pops into the air. */
public class DirtRampBlock extends StairsBlock {
    public DirtRampBlock(BlockState baseBlockState, Settings settings) {
        super(baseBlockState, settings);
    }
}
