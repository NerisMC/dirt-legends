package com.dirtlegends.block;

import com.dirtlegends.entity.MotoVehicleEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.phys.Vec3;

/** Liquid mud: walking through it is a slog. Vehicles handle it in their own physics (Surface.MUD_LIQUID). */
public class MudBlock extends LiquidBlock {
    public MudBlock(FlowingFluid fluid, Properties settings) {
        super(fluid, settings);
    }

    @Override
    protected void entityInside(BlockState state, Level world, BlockPos pos, Entity entity) {
        if (!(entity instanceof MotoVehicleEntity) && !(entity.getVehicle() instanceof MotoVehicleEntity)) {
            entity.makeStuckInBlock(state, new Vec3(0.45, 0.6, 0.45));
        }
    }
}
