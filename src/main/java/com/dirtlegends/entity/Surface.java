package com.dirtlegends.entity;

import com.dirtlegends.block.KickerRampBlock;
import com.dirtlegends.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * How a block surface feels to ride on.
 *
 * @param grip  0..1, how quickly the tyres pull the vehicle back in line (low = slides)
 * @param top   top speed multiplier
 * @param accel acceleration multiplier
 */
public record Surface(float grip, float top, float accel) {
    public static final Surface TRACK = new Surface(0.90f, 1.08f, 1.05f);
    public static final Surface DIRT = new Surface(0.85f, 1.00f, 1.00f);
    public static final Surface HARD = new Surface(0.95f, 0.95f, 0.90f);
    public static final Surface SAND = new Surface(0.45f, 0.70f, 0.60f);
    public static final Surface MUD = new Surface(0.50f, 0.55f, 0.50f);
    public static final Surface SNOW = new Surface(0.35f, 0.70f, 0.55f);
    public static final Surface ICE = new Surface(0.06f, 1.00f, 0.15f);

    public static Surface of(MotoVehicleEntity vehicle) {
        World world = vehicle.getWorld();
        BlockState feet = world.getBlockState(vehicle.getBlockPos());
        if (feet.isOf(Blocks.SNOW)) return SNOW;
        if (isTrack(feet)) return TRACK;
        BlockState s = world.getBlockState(BlockPos.ofFloored(vehicle.getX(), vehicle.getY() - 0.2, vehicle.getZ()));
        if (isTrack(s)) return TRACK;
        if (s.isIn(BlockTags.ICE)) return ICE;
        if (s.isIn(BlockTags.SAND) || s.isOf(Blocks.GRAVEL) || s.isOf(Blocks.SUSPICIOUS_GRAVEL)) return SAND;
        if (s.isOf(Blocks.MUD) || s.isOf(Blocks.SOUL_SAND) || s.isOf(Blocks.SOUL_SOIL) || s.isOf(Blocks.CLAY)
                || s.isOf(Blocks.MUDDY_MANGROVE_ROOTS)) return MUD;
        if (s.isOf(Blocks.SNOW_BLOCK) || s.isOf(Blocks.POWDER_SNOW)) return SNOW;
        if (s.isIn(BlockTags.DIRT) || s.isOf(Blocks.DIRT_PATH) || s.isOf(Blocks.FARMLAND)) return DIRT;
        return HARD;
    }

    private static boolean isTrack(BlockState s) {
        return s.isOf(ModBlocks.TRACK_DIRT) || s.isOf(ModBlocks.DIRT_RAMP) || s.isOf(ModBlocks.DIRT_SLAB)
                || s.isOf(ModBlocks.START_FINISH_LINE) || s.isOf(ModBlocks.CHECKPOINT)
                || s.getBlock() instanceof KickerRampBlock;
    }
}
