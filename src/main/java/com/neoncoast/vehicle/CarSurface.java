package com.neoncoast.vehicle;

import com.neoncoast.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;

/** Road cars are fast on tarmac and miserable off-road. */
public record CarSurface(float grip, float top, float accel) {
    public static final CarSurface ROAD = new CarSurface(0.95f, 1.00f, 1.00f);
    public static final CarSurface HARD = new CarSurface(0.85f, 0.85f, 0.90f);
    public static final CarSurface DIRT = new CarSurface(0.65f, 0.55f, 0.70f);
    public static final CarSurface SAND = new CarSurface(0.45f, 0.40f, 0.45f);
    public static final CarSurface SNOW = new CarSurface(0.30f, 0.50f, 0.45f);
    public static final CarSurface ICE = new CarSurface(0.07f, 0.90f, 0.20f);

    public static CarSurface of(CarEntity car) {
        BlockState feet = car.getWorld().getBlockState(car.getBlockPos());
        if (feet.isOf(Blocks.SNOW)) return SNOW;
        BlockState s = car.getWorld().getBlockState(BlockPos.ofFloored(car.getX(), car.getY() - 0.2, car.getZ()));
        if (s.isOf(ModBlocks.ASPHALT) || s.isOf(ModBlocks.ROAD_LINE) || s.isOf(ModBlocks.SIDEWALK)) return ROAD;
        if (s.isIn(BlockTags.ICE)) return ICE;
        if (s.isIn(BlockTags.SAND) || s.isOf(Blocks.GRAVEL)) return SAND;
        if (s.isOf(Blocks.SNOW_BLOCK) || s.isOf(Blocks.POWDER_SNOW)) return SNOW;
        if (s.isIn(BlockTags.DIRT) || s.isOf(Blocks.DIRT_PATH) || s.isOf(Blocks.FARMLAND) || s.isIn(BlockTags.LEAVES)) return DIRT;
        return HARD;
    }
}
