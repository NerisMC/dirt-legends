package com.dirtlegends.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/** Gas station pump. Any vehicle parked within 2 blocks refuels automatically. */
public class FuelPumpBlock extends HorizontalFacingBlock {
    public static final MapCodec<FuelPumpBlock> CODEC = createCodec(FuelPumpBlock::new);
    private static final VoxelShape NORTH_SOUTH = Block.createCuboidShape(3, 0, 4, 13, 15, 12);
    private static final VoxelShape EAST_WEST = Block.createCuboidShape(4, 0, 3, 12, 15, 13);

    public FuelPumpBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction d = state.get(FACING);
        return d == Direction.NORTH || d == Direction.SOUTH ? NORTH_SOUTH : EAST_WEST;
    }
}
