package com.dirtlegends.block;

import com.dirtlegends.entity.MotoVehicleEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Liquid mud: walking through it is a slog. Vehicles handle it in their own physics (Surface.MUD_LIQUID). */
public class MudBlock extends FluidBlock {
    public MudBlock(FlowableFluid fluid, Settings settings) {
        super(fluid, settings);
    }

    @Override
    protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!(entity instanceof MotoVehicleEntity) && !(entity.getVehicle() instanceof MotoVehicleEntity)) {
            entity.slowMovement(state, new Vec3d(0.45, 0.6, 0.45));
        }
    }
}
