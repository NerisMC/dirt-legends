package com.dirtlegends.block;

import com.mojang.serialization.MapCodec;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Wooden kicker that rises toward the direction you were facing when you placed it and launches vehicles. */
public class KickerRampBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<KickerRampBlock> CODEC = simpleCodec(KickerRampBlock::new);
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            SHAPES.put(d, makeShape(d));
        }
    }

    public KickerRampBlock(Properties settings) {
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
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    /** Four steps, 2/4/6/8 pixels tall, rising toward {@code facing}. Matches models/block/kicker_ramp.json. */
    private static VoxelShape makeShape(Direction facing) {
        VoxelShape shape = Shapes.empty();
        for (int i = 0; i < 4; i++) {
            double a = i * 4, b = i * 4 + 4, h = 2 + i * 2;
            double x1 = 0, x2 = 16, z1 = 0, z2 = 16;
            switch (facing) {
                case NORTH -> { z1 = 16 - b; z2 = 16 - a; }
                case SOUTH -> { z1 = a; z2 = b; }
                case WEST -> { x1 = 16 - b; x2 = 16 - a; }
                default -> { x1 = a; x2 = b; }
            }
            shape = Shapes.or(shape, Block.box(x1, 0, z1, x2, h, z2));
        }
        return shape;
    }
}
