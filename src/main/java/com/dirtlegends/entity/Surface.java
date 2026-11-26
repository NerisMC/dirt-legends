package com.dirtlegends.entity;

import com.dirtlegends.block.KickerRampBlock;
import com.dirtlegends.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

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
    public static final Surface MUD_LIQUID = new Surface(0.30f, 0.40f, 0.35f);
    public static final Surface ICE = new Surface(0.06f, 1.00f, 0.15f);

    public static Surface of(MotoVehicleEntity vehicle) {
        Level world = vehicle.level();
        BlockState feet = world.getBlockState(vehicle.blockPosition());
        if (world.getFluidState(vehicle.blockPosition()).getType() instanceof com.dirtlegends.fluid.MudFluid) return MUD_LIQUID;
        if (feet.is(Blocks.SNOW)) return SNOW;
        if (isTrack(feet)) return TRACK;
        BlockState s = world.getBlockState(BlockPos.containing(vehicle.getX(), vehicle.getY() - 0.2, vehicle.getZ()));
        if (isTrack(s)) return TRACK;
        if (s.is(BlockTags.ICE)) return ICE;
        if (s.is(BlockTags.SAND) || s.is(Blocks.GRAVEL) || s.is(Blocks.SUSPICIOUS_GRAVEL)) return SAND;
        if (s.is(Blocks.MUD) || s.is(Blocks.SOUL_SAND) || s.is(Blocks.SOUL_SOIL) || s.is(Blocks.CLAY)
                || s.is(Blocks.MUDDY_MANGROVE_ROOTS)) return MUD;
        if (s.is(Blocks.SNOW_BLOCK) || s.is(Blocks.POWDER_SNOW)) return SNOW;
        if (s.is(BlockTags.DIRT) || s.is(Blocks.DIRT_PATH) || s.is(Blocks.FARMLAND)) return DIRT;
        return HARD;
    }

    private static boolean isTrack(BlockState s) {
        return s.is(ModBlocks.TRACK_DIRT) || s.is(ModBlocks.DIRT_RAMP) || s.is(ModBlocks.DIRT_SLAB)
                || s.is(ModBlocks.START_FINISH_LINE) || s.is(ModBlocks.CHECKPOINT)
                || s.getBlock() instanceof KickerRampBlock;
    }
}
