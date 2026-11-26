package com.dirtlegends.block;

import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Stair-shaped dirt ramp. Hit it fast and the vehicle pops into the air. */
public class DirtRampBlock extends StairBlock {
    public DirtRampBlock(BlockState baseBlockState, Properties settings) {
        super(baseBlockState, settings);
    }
}
