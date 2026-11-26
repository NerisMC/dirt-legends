package com.dirtlegends.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Gas station pump. Any vehicle parked within 2 blocks refuels automatically. */
public class FuelPumpBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<FuelPumpBlock> CODEC = simpleCodec(FuelPumpBlock::new);
    private static final VoxelShape NORTH_SOUTH = Block.box(3, 0, 4, 13, 15, 12);
    private static final VoxelShape EAST_WEST = Block.box(4, 0, 3, 12, 15, 13);

    public FuelPumpBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        Direction d = state.getValue(FACING);
        return d == Direction.NORTH || d == Direction.SOUTH ? NORTH_SOUTH : EAST_WEST;
    }
}
