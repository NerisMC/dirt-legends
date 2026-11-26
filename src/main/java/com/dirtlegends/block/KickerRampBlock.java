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
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

import java.util.EnumMap;
import java.util.Map;

/** Wooden kicker that rises toward the direction you were facing when you placed it and launches vehicles. */
public class KickerRampBlock extends HorizontalFacingBlock {
    public static final MapCodec<KickerRampBlock> CODEC = createCodec(KickerRampBlock::new);
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        for (Direction d : Direction.Type.HORIZONTAL) {
            SHAPES.put(d, makeShape(d));
        }
    }

    public KickerRampBlock(Settings settings) {
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
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing());
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPES.get(state.get(FACING));
    }

    /** Four steps, 2/4/6/8 pixels tall, rising toward {@code facing}. Matches models/block/kicker_ramp.json. */
    private static VoxelShape makeShape(Direction facing) {
        VoxelShape shape = VoxelShapes.empty();
        for (int i = 0; i < 4; i++) {
            double a = i * 4, b = i * 4 + 4, h = 2 + i * 2;
            double x1 = 0, x2 = 16, z1 = 0, z2 = 16;
            switch (facing) {
                case NORTH -> { z1 = 16 - b; z2 = 16 - a; }
                case SOUTH -> { z1 = a; z2 = b; }
                case WEST -> { x1 = 16 - b; x2 = 16 - a; }
                default -> { x1 = a; x2 = b; }
            }
            shape = VoxelShapes.union(shape, Block.createCuboidShape(x1, 0, z1, x2, h, z2));
        }
        return shape;
    }
}
